package de.c7h12.webuntis.dto

import groovy.transform.CompileStatic
import io.swagger.v3.oas.annotations.media.Schema

/**
 * Request DTO for fetching holidays via 2017 API
 */
@CompileStatic
@Schema(description = "Request for fetching school holidays")
class Holidays2017Request extends Enhanced2017Request {

    @Schema(description = "Filter by school year ID", example = "1")
    Integer schoolyearId
}
