package net.robi42.tempmunger.importexport.service

import net.robi42.tempmunger.domain.model.TemporalFormat
import net.robi42.tempmunger.importexport.dao.CsvExporter
import net.robi42.tempmunger.importexport.dao.CsvImporter
import org.springframework.stereotype.Service

@Service class CsvImportExportServiceImpl(private val importer: CsvImporter,
                                          private val exporter: CsvExporter) : CsvImportExportService {

    override fun import(csv: ByteArray, separator: Char) = importer.import(csv, separator)

    override fun export(temporalFormat: TemporalFormat, excludeFields: Set<String>, vararg temporalFields: String) =
            exporter.export(temporalFormat, excludeFields, *temporalFields)

}
