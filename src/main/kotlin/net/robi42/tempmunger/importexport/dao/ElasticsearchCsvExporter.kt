package net.robi42.tempmunger.importexport.dao

import net.robi42.tempmunger.error.TempMungerParseException
import net.robi42.tempmunger.util.logger
import net.robi42.tempmunger.util.toEpochMilli
import net.robi42.tempmunger.domain.model.TemporalFormat
import net.robi42.tempmunger.domain.model.TemporalFormat.ISO_DATE
import net.robi42.tempmunger.domain.model.TemporalFormat.ISO_DATE_TIME
import net.robi42.tempmunger.search.dao.SearchRepository
import net.robi42.tempmunger.transform.util.DateTimeParser
import org.apache.commons.csv.CSVFormat
import org.apache.commons.csv.CSVPrinter
import org.springframework.stereotype.Component
import java.io.StringWriter
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter.ISO_DATE as ISO_DATE_FORMATTER
import java.time.format.DateTimeFormatter.ISO_DATE_TIME as ISO_DATE_TIME_FORMATTER

@Component class ElasticsearchCsvExporter(private val searchRepository: SearchRepository,
                                          private val dateTimeParser: DateTimeParser) : CsvExporter {

    private val log by logger()

    override fun export(temporalFormat: TemporalFormat,
                        excludeFields: Set<String>,
                        vararg temporalFields: String): String {
        val out = StringWriter()
        val documents = allDocumentsFormatted(temporalFormat, excludeFields, temporalFields)
        val headers = headersOf(documents.first())
        val format = csvFormatWith(headers)
        val printer = CSVPrinter(out, format)
        val records = documents.map { it.values }

        printer.printRecords(records)

        return out.toString()
    }

    private fun allDocumentsFormatted(temporalFormat: TemporalFormat,
                                      excludeFields: Set<String>,
                                      temporalFields: Array<out String>) = searchRepository.findAll().map {
        val document = it

        excludeFields.forEach { document.remove(it) }

        temporalFields.forEach {
            if (!excludeFields.contains(it)) {
                val temporal = document[it].toString()

                try {
                    val dateTime = dateTimeParser.parse(temporal)
                    document[it] = format(temporalFormat, dateTime)
                } catch (e: TempMungerParseException) {
                    log.debug("Detected missing value on parse attempt, ignoring it.")
                }
            }
        }

        document
    }

    private fun format(temporalFormat: TemporalFormat, dateTime: LocalDateTime) = when (temporalFormat) {
        ISO_DATE ->
            ISO_DATE_FORMATTER.format(dateTime)
        ISO_DATE_TIME ->
            ISO_DATE_TIME_FORMATTER.format(dateTime)
        else ->
            dateTime.toEpochMilli().toString()
    }

    private fun headersOf(document: MutableMap<String, Any>) = document.keys.toTypedArray()

    private fun csvFormatWith(headers: Array<String>) = CSVFormat.DEFAULT.withHeader(*headers)

}
