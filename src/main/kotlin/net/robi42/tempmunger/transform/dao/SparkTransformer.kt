package net.robi42.tempmunger.transform.dao

import net.robi42.tempmunger.domain.model.TemporalScale
import java.time.Instant
import java.time.Year
import java.time.YearMonth

interface SparkTransformer {

    /** Transforms missing temporal fields to given instant. */
    fun transformMissing(temporalField: String, to: Instant)

    /** Transforms temporal fields to given instant filtering for given temp. value + scale. */
    fun transform(temporalField: String, value: Instant, scale: TemporalScale, toBe: Instant)

    /** Transforms temporal fields to given year/month filtering for given temp. value + scale. */
    fun transform(temporalField: String, value: Instant, scale: TemporalScale, toBeWith: YearMonth)

    /** Transforms temporal fields to given year filtering for given temp. value + scale. */
    fun transform(temporalField: String, value: Instant, scale: TemporalScale, toBeWith: Year)

    /** Transforms temporal source with target field performing merge op. */
    fun transformMerging(sourceField: String, targetField: String, avg: Long)

    /** Updates Spark RDD. */
    fun updateRdd()

}
