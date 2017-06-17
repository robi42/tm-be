package net.robi42.tempmunger.domain.dto

import java.time.Instant

data class ErrorDto(val timestamp: Instant,
                    val status: Int,
                    val error: String,
                    val exception: Class<*>,
                    val message: String?,
                    val path: String)
