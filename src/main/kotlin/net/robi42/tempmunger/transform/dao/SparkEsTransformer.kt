package net.robi42.tempmunger.transform.dao

import com.google.common.base.Stopwatch
import net.robi42.tempmunger.error.TempMungerParseException
import net.robi42.tempmunger.util.logger
import net.robi42.tempmunger.util.toEpochMilli
import net.robi42.tempmunger.util.utc
import net.robi42.tempmunger.domain.model.TemporalScale
import net.robi42.tempmunger.search.util.QueryParamFactory
import net.robi42.tempmunger.transform.SparkEsContextProvider.Companion.ES_DOC_ID
import net.robi42.tempmunger.transform.SparkEsRddProvider
import net.robi42.tempmunger.transform.SparkEsRddProvider.Companion.ES_RESOURCE
import net.robi42.tempmunger.transform.util.LenientDateTimeParser
import org.apache.spark.api.java.JavaPairRDD
import org.apache.spark.api.java.JavaRDD
import org.elasticsearch.index.query.QueryBuilder
import org.elasticsearch.index.query.QueryBuilders.*
import org.elasticsearch.index.query.RangeQueryBuilder
import org.elasticsearch.spark.rdd.api.java.JavaEsSpark.saveToEs
import org.springframework.stereotype.Component
import java.time.Instant
import java.time.Year
import java.time.YearMonth
import java.util.*
import java.util.Optional.empty
import java.util.concurrent.TimeUnit.MILLISECONDS

@Component class SparkEsTransformer(private val rddProvider: SparkEsRddProvider,
                                    private val queryParamFactory: QueryParamFactory) : SparkTransformer {

    companion object {
        private val dateTimeParser = LenientDateTimeParser()

        private fun parse(text: String) = try {
            dateTimeParser.parse(text).toEpochMilli()
        } catch (e: TempMungerParseException) {
            null
        }

        private fun avg(pair: Pair<Long, Long>) = (pair.first + pair.second) / 2
    }

    private val log by logger()

    private val rdd: JavaPairRDD<String, Map<String, Any>>
        get() = rddProvider.rdd

    override fun transformMissing(temporalField: String, to: Instant) {
        transform(missingQuery(temporalField), temporalField, to)
    }

    override fun transform(temporalField: String, value: Instant, scale: TemporalScale, toBe: Instant) {
        val query = dateRangeQuery(temporalField, value, scale)

        transform(query, temporalField, toBe)
    }

    override fun transform(temporalField: String, value: Instant, scale: TemporalScale, toBeWith: YearMonth) {
        val query = dateRangeQuery(temporalField, value, scale)

        transform(query, temporalField, toBeWith)
    }

    override fun transform(temporalField: String, value: Instant, scale: TemporalScale, toBeWith: Year) {
        val query = dateRangeQuery(temporalField, value, scale)

        transform(query, temporalField, toBeWith)
    }

    override fun transformMerging(sourceField: String, targetField: String, avg: Long) {
        log.info("Merging entries for fields '{}' and '{}'", sourceField, targetField)
        val watch = Stopwatch.createStarted()

        updateRdd()

        val transformedRdd = transformRdd(sourceField, targetField, avg)
        saveToEs(transformedRdd)

        log.info("Merging took {} ms", watch.elapsed(MILLISECONDS))
    }

    override fun updateRdd() {
        rddProvider.updateRdd(query = empty())
    }

    private fun missingQuery(field: String) = boolQuery().mustNot(existsQuery(field))

    private fun dateRangeQuery(field: String, from: Long, to: Long) = rangeQuery(field).from(from).to(to)

    private fun dateRangeQuery(temporalField: String, value: Instant, scale: TemporalScale): RangeQueryBuilder {
        val dateTime = value.atZone(utc())
        val from = queryParamFactory.createFrom(dateTime, scale)
        val to = queryParamFactory.createTo(dateTime, scale)

        return dateRangeQuery(temporalField, from, to)
    }

    private fun transform(query: QueryBuilder, field: String, to: Instant) {
        withTimedLogging(field) {
            updateRdd(query)
            transform(it, to)
        }
    }

    private fun transform(query: QueryBuilder, field: String, to: YearMonth) {
        withTimedLogging(field) {
            updateRdd(query)
            transform(it, to)
        }
    }

    private fun transform(query: QueryBuilder, field: String, to: Year) {
        withTimedLogging(field) {
            updateRdd(query)
            transform(it, to)
        }
    }

    private fun withTimedLogging(field: String, actionBodyWith: (String) -> Unit) {
        log.info("Transforming entries for field '{}'", field)
        val watch = Stopwatch.createStarted()

        actionBodyWith(field)

        log.info("Transformation took {} ms", watch.elapsed(MILLISECONDS))
    }

    private fun updateRdd(query: QueryBuilder) {
        rddProvider.updateRdd(Optional.of(query))
    }

    private fun transform(field: String, to: Instant) {
        val transformedRdd = transformRdd(field, to)
        saveToEs(transformedRdd)
    }

    private fun transformRdd(field: String, to: Instant) = rdd.map {
        val id = it._1
        val transformedValue = to.toEpochMilli()

        mapOf(ES_DOC_ID to id, field to transformedValue)
    }

    private fun transform(field: String, to: YearMonth) {
        val transformedRdd = transformRdd(field, to)
        saveToEs(transformedRdd)
    }

    private fun transformRdd(field: String, to: YearMonth) = rdd.map {
        val id = it._1
        val text = it._2[field].toString()
        val transformedValue = dateTimeParser.parse(text)
                .withYear(to.year)
                .withMonth(to.monthValue)
                .toEpochMilli()

        mapOf(ES_DOC_ID to id, field to transformedValue)
    }

    private fun transform(field: String, to: Year) {
        val transformedRdd = transformRdd(field, to)
        saveToEs(transformedRdd)
    }

    private fun transformRdd(field: String, to: Year) = rdd.map {
        val id = it._1
        val text = it._2[field].toString()
        val transformedValue = dateTimeParser.parse(text)
                .withYear(to.value)
                .toEpochMilli()

        mapOf(ES_DOC_ID to id, field to transformedValue)
    }

    private fun transformRdd(sourceField: String, targetField: String, avg: Long) = rdd.map {
        val id = it._1
        val sourceText = it._2[sourceField].toString()
        val targetText = it._2[targetField].toString()

        val sourceValue = parse(sourceText)
        val targetValue = parse(targetText)
        val transformedValue =
                if (sourceValue != null && targetValue != null)
                    avg(sourceValue to targetValue)
                else targetValue ?: (sourceValue ?: avg)

        mapOf(ES_DOC_ID to id, targetField to transformedValue)
    }

    private fun saveToEs(transformedRdd: JavaRDD<Map<String, Any>>?) {
        saveToEs(transformedRdd, ES_RESOURCE)
    }

}
