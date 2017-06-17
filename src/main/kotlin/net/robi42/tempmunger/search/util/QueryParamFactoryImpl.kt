package net.robi42.tempmunger.search.util

import net.robi42.tempmunger.util.toEpochMilli
import net.robi42.tempmunger.domain.model.TemporalScale
import net.robi42.tempmunger.domain.model.TemporalScale.*
import org.springframework.stereotype.Component
import java.time.LocalTime
import java.time.ZonedDateTime

@Component class QueryParamFactoryImpl : QueryParamFactory {

    override fun createFrom(dateTime: ZonedDateTime, scale: TemporalScale?) = when (scale) { // @formatter:off
        YEAR  -> dateTime.atStartOfYear().toEpochMilli()
        MONTH -> dateTime.atStartOfMonth().toEpochMilli()
        DAY   -> dateTime.toInstant().toEpochMilli()
        else  -> dateTime.atStartOfDay().toEpochMilli()
    }
    // @formatter:on

    override fun createTo(dateTime: ZonedDateTime, scale: TemporalScale?) = when (scale) { // @formatter:off
        YEAR  -> dateTime.atEndOfYear().toEpochMilli()
        MONTH -> dateTime.atEndOfMonth().toEpochMilli()
        DAY   -> dateTime.atEndOfHour().toEpochMilli()
        else  -> dateTime.atEndOfDay().toEpochMilli()
    }
    // @formatter:on

    private fun ZonedDateTime.atStartOfYear() = this.withDayOfYear(1).atStartOfDay()

    private fun ZonedDateTime.atEndOfYear() = this.atStartOfYear().plusYears(1).minusDays(1).with(LocalTime.MAX)

    private fun ZonedDateTime.atStartOfMonth() = this.withDayOfMonth(1).atStartOfDay()

    private fun ZonedDateTime.atEndOfMonth() = this.atStartOfMonth().plusMonths(1).minusDays(1).with(LocalTime.MAX)

    private fun ZonedDateTime.atEndOfHour() = this.plusHours(1).minusNanos(1).toInstant()

    private fun ZonedDateTime.atStartOfDay() = this.toLocalDate().atStartOfDay()

    private fun ZonedDateTime.atEndOfDay() = this.with(LocalTime.MAX).toInstant()

}
