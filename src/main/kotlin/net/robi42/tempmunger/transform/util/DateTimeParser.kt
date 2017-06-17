package net.robi42.tempmunger.transform.util

import java.time.LocalDateTime

interface DateTimeParser {

    /** Parses formatted text, returns corresponding date/time. */
    fun parse(text: String): LocalDateTime

}
