package de.c7h12.webuntis.dto

import groovy.transform.CompileStatic
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank

/**
 * Request DTO for QR code extraction from Base64 image
 */
@CompileStatic
@Schema(description = "Request for QR code extraction from Base64 encoded image")
class QRCodeBase64Request {

    @NotBlank(message = "Image ist erforderlich")
    @Schema(description = "Base64 encoded image (with or without data URI prefix)", required = true)
    String image
}
