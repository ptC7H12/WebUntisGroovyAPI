package de.c7h12.webuntis.dto

import groovy.transform.CompileStatic
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Pattern

/**
 * Request DTO for fetching enhanced timetable via 2017 API
 */
@CompileStatic
@Schema(description = "Request for enhanced 2017 timetable with homework and colors")
class Timetable2017Request extends Enhanced2017Request {

    @NotBlank(message = "StartDate ist erforderlich")
    @Pattern(regexp = "^\\d{4}-\\d{2}-\\d{2}$", message = "StartDate muss im Format YYYY-MM-DD sein")
    @Schema(description = "Start date", example = "2025-01-01", required = true)
    String startDate

    @NotBlank(message = "EndDate ist erforderlich")
    @Pattern(regexp = "^\\d{4}-\\d{2}-\\d{2}$", message = "EndDate muss im Format YYYY-MM-DD sein")
    @Schema(description = "End date", example = "2025-01-31", required = true)
    String endDate

    @Schema(description = "Element type", example = "STUDENT", defaultValue = "STUDENT")
    String elementType = "STUDENT"
}
