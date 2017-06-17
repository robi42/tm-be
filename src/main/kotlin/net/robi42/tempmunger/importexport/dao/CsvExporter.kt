package net.robi42.tempmunger.importexport.dao

import net.robi42.tempmunger.domain.model.TemporalFormat
import net.robi42.tempmunger.domain.model.TemporalFormat.ISO_DATE_TIME

interface CsvExporter {

    /** Exports all currently stored temporal entry data as CSV. */
    fun export(temporalFormat: TemporalFormat = ISO_DATE_TIME,
               excludeFields: Set<String> = emptySet(),
               vararg temporalFields: String): String

}
