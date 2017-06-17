package net.robi42.tempmunger.api.service

import com.fasterxml.jackson.databind.JsonNode
import net.robi42.tempmunger.domain.model.TemporalEntry
import net.robi42.tempmunger.domain.model.TemporalFormat
import net.robi42.tempmunger.domain.model.TemporalScale
import java.time.Instant
import java.time.Year
import java.time.YearMonth
import java.util.*

interface TemporalEntryService {

    /** Creates temporal entries from CSV data. */
    fun create(bytes: ByteArray, separator: Char)

    /** Exports temporal entries to CSV data. */
    fun export(temporalFormat: TemporalFormat): String

    /** Provides data schema. */
    fun schema(): Map<String, Any>

    /** Main search interface providing slicing, filtering, sorting, full-text search, aggregations, etc. */
    fun search(from: Int, size: Int, ids: Set<UUID>, sort: String?, query: Map<String, Any>): JsonNode

    /** Updates temporal entry supporting `upsert`. */
    fun update(entry: TemporalEntry): TemporalEntry

    /**
     * Delegates to
     * [SparkTransformer#transformMissing][net.robi42.tempmunger.transform.dao.SparkTransformer.transformMissing].
     * */
    fun transformMissing(temporalField: String, to: Instant)

    /** Delegates to [SparkTransformer][net.robi42.tempmunger.transform.dao.SparkTransformer]. */
    fun transform(temporalField: String, value: Instant, scale: TemporalScale, toBe: Instant)

    /** Delegates to [SparkTransformer][net.robi42.tempmunger.transform.dao.SparkTransformer]. */
    fun transform(temporalField: String, value: Instant, scale: TemporalScale, toBeWith: YearMonth)

    /** Delegates to [SparkTransformer][net.robi42.tempmunger.transform.dao.SparkTransformer]. */
    fun transform(temporalField: String, value: Instant, scale: TemporalScale, toBeWith: Year)

    /**
     * Delegates to
     * [SparkTransformer#transformMerging][net.robi42.tempmunger.transform.dao.SparkTransformer.transformMerging].
     * */
    fun merge(sourceField: String, targetField: String)

    /** Deletes entries by ID. */
    fun delete(ids: Set<UUID>)

    /** Deletes entries supporting some sliced, temporal filtering. */
    fun delete(field: String, value: String, scale: TemporalScale?, filter: String, ids: Set<UUID>)

    /** Deletes entries with missing values. */
    fun deleteMissing(field: String)

    /** Reactively detects outliers. */
    fun detectOutliers()

}
