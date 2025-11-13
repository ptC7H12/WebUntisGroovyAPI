package de.c7h12.webuntis.exception

import de.c7h12.webuntis.client.WebUntisException
import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.validation.FieldError
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.multipart.MaxUploadSizeExceededException

import java.time.Instant

/**
 * Global exception handler for centralized error handling
 */
@Slf4j
@CompileStatic
@ControllerAdvice
class GlobalExceptionHandler {

    /**
     * Handle WebUntis API specific exceptions
     */
    @ExceptionHandler(WebUntisException)
    ResponseEntity<Map<String, Object>> handleWebUntisException(WebUntisException ex) {
        log.error("WebUntis API error: {}", ex.message, ex)

        return ResponseEntity.badRequest().body([
            error: ex.message,
            timestamp: Instant.now().toString(),
            type: "WEBUNTIS_ERROR"
        ] as Map<String, Object>)
    }

    /**
     * Handle validation errors from @Valid annotation
     */
    @ExceptionHandler(MethodArgumentNotValidException)
    ResponseEntity<Map<String, Object>> handleValidationException(MethodArgumentNotValidException ex) {
        def errors = ex.bindingResult.allErrors.collect { error ->
            def fieldName = error instanceof FieldError ? ((FieldError) error).field : error.objectName
            return [
                field: fieldName,
                message: error.defaultMessage
            ]
        }

        log.warn("Validation error: {}", errors)

        return ResponseEntity.badRequest().body([
            error: "Validierungsfehler",
            timestamp: Instant.now().toString(),
            type: "VALIDATION_ERROR",
            details: errors
        ] as Map<String, Object>)
    }

    /**
     * Handle illegal arguments (e.g., invalid input data)
     */
    @ExceptionHandler(IllegalArgumentException)
    ResponseEntity<Map<String, Object>> handleIllegalArgumentException(IllegalArgumentException ex) {
        log.warn("Invalid argument: {}", ex.message)

        return ResponseEntity.badRequest().body([
            error: ex.message,
            timestamp: Instant.now().toString(),
            type: "INVALID_ARGUMENT"
        ] as Map<String, Object>)
    }

    /**
     * Handle file upload size exceeded
     */
    @ExceptionHandler(MaxUploadSizeExceededException)
    ResponseEntity<Map<String, Object>> handleMaxUploadSizeExceeded(MaxUploadSizeExceededException ex) {
        log.warn("File upload size exceeded: {}", ex.message)

        return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE).body([
            error: "Datei zu groß. Maximale Dateigröße: 10MB",
            timestamp: Instant.now().toString(),
            type: "FILE_TOO_LARGE"
        ] as Map<String, Object>)
    }

    /**
     * Handle generic runtime exceptions
     */
    @ExceptionHandler(RuntimeException)
    ResponseEntity<Map<String, Object>> handleRuntimeException(RuntimeException ex) {
        log.error("Runtime error: {}", ex.message, ex)

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body([
            error: "Ein Fehler ist aufgetreten: ${ex.message}",
            timestamp: Instant.now().toString(),
            type: "RUNTIME_ERROR"
        ] as Map<String, Object>)
    }

    /**
     * Handle all other exceptions
     */
    @ExceptionHandler(Exception)
    ResponseEntity<Map<String, Object>> handleGenericException(Exception ex) {
        log.error("Unexpected error", ex)

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body([
            error: "Ein unerwarteter Fehler ist aufgetreten",
            timestamp: Instant.now().toString(),
            type: "INTERNAL_ERROR"
        ] as Map<String, Object>)
    }
}
