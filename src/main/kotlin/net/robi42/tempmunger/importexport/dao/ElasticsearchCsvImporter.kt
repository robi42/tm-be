package net.robi42.tempmunger.importexport.dao

import com.google.common.base.Charsets.UTF_8
import com.google.common.base.Stopwatch
import com.google.common.io.Resources.getResource
import com.google.common.io.Resources.toString
import net.robi42.tempmunger.error.TempMungerException
import net.robi42.tempmunger.util.logger
import net.robi42.tempmunger.domain.model.TemporalEntry
import net.robi42.tempmunger.importexport.util.ElasticsearchCsvConverter
import org.springframework.data.elasticsearch.ElasticsearchException
import org.springframework.data.elasticsearch.core.ElasticsearchOperations
import org.springframework.data.elasticsearch.core.query.IndexQuery
import org.springframework.stereotype.Component
import java.io.IOException
import java.util.concurrent.TimeUnit.MILLISECONDS

@Component class ElasticsearchCsvImporter(private val elasticsearchTemplate: ElasticsearchOperations,
                                          private val csvConverter: ElasticsearchCsvConverter) : CsvImporter {

    private val log by logger()

    override fun import(csv: ByteArray, separator: Char) {
        val watch = Stopwatch.createStarted()
        val indexQueries = indexWipingExisting(csv, separator)

        log.info("Importing {} temporal entries took {} ms", indexQueries.size, watch.elapsed(MILLISECONDS))
    }

    private fun indexWipingExisting(csv: ByteArray, separator: Char): List<IndexQuery> {
        deleteIndex()
        createIndex()

        try {
            putMapping()

            return bulkIndex(csv, separator)
        } catch (e: IOException) {
            throw TempMungerException("I/O error occurred indexing temporal entries from CSV", e)
        }

    }

    private fun deleteIndex() {
        elasticsearchTemplate.deleteIndex(TemporalEntry.INDEX_NAME)
    }

    private fun createIndex() {
        val settings = mapOf("index.mapping.ignore_malformed" to true, "index.mapping.coerce" to true)

        elasticsearchTemplate.createIndex(TemporalEntry.INDEX_NAME, settings)
    }

    private fun putMapping() {
        val mapping = toString(getResource("temporal_entry.json"), UTF_8)

        elasticsearchTemplate.putMapping(TemporalEntry.INDEX_NAME, TemporalEntry.TYPE_NAME, mapping)
    }

    private fun bulkIndex(csv: ByteArray, separator: Char): List<IndexQuery> {
        val queries = csvConverter.convert(csv, separator)

        try {
            elasticsearchTemplate.indexAndRefresh(queries)
        } catch (e: ElasticsearchException) {
            val failedIds = e.failedDocuments.keys
            log.warn("Retrying to index failed documents: {}", failedIds.joinToString())

            val failedQueries = queries.filter { failedIds.contains(it.id) }
            elasticsearchTemplate.indexAndRefresh(failedQueries)
        }

        return queries
    }

    private fun ElasticsearchOperations.indexAndRefresh(queries: List<IndexQuery>) = this.apply {
        bulkIndex(queries)
        refresh(TemporalEntry.INDEX_NAME)
    }

}
