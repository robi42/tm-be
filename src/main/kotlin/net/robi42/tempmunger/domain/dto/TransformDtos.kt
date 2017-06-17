package net.robi42.tempmunger.domain.dto

import java.time.Instant
import java.time.Year
import java.time.YearMonth

data class TransformDto(val toTimestamp: Instant?, val toYearMonth: YearMonth?, val toYear: Year?)

data class TransformTimestampDto(val to: Instant)
