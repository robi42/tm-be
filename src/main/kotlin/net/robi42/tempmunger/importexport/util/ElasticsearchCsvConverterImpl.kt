package net.robi42.tempmunger.importexport.util

import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.ObjectMapper
import com.google.common.collect.ImmutableList
import net.robi42.tempmunger.error.TempMungerValidationException
import net.robi42.tempmunger.util.logger
import net.robi42.tempmunger.domain.model.TemporalEntry
import org.apache.commons.csv.CSVFormat
import org.apache.commons.csv.CSVRecord
import org.springframework.data.elasticsearch.core.query.IndexQuery
import org.springframework.data.elasticsearch.core.query.IndexQueryBuilder
import org.springframework.stereotype.Component
import java.io.ByteArrayInputStream
import java.io.IOException
import java.io.InputStreamReader
import java.util.*
import java.util.UUID.randomUUID

@Component class ElasticsearchCsvConverterImpl(private val objectMapper: ObjectMapper) : ElasticsearchCsvConverter {

    private val log by logger()

    override fun convert(csv: ByteArray, separator: Char): List<IndexQuery> {
        val queryBuilder = ImmutableList.builder<IndexQuery>()

        try {
            InputStreamReader(ByteArrayInputStream(csv)).use { reader ->
                addConverted(separator, reader, queryBuilder)
            }
        } catch (e: IOException) {
            val message = "Parsing CSV failed"
            log.warn(message, e)
            throw TempMungerValidationException(message, e)
        }

        return queryBuilder.build()
    }

    private fun addConverted(separator: Char,
                             reader: InputStreamReader,
                             builder: ImmutableList.Builder<IndexQuery>) {
        CSVFormat.DEFAULT.withHeader()
                .withSkipHeaderRecord()
                .withIgnoreEmptyLines()
                .withIgnoreSurroundingSpaces()
                .withDelimiter(separator)
                .parse(reader)
                .forEach {
                    convert(it).ifPresent {
                        builder.add(it)
                    }
                }
    }

    private fun convert(record: CSVRecord): Optional<IndexQuery> {
        val map = record.toMap()

        try {
            val source = map.toJson()
            val query = buildQuery(source)

            return Optional.of(query)
        } catch (e: JsonProcessingException) {
            return Optional.empty()
        }

    }

    private fun Map<String, String>.toJson()
            = objectMapper.writeValueAsString(this)

    private fun buildQuery(source: String) = IndexQueryBuilder()
            .withId(randomUUID().toString())
            .withIndexName(TemporalEntry.INDEX_NAME)
            .withType(TemporalEntry.TYPE_NAME)
            .withSource(source)
            .build()

}
