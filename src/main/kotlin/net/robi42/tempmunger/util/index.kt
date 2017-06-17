package net.robi42.tempmunger.util

import org.slf4j.LoggerFactory.getLogger
import org.springframework.http.MediaType
import org.springframework.http.MediaType.parseMediaType
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset.UTC
import kotlin.reflect.full.companionObject

/** @see <https://stackoverflow.com/a/34462577/6079644> */
internal fun <R : Any> R.logger() = lazy { getLogger(unwrapCompanionClass(this::class.java).name) }

internal fun utc() = ZoneId.of(UTC.id)

internal fun LocalDateTime.toEpochMilli() = this.toInstant(UTC).toEpochMilli()

internal object MediaTypes {
    val TEXT_CSV: MediaType = parseMediaType("text/csv")
}

private fun <T : Any> unwrapCompanionClass(ofClass: Class<T>) =
        if (ofClass.enclosingClass != null && ofClass.enclosingClass.kotlin.companionObject?.java == ofClass)
            ofClass.enclosingClass
        else
            ofClass
