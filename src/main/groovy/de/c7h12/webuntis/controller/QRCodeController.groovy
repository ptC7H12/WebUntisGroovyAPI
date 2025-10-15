package de.c7h12.webuntis.controller

import de.c7h12.webuntis.service.QRCodeService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile

@RestController
@RequestMapping("/api/qrcode")
@CrossOrigin(origins = "*")
class QRCodeController {

    @Autowired
    QRCodeService qrCodeService

    /**
     * Endpoint für Bild-Upload (Multipart Form Data)
     * Verwendung: Direkter Upload von Dateien
     */
    @PostMapping(value = "/extract", consumes = "multipart/form-data")
    ResponseEntity<?> extractQRCodeFromUpload(@RequestParam("file") MultipartFile file) {
        try {
            // Bildformat validieren
            if (!qrCodeService.isValidImageFile(file)) {
                return ResponseEntity.badRequest()
                        .body([
                                success: false,
                                error: "Ungültiges Bildformat. Erlaubt sind: JPEG, PNG, GIF, BMP"
                        ])
            }

            // QR-Code extrahieren
            def qrContent = qrCodeService.extractQRCodeFromImage(file)

            // URI parsen und strukturiert zurückgeben
            def parsedData = qrCodeService.parseUntisUri(qrContent)

            // Metadaten hinzufügen
            parsedData.fileName = file.getOriginalFilename()
            parsedData.fileSize = file.getSize()
            parsedData.timestamp = System.currentTimeMillis()

            // Optional: Raw Content für Debugging
            parsedData.rawContent = qrContent

            return ResponseEntity.ok(parsedData)

        } catch (Exception e) {
            println "ERROR: QR-Code Extraktion fehlgeschlagen: ${e.message}"
            return ResponseEntity.badRequest()
                    .body([
                            success: false,
                            error: "Fehler beim Extrahieren des QR-Codes: ${e.message}"
                    ])
        }
    }

    /**
     * Endpoint für Base64-kodierte Bilder
     * Verwendung: Für n8n Flow und Telegram Bot Integration
     */
    @PostMapping("/extract/base64")
    ResponseEntity<?> extractQRCodeFromBase64(@RequestBody Map request) {
        try {
            def base64Image = request.image as String

            if (!base64Image) {
                return ResponseEntity.badRequest()
                        .body([
                                success: false,
                                error: "Kein Base64-Bild im Request gefunden. Sende: {\"image\": \"base64string\"}"
                        ])
            }

            // QR-Code extrahieren
            def qrContent = qrCodeService.extractQRCodeFromBase64(base64Image)

            // URI parsen und strukturiert zurückgeben
            def parsedData = qrCodeService.parseUntisUri(qrContent)
            parsedData.timestamp = System.currentTimeMillis()
            parsedData.rawContent = qrContent

            return ResponseEntity.ok(parsedData)

        } catch (Exception e) {
            println "ERROR: Base64 QR-Code Extraktion fehlgeschlagen: ${e.message}"
            return ResponseEntity.badRequest()
                    .body([
                            success: false,
                            error: "Fehler beim Extrahieren des QR-Codes: ${e.message}"
                    ])
        }
    }

    /**
     * Endpoint für binäre Bilddaten
     * Verwendung: Direkter Binary Upload
     */
    @PostMapping(value = "/extract/binary", consumes = "application/octet-stream")
    ResponseEntity<?> extractQRCodeFromBinary(@RequestBody byte[] imageBytes) {
        try {
            if (imageBytes == null || imageBytes.length == 0) {
                return ResponseEntity.badRequest()
                        .body([
                                success: false,
                                error: "Keine Bilddaten empfangen"
                        ])
            }

            // QR-Code extrahieren
            def qrContent = qrCodeService.extractQRCodeFromBytes(imageBytes)

            // URI parsen und strukturiert zurückgeben
            def parsedData = qrCodeService.parseUntisUri(qrContent)
            parsedData.imageSize = imageBytes.length
            parsedData.timestamp = System.currentTimeMillis()
            parsedData.rawContent = qrContent

            return ResponseEntity.ok(parsedData)

        } catch (Exception e) {
            println "ERROR: Binary QR-Code Extraktion fehlgeschlagen: ${e.message}"
            return ResponseEntity.badRequest()
                    .body([
                            success: false,
                            error: "Fehler beim Extrahieren des QR-Codes: ${e.message}"
                    ])
        }
    }

    /**
     * Endpoint mit erweiterter Bildverarbeitung
     * Verwendung: Für Bilder mit schlechter Qualität
     */
    @PostMapping(value = "/extract/enhanced", consumes = "multipart/form-data")
    ResponseEntity<?> extractQRCodeEnhanced(@RequestParam("file") MultipartFile file) {
        try {
            if (!qrCodeService.isValidImageFile(file)) {
                return ResponseEntity.badRequest()
                        .body([
                                success: false,
                                error: "Ungültiges Bildformat. Erlaubt sind: JPEG, PNG, GIF, BMP"
                        ])
            }

            // QR-Code mit Bildverbesserung extrahieren
            def qrContent = qrCodeService.extractQRCodeWithEnhancement(file)

            // URI parsen und strukturiert zurückgeben
            def parsedData = qrCodeService.parseUntisUri(qrContent)
            parsedData.fileName = file.getOriginalFilename()
            parsedData.fileSize = file.getSize()
            parsedData.enhanced = true
            parsedData.timestamp = System.currentTimeMillis()
            parsedData.rawContent = qrContent

            return ResponseEntity.ok(parsedData)

        } catch (Exception e) {
            println "ERROR: Enhanced QR-Code Extraktion fehlgeschlagen: ${e.message}"
            return ResponseEntity.badRequest()
                    .body([
                            success: false,
                            error: "Fehler beim Extrahieren des QR-Codes: ${e.message}"
                    ])
        }
    }

    /**
     * Health-Check Endpoint
     */
    @GetMapping("/health")
    ResponseEntity<?> healthCheck() {
        return ResponseEntity.ok([
                status: "ok",
                service: "QRCode Extraction Service",
                version: "1.0",
                availableEndpoints: [
                        "/api/qrcode/extract (POST, multipart/form-data)",
                        "/api/qrcode/extract/base64 (POST, JSON)",
                        "/api/qrcode/extract/binary (POST, binary)",
                        "/api/qrcode/extract/enhanced (POST, multipart/form-data)",
                        "/api/qrcode/extract/authenticate (POST, multipart/form-data)"
                ]
        ])
    }

    /**
     * BONUS: QR-Code extrahieren + direkt mit WebUntis authentifizieren
     * Verwendung: One-Shot Operation für n8n
     */
    @PostMapping(value = "/extract/authenticate", consumes = "multipart/form-data")
    ResponseEntity<?> extractAndAuthenticateWebUntis(@RequestParam("file") MultipartFile file) {
        try {
            if (!qrCodeService.isValidImageFile(file)) {
                return ResponseEntity.badRequest()
                        .body([
                                success: false,
                                error: "Ungültiges Bildformat. Erlaubt sind: JPEG, PNG, GIF, BMP"
                        ])
            }

            // QR-Code extrahieren
            def qrContent = qrCodeService.extractQRCodeFromImage(file)

            // URI parsen
            def parsedData = qrCodeService.parseUntisUri(qrContent)

            // Prüfen ob es WebUntis Daten sind
            if (parsedData.type != "untis_setschool") {
                parsedData.authenticated = false
                parsedData.authError = "QR-Code enthält keine WebUntis Login-Daten"
                return ResponseEntity.ok(parsedData)
            }

            // Mit WebUntis authentifizieren (benötigt WebUntisClient)
            // HINWEIS: Dieser Teil ist optional und benötigt @Autowired WebUntisClient
            /*
            try {
                def session = webUntisClient.authenticateWithSecret(
                    parsedData.school as String,
                    parsedData.user as String,
                    parsedData.key as String,
                    parsedData.server as String
                )

                parsedData.authenticated = true
                parsedData.sessionId = session.sessionId
                parsedData.personId = session.personId

                // Optional: User-Daten laden
                def userData = webUntisClient.getUserData2017(session)
                if (userData?.userData?.displayName) {
                    parsedData.displayName = userData.userData.displayName
                }

            } catch (Exception authError) {
                parsedData.authenticated = false
                parsedData.authError = "Authentifizierung fehlgeschlagen: ${authError.message}"
            }
            */

            // Für jetzt: Nur geparste Daten zurückgeben
            parsedData.authenticated = false
            parsedData.authNote = "Automatische Authentifizierung noch nicht aktiviert. Verwende /api/webuntis/v2017/* Endpoints."
            parsedData.timestamp = System.currentTimeMillis()
            parsedData.rawContent = qrContent

            return ResponseEntity.ok(parsedData)

        } catch (Exception e) {
            println "ERROR: QR-Code Extraktion/Auth fehlgeschlagen: ${e.message}"
            return ResponseEntity.badRequest()
                    .body([
                            success: false,
                            error: "Fehler: ${e.message}"
                    ])
        }
    }
}