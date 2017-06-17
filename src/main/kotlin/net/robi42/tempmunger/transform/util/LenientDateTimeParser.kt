package net.robi42.tempmunger.transform.util

import net.robi42.tempmunger.error.TempMungerParseException
import net.robi42.tempmunger.util.utc
import org.springframework.stereotype.Component
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeFormatter.*
import java.time.format.DateTimeParseException
import java.time.format.ResolverStyle.SMART

@Component class LenientDateTimeParser : DateTimeParser {

    override fun parse(text: String) = try {
        parseDate(text, ISO_ORDINAL_DATE)
    } catch (e: DateTimeParseException) {
        try {
            parseDate(text, BASIC_ISO_DATE)
        } catch (e: DateTimeParseException) {
            try {
                parseDate(text, ISO_DATE)
            } catch (e: DateTimeParseException) {
                try {
                    parseDateTime(text, ISO_DATE_TIME)
                } catch (e: DateTimeParseException) {
                    try {
                        parseDateTime(text, ISO_ZONED_DATE_TIME)
                    } catch (e: DateTimeParseException) {
                        try {
                            parseDate(text, "yyyy/MM/dd")
                        } catch (e: DateTimeParseException) {
                            try {
                                parseDateTime(text, "yyyy/MM/dd HH:mm[:ss[.SSS]]")
                            } catch (e: DateTimeParseException) {
                                try {
                                    parseDate(text, "MM/dd/yyyy")
                                } catch (e: DateTimeParseException) {
                                    try {
                                        parseDateTime(text, "MM/dd/yyyy HH:mm[:ss[.SSS]]")
                                    } catch (e: DateTimeParseException) {
                                        try {
                                            parseDate(text, "dd/MM/yyyy")
                                        } catch (e: DateTimeParseException) {
                                            try {
                                                parseDateTime(text, "dd/MM/yyyy HH:mm[:ss[.SSS]]")
                                            } catch (e: DateTimeParseException) {
                                                try {
                                                    parseDate(text, "dd.MM.yyyy")
                                                } catch (e: DateTimeParseException) {
                                                    try {
                                                        parseDateTime(text, "dd.MM.yyyy HH:mm[:ss[.SSS]]")
                                                    } catch (e: DateTimeParseException) {
                                                        try {
                                                            parseFromEpochMilli(text)
                                                        } catch (e: NumberFormatException) {
                                                            throw TempMungerParseException("Could not parse '$text'", e)
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private fun parseDateTime(text: String, formatter: DateTimeFormatter): LocalDateTime =
            LocalDateTime.parse(text, formatter.smartResolving())

    private fun parseDateTime(text: String, pattern: String): LocalDateTime =
            LocalDateTime.parse(text, format(pattern))

    private fun parseDate(text: String, formatter: DateTimeFormatter): LocalDateTime =
            LocalDate.parse(text, formatter.smartResolving()).atStartOfDay()

    private fun parseDate(text: String, pattern: String): LocalDateTime =
            LocalDate.parse(text, format(pattern)).atStartOfDay()

    private fun parseFromEpochMilli(text: String): LocalDateTime {
        val ofEpochMilli = Instant.ofEpochMilli(text.toLong())
        return LocalDateTime.ofInstant(ofEpochMilli, utc())
    }

    private fun format(pattern: String) = DateTimeFormatter.ofPattern(pattern).smartResolving()

    private fun DateTimeFormatter.smartResolving() = this.withResolverStyle(SMART)

}
