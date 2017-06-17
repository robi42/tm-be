package net.robi42.tempmunger.api.service

import com.google.common.base.Stopwatch
import net.robi42.tempmunger.domain.model.TemporalEntry
import net.robi42.tempmunger.domain.model.TemporalEntry.Companion.INDEX_NAME
import net.robi42.tempmunger.domain.model.TemporalEntry.Companion.TYPE_NAME
import net.robi42.tempmunger.domain.model.TemporalFormat
import net.robi42.tempmunger.domain.model.TemporalScale
import net.robi42.tempmunger.importexport.service.CsvImportExportService
import net.robi42.tempmunger.search.dao.ElasticsearchRepository
import net.robi42.tempmunger.search.util.QueryParamFactory
import net.robi42.tempmunger.util.logger
import org.elasticsearch.action.update.UpdateRequest
import org.elasticsearch.index.query.QueryBuilder
import org.elasticsearch.index.query.QueryBuilders.*
import org.springframework.data.elasticsearch.core.query.DeleteQuery
import org.springframework.data.elasticsearch.core.query.UpdateQuery
import org.springframework.stereotype.Service
import java.time.Instant
import java.time.Year
import java.time.YearMonth
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter.ISO_ZONED_DATE_TIME
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.TimeUnit.MILLISECONDS

@Service class TemporalEntryServiceImpl(private val queryParamFactory: QueryParamFactory,
                                        private val searchRepository: ElasticsearchRepository,
                                        private val importExportService: CsvImportExportService,
                                        private val sparkServiceProvider: SparkEsServiceProvider,
                                        private val taskExecutor: ExecutorService) : TemporalEntryService {

    private val log by logger()

    private val elasticsearchTemplate get() = searchRepository.elasticsearchTemplate
    private val sparkContext get() = sparkServiceProvider.context

    private val transformer get() = sparkServiceProvider.transformer
    private val predictor get() = sparkServiceProvider.predictor
    private val outlierDetectionService get() = sparkServiceProvider.outlierDetectionService

    /** This (non-distributed) in-memory storage workaround can only be acceptable in a prototype, ofc. */
    private val removedFields = mutableSetOf<String>()
    private val isoDateTimeRegex = Regex("^\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}\\.\\d{3}Z$")

    override fun create(bytes: ByteArray, separator: Char) {
        sparkContext.cancelAllJobs()
        importExportService.import(bytes, separator)
        transformer.updateRdd()

        taskExecutor.submit {
            predictor.updateRdd()
            detectOutliers()
        }
        removedFields.clear()
    }

    override fun export(temporalFormat: TemporalFormat) =
            importExportService.export(temporalFormat,
                    excludeFields = removedFields,
                    temporalFields = *schema().temporalFields().toTypedArray())

    @Suppress("UNCHECKED_CAST")
    override fun schema() = (elasticsearchTemplate.getMapping(INDEX_NAME, TYPE_NAME)["properties"] as Map<String, Any>)
            .filterKeys { !removedFields.contains(it) }

    override fun search(from: Int, size: Int, ids: Set<UUID>, sort: String?, query: Map<String, Any>)
            = searchRepository.search(from, size, ids, sort, query)

    override fun update(entry: TemporalEntry): TemporalEntry {
        val query = updateQuery(entry)
        elasticsearchTemplate.update(query)
        return entry
    }

    override fun transformMissing(temporalField: String, to: Instant) {
        transformer.transformMissing(temporalField, to)
    }

    override fun transform(temporalField: String, value: Instant, scale: TemporalScale, toBe: Instant) {
        transformer.transform(temporalField, value, scale, toBe)
    }

    override fun transform(temporalField: String, value: Instant, scale: TemporalScale, toBeWith: YearMonth) {
        transformer.transform(temporalField, value, scale, toBeWith)
    }

    override fun transform(temporalField: String, value: Instant, scale: TemporalScale, toBeWith: Year) {
        transformer.transform(temporalField, value, scale, toBeWith)
    }

    override fun merge(sourceField: String, targetField: String) {
        val avg = calculateAvg(sourceField, targetField)
        transformer.transformMerging(sourceField, targetField, avg)
        removedFields += sourceField
    }

    override fun delete(ids: Set<UUID>) {
        val query = deleteBy(ids)
        elasticsearchTemplate.delete(query)
    }

    override fun delete(field: String, value: String, scale: TemporalScale?, filter: String, ids: Set<UUID>) {
        val query = deleteBy(field, value, scale, filter, ids)
        elasticsearchTemplate.delete(query)
    }

    override fun deleteMissing(field: String) {
        val query = deleteBy(missingField = field)
        elasticsearchTemplate.delete(query)
    }

    override fun detectOutliers() {
        log.info("Starting outlier detection")
        val watch = Stopwatch.createStarted()

        doDetectOutliers()

        log.info("Outlier detection took {} ms", watch.elapsed(MILLISECONDS))
    }

    private fun Map<String, Any>.temporalFields() = this.filterValues {
        val field = it as Map<*, *>
        field["type"]!! == "date"
    }.keys.toSet()

    private fun updateQuery(entry: TemporalEntry) = UpdateQuery().apply {
        indexName = INDEX_NAME
        type = TYPE_NAME
        id = entry.id.toString()
        updateRequest = UpdateRequest()
                .doc(entry.source)
                .docAsUpsert(true)
                .retryOnConflict(2)
                .refresh(true)
        setDoUpsert(true)
    }

    private fun calculateAvg(sourceField: String, targetField: String): Long {
        val sourceAvg = searchRepository.avgAgg(sourceField)
        val targetAvg = searchRepository.avgAgg(targetField)
        val avg = (sourceAvg + targetAvg) / 2
        return avg.toLong()
    }

    private fun deleteBy(ids: Set<UUID>) = deleteBy(idsQueryWith(ids))

    private fun idsQueryWith(ids: Set<UUID>) = idsQuery().ids(*ids.map(UUID::toString).toTypedArray())

    private fun deleteBy(field: String,
                         value: String,
                         scale: TemporalScale?,
                         filter: String,
                         ids: Set<UUID>) = deleteBy(
            if (isTemporal(field) && isIsoDateTimeFormat(value))
                filteredQuery(dateRangeQuery(field, value, scale), filter, ids)
            else
                filteredQuery(termQuery(field, value), filter, ids))

    private fun isTemporal(fieldName: String) = schema().filterValues {
        val field = it as Map<*, *>
        field["type"] == "date"
    }.containsKey(fieldName)

    private fun isIsoDateTimeFormat(value: String) = isoDateTimeRegex.matches(value)

    private fun filteredQuery(query: QueryBuilder, filter: String, ids: Set<UUID>) = boolQuery()
            .must(query)
            .filter(if (filter.isNotBlank()) query(filter) else matchAllQuery())
            .filter(if (ids.isNotEmpty()) idsQueryWith(ids) else matchAllQuery())

    private fun query(filter: String) = searchRepository.createQuery(filter)

    private fun dateRangeQuery(field: String, value: String, scale: TemporalScale?): QueryBuilder {
        val dateTime = ZonedDateTime.parse(value, ISO_ZONED_DATE_TIME)

        return rangeQuery(field)
                .from(from(dateTime, scale))
                .to(to(dateTime, scale))
    }

    private fun from(dateTime: ZonedDateTime, scale: TemporalScale?) = queryParamFactory.createFrom(dateTime, scale)

    private fun to(dateTime: ZonedDateTime, scale: TemporalScale?) = queryParamFactory.createTo(dateTime, scale)

    private fun deleteBy(missingField: String) = deleteBy(boolQuery().mustNot(existsQuery(missingField)))

    private fun deleteBy(queryBuilder: QueryBuilder) = DeleteQuery().apply {
        index = INDEX_NAME
        type = TYPE_NAME
        query = queryBuilder
    }

    private fun doDetectOutliers() {
        try {
            val temporalFields = schema().temporalFields()
            outlierDetectionService.detectOutliers(temporalFields)
        } catch (e: Exception) {
            log.error("Outlier detection failed", e)
        }
    }

}
