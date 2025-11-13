package de.c7h12.webuntis.dto

import groovy.transform.CompileStatic
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Pattern

/**
 * Base authentication request DTO
 */
@CompileStatic
@Schema(description = "Authentication credentials for WebUntis API")
class AuthenticationRequest {

    @NotBlank(message = "School ist erforderlich")
    @Schema(description = "School name", example = "demo-school", required = true)
    String school

    @NotBlank(message = "Username ist erforderlich")
    @Schema(description = "Username", example = "student", required = true)
    String username

    @NotBlank(message = "Password ist erforderlich")
    @Schema(description = "Password", example = "password123", required = true)
    String password

    @NotBlank(message = "Server ist erforderlich")
    @Pattern(regexp = "^[a-zA-Z0-9.-]+$", message = "Server muss eine gültige Domain sein")
    @Schema(description = "WebUntis server hostname", example = "demo.webuntis.com", required = true)
    String server
}
