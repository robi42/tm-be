package net.robi42.tempmunger.error

import org.springframework.http.HttpStatus.BAD_REQUEST
import org.springframework.web.bind.annotation.ResponseStatus

@ResponseStatus(BAD_REQUEST, reason = "Invalid input")
internal class TempMungerValidationException : TempMungerException {

    constructor(message: String?) : super(message)

    constructor(message: String, cause: Throwable) : super(message, cause)

}
