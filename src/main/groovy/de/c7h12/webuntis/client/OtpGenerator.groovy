package de.c7h12.webuntis.client

import de.c7h12.webuntis.constants.WebUntisConstants
import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import org.apache.commons.codec.binary.Base32
import org.springframework.stereotype.Component

import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import java.nio.ByteBuffer
import java.time.Instant

/**
 * TOTP (Time-based One-Time Password) generator for WebUntis 2017 API
 * Implements RFC 6238 TOTP algorithm
 */
@Slf4j
@CompileStatic
@Component
class OtpGenerator {

    /**
     * Generates a TOTP (Time-based One-Time Password) from App Secret
     * @param appSecret Base32-encoded secret key
     * @param currentTime Optional timestamp (defaults to now)
     * @return 6-digit OTP code
     */
    String generateTOTP(String appSecret, Long currentTime = null) {
        try {
            // Use current time if not provided
            long timeMillis = currentTime ?: Instant.now().toEpochMilli()
            long timeSeconds = timeMillis / 1000L

            // Calculate time step (30 second intervals)
            long timeStep = timeSeconds / WebUntisConstants.OTP_TIME_STEP_SECONDS

            // Decode Base32 App Secret
            byte[] decodedSecret = decodeBase32(appSecret)

            // Generate HMAC-SHA1 hash
            byte[] hash = generateHMAC(decodedSecret, timeStep)

            // Extract OTP from hash
            int otp = extractOTP(hash)

            // Format as 6-digit string with leading zeros
            String otpString = String.format("%06d", otp)

            log.debug("Generated TOTP: {} for timeStep: {}", otpString, timeStep)
            return otpString

        } catch (Exception e) {
            log.error("Failed to generate TOTP: {}", e.message, e)
            throw new WebUntisException("OTP-Generierung fehlgeschlagen: ${e.message}", e)
        }
    }

    /**
     * Decodes Base32-encoded secret key
     */
    private static byte[] decodeBase32(String appSecret) {
        try {
            // Remove any whitespace and convert to uppercase
            String cleanSecret = appSecret.replaceAll("\\s", "").toUpperCase()

            // Decode Base32
            Base32 base32 = new Base32()
            return base32.decode(cleanSecret)

        } catch (Exception e) {
            throw new IllegalArgumentException("Ungültiges App-Secret Format (Base32 erwartet)", e)
        }
    }

    /**
     * Generates HMAC-SHA1 hash of the time counter
     */
    private static byte[] generateHMAC(byte[] secret, long counter) {
        try {
            // Convert counter to 8-byte array (big-endian)
            ByteBuffer buffer = ByteBuffer.allocate(8)
            buffer.putLong(counter)
            byte[] timeBytes = buffer.array()

            // Create HMAC-SHA1 instance
            Mac hmac = Mac.getInstance(WebUntisConstants.OTP_ALGORITHM)
            SecretKeySpec keySpec = new SecretKeySpec(secret, WebUntisConstants.OTP_ALGORITHM)
            hmac.init(keySpec)

            // Generate hash
            return hmac.doFinal(timeBytes)

        } catch (Exception e) {
            throw new RuntimeException("HMAC-Generierung fehlgeschlagen", e)
        }
    }

    /**
     * Extracts OTP value from HMAC hash using dynamic truncation
     * Implements RFC 6238 dynamic truncation algorithm
     */
    private static int extractOTP(byte[] hash) {
        // Get offset from last 4 bits of hash
        int offset = hash[hash.length - 1] & 0x0F

        // Extract 4 bytes starting at offset
        int binary = ((hash[offset] & 0x7F) << 24) |
                     ((hash[offset + 1] & 0xFF) << 16) |
                     ((hash[offset + 2] & 0xFF) << 8) |
                     (hash[offset + 3] & 0xFF)

        // Modulo to get 6-digit code
        int divisor = (int) Math.pow(10, WebUntisConstants.OTP_DIGITS)
        return binary % divisor
    }

    /**
     * Validates if an OTP matches the expected value for a given time window
     * Allows for small time drift (±1 time step)
     */
    boolean validateTOTP(String appSecret, String providedOtp, int allowedTimeDrift = 1) {
        try {
            long currentTime = Instant.now().toEpochMilli()

            // Check current time and adjacent time windows
            for (int i = -allowedTimeDrift; i <= allowedTimeDrift; i++) {
                long adjustedTime = currentTime + (i * WebUntisConstants.OTP_TIME_STEP_SECONDS * 1000L)
                String generatedOtp = generateTOTP(appSecret, adjustedTime)

                if (generatedOtp == providedOtp) {
                    log.debug("TOTP validated successfully (drift: {} steps)", i)
                    return true
                }
            }

            log.warn("TOTP validation failed")
            return false

        } catch (Exception e) {
            log.error("TOTP validation error: {}", e.message, e)
            return false
        }
    }
}
