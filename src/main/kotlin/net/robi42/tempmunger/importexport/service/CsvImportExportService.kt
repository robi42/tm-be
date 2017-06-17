package net.robi42.tempmunger.importexport.service

import net.robi42.tempmunger.domain.model.TemporalFormat

interface CsvImportExportService {

    /** Delegates to [CsvImporter#import][net.robi42.tempmunger.importexport.dao.CsvImporter.import]. */
    fun import(csv: ByteArray, separator: Char)

    /** Delegates to [CsvExporter#export][net.robi42.tempmunger.importexport.dao.CsvExporter.export]. */
    fun export(temporalFormat: TemporalFormat,
               excludeFields: Set<String>,
               vararg temporalFields: String): String

}
