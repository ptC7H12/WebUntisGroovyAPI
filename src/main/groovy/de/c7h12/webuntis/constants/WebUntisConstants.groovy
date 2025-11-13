package de.c7h12.webuntis.constants

import groovy.transform.CompileStatic

/**
 * Constants for WebUntis API integration
 */
@CompileStatic
class WebUntisConstants {

    // Element Types for WebUntis API
    static final int ELEMENT_TYPE_CLASS = 1
    static final int ELEMENT_TYPE_TEACHER = 2
    static final int ELEMENT_TYPE_SUBJECT = 3
    static final int ELEMENT_TYPE_ROOM = 4
    static final int ELEMENT_TYPE_STUDENT = 5

    // Cache Settings
    static final long MASTER_DATA_CACHE_VALIDITY_MS = 5 * 60 * 1000 // 5 minutes

    // OTP/TOTP Settings
    static final int OTP_TIME_STEP_SECONDS = 30
    static final int OTP_DIGITS = 6
    static final String OTP_ALGORITHM = "HmacSHA1"

    // Lesson Status Codes
    static final String LESSON_CODE_REGULAR = "REGULAR"
    static final String LESSON_CODE_CANCELLED = "CANCELLED"
    static final String LESSON_CODE_EXAM = "EXAM"
    static final String LESSON_CODE_SUBSTITUTION = "SUBSTITUTION"
    static final String LESSON_CODE_BREAK_SUPERVISION = "BREAK_SUPERVISION"
    static final String LESSON_CODE_OFFICE_HOUR = "OFFICE_HOUR"

    // Homework Status
    static final String HOMEWORK_STATUS_PENDING = "pending"
    static final String HOMEWORK_STATUS_DUE_TODAY = "due_today"
    static final String HOMEWORK_STATUS_OVERDUE = "overdue"
    static final String HOMEWORK_STATUS_COMPLETED = "completed"

    // Holiday Types
    static final String HOLIDAY_TYPE_SUMMER = "Sommerferien"
    static final String HOLIDAY_TYPE_AUTUMN = "Herbstferien"
    static final String HOLIDAY_TYPE_CHRISTMAS = "Weihnachtsferien"
    static final String HOLIDAY_TYPE_WINTER = "Winterferien"
    static final String HOLIDAY_TYPE_EASTER = "Osterferien"
    static final String HOLIDAY_TYPE_PENTECOST = "Pfingstferien"

    // HTTP Client Settings
    static final String USER_AGENT = "SpringBoot-Groovy-WebUntis-Client"
    static final String CLIENT_NAME = "SpringBootGroovyApp"
    static final int DEFAULT_CONNECT_TIMEOUT = 5000
    static final int DEFAULT_READ_TIMEOUT = 10000

    // File Upload Settings
    static final long MAX_FILE_SIZE_BYTES = 10 * 1024 * 1024 // 10 MB
    static final List<String> ALLOWED_IMAGE_TYPES = ["image/jpeg", "image/png", "image/gif", "image/bmp"]
    static final List<String> ALLOWED_IMAGE_EXTENSIONS = ["jpg", "jpeg", "png", "gif", "bmp"]

    // Date Format Patterns
    static final String DATE_PATTERN_WEBUNTIS = "yyyyMMdd"
    static final String DATE_PATTERN_ISO = "yyyy-MM-dd"
    static final String TIME_PATTERN_WEBUNTIS = "HHmm"
    static final String TIME_PATTERN_FORMATTED = "HH:mm"

    // Weekdays (German)
    static final Map<Integer, String> WEEKDAY_NAMES = [
        1: "Montag",
        2: "Dienstag",
        3: "Mittwoch",
        4: "Donnerstag",
        5: "Freitag",
        6: "Samstag",
        7: "Sonntag"
    ].asImmutable()

    // API Endpoints
    static final String JSONRPC_ENDPOINT = "/WebUntis/jsonrpc.do"
    static final String JSONRPC_INTERN_ENDPOINT = "/WebUntis/jsonrpc_intern.do"
    static final String JSONRPC_VERSION = "2.0"

    // Error Messages (German)
    static final String ERROR_AUTHENTICATION_FAILED = "Authentifizierung fehlgeschlagen"
    static final String ERROR_INVALID_CREDENTIALS = "Ungültige Zugangsdaten"
    static final String ERROR_SESSION_EXPIRED = "Sitzung abgelaufen"
    static final String ERROR_INVALID_APP_SECRET = "Ungültiges App-Secret"
    static final String ERROR_QR_CODE_NOT_FOUND = "Kein QR-Code im Bild gefunden"
    static final String ERROR_INVALID_IMAGE_FORMAT = "Ungültiges Bildformat"
    static final String ERROR_FILE_TOO_LARGE = "Datei zu groß"
    static final String ERROR_NETWORK_ERROR = "Netzwerkfehler"

    // Character Encodings
    static final List<String> SUPPORTED_ENCODINGS = ["ISO-8859-1", "UTF-8", "Windows-1252"]

    private WebUntisConstants() {
        throw new UnsupportedOperationException("Constants class cannot be instantiated")
    }
}
