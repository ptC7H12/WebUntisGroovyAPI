package de.c7h12.webuntis.dto

import groovy.transform.CompileStatic
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Pattern

/**
 * Request DTO for fetching messages via 2017 API
 */
@CompileStatic
@Schema(description = "Request for fetching daily messages")
class Messages2017Request extends Enhanced2017Request {

    @Pattern(regexp = "^\\d{4}-\\d{2}-\\d{2}$", message = "Date muss im Format YYYY-MM-DD sein")
    @Schema(description = "Date for messages (defaults to today)", example = "2025-01-15")
    String date
}
