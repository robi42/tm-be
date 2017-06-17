package net.robi42.tempmunger.importexport.dao

interface CsvImporter {

    /** Imports CSV data to search index. */
    fun import(csv: ByteArray, separator: Char)

}
