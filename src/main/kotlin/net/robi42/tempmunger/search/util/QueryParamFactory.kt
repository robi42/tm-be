package net.robi42.tempmunger.search.util

import net.robi42.tempmunger.domain.model.TemporalScale
import java.time.ZonedDateTime

interface QueryParamFactory {

    /** Creates `from` parameter for usage in Elasticsearch queries. */
    fun createFrom(dateTime: ZonedDateTime, scale: TemporalScale?): Long

    /** Creates `to` parameter for usage in Elasticsearch queries. */
    fun createTo(dateTime: ZonedDateTime, scale: TemporalScale?): Long

}
