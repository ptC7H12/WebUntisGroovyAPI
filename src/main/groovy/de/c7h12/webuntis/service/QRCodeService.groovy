package de.c7h12.webuntis.service

import com.google.zxing.*
import com.google.zxing.client.j2se.BufferedImageLuminanceSource
import com.google.zxing.common.HybridBinarizer
import org.springframework.stereotype.Service
import org.springframework.web.multipart.MultipartFile

import javax.imageio.ImageIO
import java.awt.image.BufferedImage

@Service
class QRCodeService {

    /**
     * Extrahiert QR-Code Inhalt aus einem MultipartFile (Bild-Upload)
     */
    String extractQRCodeFromImage(MultipartFile file) {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("Datei ist leer")
        }

        try {
            // Bild aus MultipartFile laden
            BufferedImage image = ImageIO.read(file.getInputStream())

            if (image == null) {
                throw new IllegalArgumentException("Ungültiges Bildformat")
            }

            return decodeQRCode(image)

        } catch (Exception e) {
            throw new RuntimeException("Fehler beim Verarbeiten des Bildes: ${e.message}", e)
        }
    }

    /**
     * Extrahiert QR-Code Inhalt aus einem byte[] (z.B. von Telegram Bot)
     */
    String extractQRCodeFromBytes(byte[] imageBytes) {
        try {
            BufferedImage image = ImageIO.read(new ByteArrayInputStream(imageBytes))

            if (image == null) {
                throw new IllegalArgumentException("Ungültiges Bildformat")
            }

            return decodeQRCode(image)

        } catch (Exception e) {
            throw new RuntimeException("Fehler beim Verarbeiten des Bildes: ${e.message}", e)
        }
    }

    /**
     * Extrahiert QR-Code Inhalt aus Base64-kodiertem String
     */
    String extractQRCodeFromBase64(String base64Image) {
        try {
            // "data:image/png;base64," Prefix entfernen falls vorhanden
            def cleanBase64 = base64Image.replaceFirst("^data:image/[^;]+;base64,", "")

            byte[] imageBytes = Base64.getDecoder().decode(cleanBase64)
            return extractQRCodeFromBytes(imageBytes)

        } catch (Exception e) {
            throw new RuntimeException("Fehler beim Dekodieren des Base64-Bildes: ${e.message}", e)
        }
    }

    /**
     * Dekodiert QR-Code aus BufferedImage
     */
    private String decodeQRCode(BufferedImage image) {
        try {
            LuminanceSource source = new BufferedImageLuminanceSource(image)
            BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(source))

            // QR-Code dekodieren mit verschiedenen Hints für bessere Erkennung
            Map<DecodeHintType, Object> hints = [
                    (DecodeHintType.TRY_HARDER): Boolean.TRUE,
                    (DecodeHintType.POSSIBLE_FORMATS): [BarcodeFormat.QR_CODE],
                    (DecodeHintType.CHARACTER_SET): "UTF-8"
            ]

            Result result = new MultiFormatReader().decode(bitmap, hints)

            println "DEBUG: QR-Code erfolgreich dekodiert: ${result.getText()}"
            println "DEBUG: Format: ${result.getBarcodeFormat()}"

            return result.getText()

        } catch (NotFoundException e) {
            throw new RuntimeException("Kein QR-Code im Bild gefunden", e)
        } catch (Exception e) {
            throw new RuntimeException("Fehler beim Dekodieren des QR-Codes: ${e.message}", e)
        }
    }

    /**
     * Versucht QR-Code mit verschiedenen Bildvorverarbeitungen zu extrahieren
     * (nützlich bei schlechter Bildqualität)
     */
    String extractQRCodeWithEnhancement(MultipartFile file) {
        try {
            BufferedImage image = ImageIO.read(file.getInputStream())

            // Zuerst normalen Versuch
            try {
                return decodeQRCode(image)
            } catch (Exception e) {
                println "DEBUG: Normale Dekodierung fehlgeschlagen, versuche mit Bildverbesserung..."
            }

            // Graustufen-Konvertierung
            BufferedImage grayImage = convertToGrayscale(image)
            try {
                return decodeQRCode(grayImage)
            } catch (Exception e) {
                println "DEBUG: Graustufen-Dekodierung fehlgeschlagen..."
            }

            // Kontrast erhöhen
            BufferedImage contrastImage = increaseContrast(image)
            try {
                return decodeQRCode(contrastImage)
            } catch (Exception e) {
                println "DEBUG: Kontrast-Dekodierung fehlgeschlagen..."
            }

            throw new RuntimeException("QR-Code konnte auch mit Bildverbesserung nicht erkannt werden")

        } catch (Exception e) {
            throw new RuntimeException("Fehler bei der erweiterten QR-Code-Extraktion: ${e.message}", e)
        }
    }

    /**
     * Konvertiert Bild zu Graustufen
     */
    private BufferedImage convertToGrayscale(BufferedImage original) {
        BufferedImage gray = new BufferedImage(
                original.getWidth(),
                original.getHeight(),
                BufferedImage.TYPE_BYTE_GRAY
        )

        def graphics = gray.createGraphics()
        graphics.drawImage(original, 0, 0, null)
        graphics.dispose()

        return gray
    }

    /**
     * Erhöht Kontrast des Bildes
     */
    private BufferedImage increaseContrast(BufferedImage original) {
        BufferedImage contrasted = new BufferedImage(
                original.getWidth(),
                original.getHeight(),
                original.getType()
        )

        def factor = 1.5 // Kontrast-Faktor

        for (int y = 0; y < original.getHeight(); y++) {
            for (int x = 0; x < original.getWidth(); x++) {
                int rgb = original.getRGB(x, y)

                int r = (rgb >> 16) & 0xFF
                int g = (rgb >> 8) & 0xFF
                int b = rgb & 0xFF

                r = (int) Math.min(255, Math.max(0, ((r - 128) * factor) + 128))
                g = (int) Math.min(255, Math.max(0, ((g - 128) * factor) + 128))
                b = (int) Math.min(255, Math.max(0, ((b - 128) * factor) + 128))

                int newRgb = (r << 16) | (g << 8) | b
                contrasted.setRGB(x, y, newRgb)
            }
        }

        return contrasted
    }

    /**
     * Validiert ob eine Datei ein gültiges Bildformat hat
     */
    boolean isValidImageFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return false
        }

        def contentType = file.getContentType()
        def validTypes = ['image/jpeg', 'image/png', 'image/jpg', 'image/gif', 'image/bmp']

        return validTypes.contains(contentType?.toLowerCase())
    }

    /**
     * Parst WebUntis untis:// URI und extrahiert alle Parameter
     */
    Map parseUntisUri(String qrContent) {
        if (!qrContent) {
            return [success: false, error: "QR-Content ist leer"]
        }

        try {
            // Prüfen ob es ein untis:// URI ist
            if (qrContent.startsWith("untis://setschool?")) {
                return parseUntisSetSchoolUri(qrContent)
            }
            // Prüfen ob es ein otpauth:// URI ist (für App Secret)
            else if (qrContent.startsWith("otpauth://totp/")) {
                return parseOtpauthUri(qrContent)
            }
            // Fallback: Rohen Content zurückgeben
            else {
                return [
                        success: true,
                        type: "unknown",
                        content: qrContent
                ]
            }
        } catch (Exception e) {
            println "ERROR: Fehler beim Parsen des URI: ${e.message}"
            return [
                    success: false,
                    error: "Fehler beim Parsen: ${e.message}",
                    rawContent: qrContent
            ]
        }
    }

    /**
     * Parst untis://setschool? URI
     * Format: untis://setschool?url=hepta.webuntis.com&school=fwm-ges-bielefeld&user=aaron.heptin&key=OL6KK2YTPFCZ4JLU&schoolNumber=5200900
     */
    private Map parseUntisSetSchoolUri(String uri) {
        def result = [success: true, type: "untis_setschool"]

        try {
            // Query-String extrahieren (alles nach dem "?")
            def queryString = uri.substring(uri.indexOf("?") + 1)

            // Parameter parsen
            def params = parseQueryString(queryString)

            // Standard-Parameter extrahieren
            result.url = params.url ?: null
            result.school = params.school ?: null
            result.user = params.user ?: null
            result.key = params.key ?: null
            result.schoolNumber = params.schoolNumber ?: null

            // Optional: Server normalisieren (https:// hinzufügen falls nicht vorhanden)
            if (result.url && !result.url.startsWith("http")) {
                result.server = "https://${result.url}"
            } else {
                result.server = result.url
            }

            // Alle weiteren Parameter auch hinzufügen
            params.each { key, value ->
                if (!result.containsKey(key)) {
                    result[key] = value
                }
            }

            println "DEBUG: Parsed untis:// URI: ${result}"
            return result

        } catch (Exception e) {
            println "ERROR: Fehler beim Parsen des untis:// URI: ${e.message}"
            return [
                    success: false,
                    error: "Fehler beim Parsen des untis:// URI: ${e.message}",
                    rawUri: uri
            ]
        }
    }

    /**
     * Parst otpauth:// TOTP URI
     * Format: otpauth://totp/WebUntis:username?secret=ABCDEF123456&issuer=WebUntis
     */
    private Map parseOtpauthUri(String uri) {
        def result = [success: true, type: "otpauth_totp"]

        try {
            // Label extrahieren (zwischen totp/ und ?)
            def labelPart = uri.substring("otpauth://totp/".length(), uri.indexOf("?"))

            // Issuer und Username aus Label extrahieren
            if (labelPart.contains(":")) {
                def parts = labelPart.split(":")
                result.issuer = URLDecoder.decode(parts[0], "UTF-8")
                result.username = URLDecoder.decode(parts[1], "UTF-8")
            } else {
                result.label = URLDecoder.decode(labelPart, "UTF-8")
            }

            // Query-String extrahieren
            def queryString = uri.substring(uri.indexOf("?") + 1)
            def params = parseQueryString(queryString)

            result.secret = params.secret ?: null
            result.algorithm = params.algorithm ?: "SHA1"
            result.digits = params.digits ? params.digits.toInteger() : 6
            result.period = params.period ? params.period.toInteger() : 30

            // Alle weiteren Parameter
            params.each { key, value ->
                if (!result.containsKey(key)) {
                    result[key] = value
                }
            }

            println "DEBUG: Parsed otpauth:// URI: ${result}"
            return result

        } catch (Exception e) {
            println "ERROR: Fehler beim Parsen des otpauth:// URI: ${e.message}"
            return [
                    success: false,
                    error: "Fehler beim Parsen des otpauth:// URI: ${e.message}",
                    rawUri: uri
            ]
        }
    }

    /**
     * Parst Query-String zu Map
     * Beispiel: "url=test.com&school=myschool" -> [url: "test.com", school: "myschool"]
     */
    private Map<String, String> parseQueryString(String queryString) {
        def params = [:]

        queryString.split("&").each { param ->
            def parts = param.split("=", 2)
            if (parts.length == 2) {
                def key = URLDecoder.decode(parts[0], "UTF-8")
                def value = URLDecoder.decode(parts[1], "UTF-8")
                params[key] = value
            }
        }

        return params
    }
}