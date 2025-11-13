package de.c7h12.webuntis.dto

import groovy.transform.CompileStatic
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank

/**
 * Base request DTO for 2017 API endpoints (requires appSecret)
 */
@CompileStatic
@Schema(description = "Authentication request for enhanced 2017 API (requires App Secret)")
class Enhanced2017Request extends AuthenticationRequest {

    @NotBlank(message = "AppSecret ist für 2017 API Methoden erforderlich")
    @Schema(description = "App Secret for 2017 API authentication", example = "ABCD1234EFGH5678", required = true)
    String appSecret
}
