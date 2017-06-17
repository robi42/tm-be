package net.robi42.tempmunger.api.web

import net.robi42.tempmunger.domain.dto.ErrorDto
import org.elasticsearch.action.ActionRequestValidationException
import org.springframework.http.HttpStatus.BAD_REQUEST
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler
import java.time.Instant
import javax.servlet.http.HttpServletRequest
import javax.validation.ValidationException

@ControllerAdvice class ExceptionControllerAdvice : ResponseEntityExceptionHandler() {

    @ExceptionHandler(
            ValidationException::class,
            IllegalArgumentException::class,
            ActionRequestValidationException::class)
    fun handleValidationException(request: HttpServletRequest, e: Exception): ResponseEntity<ErrorDto> =
            ResponseEntity.badRequest().body(ErrorDto(
                    timestamp = Instant.now(),
                    status = BAD_REQUEST.value(),
                    error = BAD_REQUEST.reasonPhrase,
                    exception = e.javaClass,
                    message = e.message,
                    path = request.requestURI))

}
