package net.robi42.tempmunger.importexport.util

import org.springframework.data.elasticsearch.core.query.IndexQuery

interface ElasticsearchCsvConverter {

    /** Converts CSV data to Elasticsearch index queries for import. */
    fun convert(csv: ByteArray, separator: Char): List<IndexQuery>

}
