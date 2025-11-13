package de.c7h12.webuntis.dto

import groovy.transform.CompileStatic
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Pattern

/**
 * Request DTO for fetching timetable for a date range
 */
@CompileStatic
@Schema(description = "Request for fetching timetable within a date range")
class TimetableRangeRequest extends AuthenticationRequest {

    @NotBlank(message = "StartDate ist erforderlich")
    @Pattern(regexp = "^\\d{4}-\\d{2}-\\d{2}$", message = "StartDate muss im Format YYYY-MM-DD sein")
    @Schema(description = "Start date", example = "2025-01-01", required = true)
    String startDate

    @NotBlank(message = "EndDate ist erforderlich")
    @Pattern(regexp = "^\\d{4}-\\d{2}-\\d{2}$", message = "EndDate muss im Format YYYY-MM-DD sein")
    @Schema(description = "End date", example = "2025-01-31", required = true)
    String endDate
}
