package de.c7h12.webuntis.client

import com.fasterxml.jackson.databind.JsonNode
import de.c7h12.webuntis.constants.WebUntisConstants
import de.c7h12.webuntis.parser.WebUntisMasterDataParser
import de.c7h12.webuntis.parser.WebUntisTimetableParser
import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import org.springframework.stereotype.Component

import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * Main WebUntis API client
 * Coordinates authentication, session management, and API calls
 *
 * Refactored to use:
 * - WebUntisHttpClient for HTTP communication
 * - OtpGenerator for TOTP generation
 * - WebUntisMasterDataParser for parsing master data
 * - WebUntisTimetableParser for parsing timetable data
 */
@Slf4j
@CompileStatic
@Component
class WebUntisClient {

    private final WebUntisHttpClient httpClient
    private final OtpGenerator otpGenerator
    private final WebUntisMasterDataParser masterDataParser
    private final WebUntisTimetableParser timetableParser

    WebUntisClient(
            WebUntisHttpClient httpClient,
            OtpGenerator otpGenerator,
            WebUntisMasterDataParser masterDataParser,
            WebUntisTimetableParser timetableParser
    ) {
        this.httpClient = httpClient
        this.otpGenerator = otpGenerator
        this.masterDataParser = masterDataParser
        this.timetableParser = timetableParser
    }

    // ========== Authentication Methods ==========

    /**
     * Authenticates with WebUntis using username and password (Standard API)
     */
    WebUntisSession authenticate(String school, String username, String password, String server) {
        try {
            def normalizedServer = httpClient.normalizeServer(server)
            def uri = httpClient.buildJsonRpcUri(normalizedServer, school)

            def params = [
                    user    : username,
                    password: password,
                    client  : WebUntisConstants.CLIENT_NAME
            ]

            def request = httpClient.createJsonRpcRequest("authenticate", params)
            def result = httpClient.executeJsonRpc(uri, request)

            def sessionId = result.get("sessionId").asText()
            def personId = result.get("personId").asInt()

            log.info("Authentication successful for user: {} in school: {}", username, school)

            return WebUntisSession.builder()
                    .sessionId(sessionId)
                    .personId(personId)
                    .school(school)
                    .server(normalizedServer)
                    .build()

        } catch (Exception e) {
            log.error("Authentication failed: {}", e.message, e)
            throw new WebUntisException("Authentifizierung fehlgeschlagen: ${e.message}", e)
        }
    }

    /**
     * Authenticates with WebUntis using App Secret (2017 API)
     * Automatically generates OTP from App Secret
     */
    WebUntisSession authenticateWithSecret(String school, String username, String appSecret, String server) {
        try {
            def normalizedServer = httpClient.normalizeServer(server)
            def uri = httpClient.buildJsonRpcInternUri(normalizedServer, school)

            def currentTime = System.currentTimeMillis()
            def otp = otpGenerator.generateTOTP(appSecret, currentTime)

            def params = [[
                    masterDataTimestamp: currentTime.toString(),
                    type: "STUDENT",
                    auth: [
                            user: username,
                            otp: otp,
                            clientTime: currentTime.toString()
                    ]
            ]]

            def request = httpClient.createJsonRpcRequest("getUserData2017", params)
            def result = httpClient.executeJsonRpc(uri, request)

            def userData = result.get("userData")
            def masterData = result.get("masterData")

            def sessionId = "enhanced-session-${currentTime}"
            def personId = userData.get("elemId").asInt()

            def session = WebUntisSession.builder()
                    .sessionId(sessionId)
                    .personId(personId)
                    .school(school)
                    .server(normalizedServer)
                    .appSecret(appSecret)
                    .username(username)
                    .build()

            // Parse and cache master data
            session.masterData = masterDataParser.parseMasterData(masterData)

            log.info("Enhanced authentication successful for user: {} in school: {}", username, school)
            if (userData.has("displayName")) {
                log.info("User display name: {}", userData.get("displayName").asText())
            }

            return session

        } catch (Exception e) {
            log.error("Enhanced authentication failed: {}", e.message, e)
            throw new WebUntisException("Erweiterte Authentifizierung fehlgeschlagen: ${e.message}", e)
        }
    }

    /**
     * Logs out from WebUntis session
     */
    void logout(WebUntisSession session) {
        if (!session?.sessionId) {
            return
        }

        try {
            def uri = httpClient.buildJsonRpcUri(session.server, session.school)
            def request = httpClient.createJsonRpcRequest("logout", [:])
            httpClient.executeJsonRpc(uri, request, session)

            log.debug("Successfully logged out session: {}", session.sessionId)
        } catch (Exception e) {
            log.debug("Logout failed (non-critical): {}", e.message)
        }
    }

    // ========== Standard API Methods ==========

    /**
     * Gets timetable for a date range (Standard API)
     */
    List<Map> getTimetable(WebUntisSession session, LocalDate startDate, LocalDate endDate, int elementId, int elementType) {
        try {
            def uri = httpClient.buildJsonRpcInternUri(session.server, session.school)

            def element = [
                    type: elementType,
                    id  : elementId
            ]

            def options = [
                    startDate        : startDate.format(DateTimeFormatter.ofPattern("yyyyMMdd")),
                    endDate          : endDate.format(DateTimeFormatter.ofPattern("yyyyMMdd")),
                    element          : element,
                    onlyBaseTimetable: false,
                    showInfo         : true,
                    showSubstText    : true,
                    showLsText       : true,
                    showLsNumber     : true,
                    showStudentgroup : true
            ]

            def request = httpClient.createJsonRpcRequest("getTimetable", [options: options])
            def result = httpClient.executeJsonRpc(uri, request, session)

            return timetableParser.parseAdvancedTimetableEntries(result)

        } catch (Exception e) {
            log.error("Failed to get timetable: {}", e.message, e)
            throw new WebUntisException("Fehler beim Abrufen des Stundenplans: ${e.message}", e)
        }
    }

    /**
     * Gets subjects list (Standard API)
     */
    List<Map> getSubjects(WebUntisSession session) {
        try {
            def uri = httpClient.buildJsonRpcUri(session.server, session.school)
            def request = httpClient.createJsonRpcRequest("getSubjects", [:])
            def result = httpClient.executeJsonRpc(uri, request, session)

            return masterDataParser.parseSubjects(result)

        } catch (Exception e) {
            log.error("Failed to get subjects: {}", e.message, e)
            throw new WebUntisException("Fehler beim Abrufen der Fächer: ${e.message}", e)
        }
    }

    /**
     * Gets teachers list (Standard API)
     */
    List<Map> getTeachers(WebUntisSession session) {
        try {
            def uri = httpClient.buildJsonRpcUri(session.server, session.school)
            def request = httpClient.createJsonRpcRequest("getTeachers", [:])
            def result = httpClient.executeJsonRpc(uri, request, session)

            return masterDataParser.parseTeachers(result)

        } catch (Exception e) {
            log.error("Failed to get teachers: {}", e.message, e)
            throw new WebUntisException("Fehler beim Abrufen der Lehrer: ${e.message}", e)
        }
    }

    /**
     * Gets rooms list (Standard API)
     */
    List<Map> getRooms(WebUntisSession session) {
        try {
            def uri = httpClient.buildJsonRpcUri(session.server, session.school)
            def request = httpClient.createJsonRpcRequest("getRooms", [:])
            def result = httpClient.executeJsonRpc(uri, request, session)

            return masterDataParser.parseRooms(result)

        } catch (Exception e) {
            log.error("Failed to get rooms: {}", e.message, e)
            throw new WebUntisException("Fehler beim Abrufen der Räume: ${e.message}", e)
        }
    }

    /**
     * Gets classes list (Standard API)
     */
    List<Map> getClasses(WebUntisSession session) {
        try {
            def uri = httpClient.buildJsonRpcUri(session.server, session.school)
            def request = httpClient.createJsonRpcRequest("getKlassen", [:])
            def result = httpClient.executeJsonRpc(uri, request, session)

            return masterDataParser.parseClasses(result)

        } catch (Exception e) {
            log.error("Failed to get classes: {}", e.message, e)
            throw new WebUntisException("Fehler beim Abrufen der Klassen: ${e.message}", e)
        }
    }

    // ========== Enhanced 2017 API Methods ==========

    /**
     * Gets enhanced timetable with master data (2017 API)
     */
    List<Map> getTimetable2017(WebUntisSession session, LocalDate startDate, LocalDate endDate, int elementId, String elementType) {
        try {
            ensureUserData2017(session)

            def uri = httpClient.buildJsonRpcInternUri(session.server, session.school)
            def currentTime = System.currentTimeMillis()
            def otp = otpGenerator.generateTOTP(session.appSecret, currentTime)

            def params = [[
                    id: elementId,
                    type: elementType,
                    startDate: startDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")),
                    endDate: endDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")),
                    masterDataTimestamp: currentTime.toString(),
                    auth: [
                            user: session.username,
                            otp: otp,
                            clientTime: currentTime.toString()
                    ]
            ]]

            def request = httpClient.createJsonRpcRequest("getTimetable2017", params)
            def result = httpClient.executeJsonRpc(uri, request)

            // Extract periods from response
            def timetableData
            if (result.has("timetable")) {
                def timetable = result.get("timetable")
                timetableData = timetable.has("periods") ? timetable.get("periods") : timetable
            } else {
                timetableData = result
            }

            return timetableParser.parseWebUntis2017PeriodsFormat(timetableData, session.masterData)

        } catch (Exception e) {
            log.error("Failed to get 2017 timetable: {}", e.message, e)
            throw new WebUntisException("Fehler beim Abrufen des erweiterten Stundenplans: ${e.message}", e)
        }
    }

    /**
     * Gets homework for date range (2017 API)
     */
    List<Map> getHomework2017(WebUntisSession session, LocalDate startDate, LocalDate endDate, int studentId) {
        try {
            ensureUserData2017(session)

            def uri = httpClient.buildJsonRpcInternUri(session.server, session.school)
            def currentTime = System.currentTimeMillis()
            def otp = otpGenerator.generateTOTP(session.appSecret, currentTime)

            def params = [[
                    id: studentId,
                    type: "STUDENT",
                    startDate: startDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")),
                    endDate: endDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")),
                    masterDataTimestamp: currentTime.toString(),
                    auth: [
                            user: session.username,
                            otp: otp,
                            clientTime: currentTime.toString()
                    ]
            ]]

            def request = httpClient.createJsonRpcRequest("getHomeWork2017", params)
            def result = httpClient.executeJsonRpc(uri, request)

            return parseHomework2017WithMasterData(result, session.masterData)

        } catch (Exception e) {
            log.error("Failed to get homework: {}", e.message, e)
            throw new WebUntisException("Fehler beim Abrufen der Hausaufgaben: ${e.message}", e)
        }
    }

    /**
     * Gets messages for a specific day (2017 API)
     */
    List<Map> getMessagesOfDay2017(WebUntisSession session, LocalDate date) {
        try {
            ensureUserData2017(session)

            def uri = httpClient.buildJsonRpcInternUri(session.server, session.school)
            def currentTime = System.currentTimeMillis()
            def otp = otpGenerator.generateTOTP(session.appSecret, currentTime)

            def params = [[
                    date: date.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")),
                    masterDataTimestamp: currentTime.toString(),
                    auth: [
                            user: session.username,
                            otp: otp,
                            clientTime: currentTime.toString()
                    ]
            ]]

            def request = httpClient.createJsonRpcRequest("getMessagesOfDay2017", params)
            def result = httpClient.executeJsonRpc(uri, request)

            return parseMessages(result)

        } catch (Exception e) {
            log.error("Failed to get messages: {}", e.message, e)
            throw new WebUntisException("Fehler beim Abrufen der Nachrichten: ${e.message}", e)
        }
    }

    /**
     * Gets student absences for date range (2017 API)
     */
    List<Map> getStudentAbsences2017(WebUntisSession session, LocalDate startDate, LocalDate endDate,
                                     boolean includeExcused, boolean includeUnexcused) {
        try {
            ensureUserData2017(session)

            def uri = httpClient.buildJsonRpcInternUri(session.server, session.school)
            def currentTime = System.currentTimeMillis()
            def otp = otpGenerator.generateTOTP(session.appSecret, currentTime)

            def params = [[
                    studentId: session.personId,
                    startDate: startDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")),
                    endDate: endDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")),
                    includeExcused: includeExcused,
                    includeUnexcused: includeUnexcused,
                    masterDataTimestamp: currentTime.toString(),
                    auth: [
                            user: session.username,
                            otp: otp,
                            clientTime: currentTime.toString()
                    ]
            ]]

            def request = httpClient.createJsonRpcRequest("getStudentAbsences2017", params)
            def result = httpClient.executeJsonRpc(uri, request)

            return parseAbsences(result)

        } catch (Exception e) {
            log.error("Failed to get absences: {}", e.message, e)
            throw new WebUntisException("Fehler beim Abrufen der Abwesenheiten: ${e.message}", e)
        }
    }

    /**
     * Gets complete user data and master data (2017 API)
     */
    Map getUserData2017(WebUntisSession session) {
        try {
            def uri = httpClient.buildJsonRpcInternUri(session.server, session.school)
            def currentTime = System.currentTimeMillis()
            def otp = otpGenerator.generateTOTP(session.appSecret, currentTime)

            def params = [[
                    masterDataTimestamp: currentTime.toString(),
                    type: "STUDENT",
                    auth: [
                            user: session.username,
                            otp: otp,
                            clientTime: currentTime.toString()
                    ]
            ]]

            def request = httpClient.createJsonRpcRequest("getUserData2017", params)
            def result = httpClient.executeJsonRpc(uri, request)

            // Update session with master data
            if (result.has("masterData")) {
                session.masterData = masterDataParser.parseMasterData(result.get("masterData"))
            }

            return parseUserData(result)

        } catch (Exception e) {
            log.error("Failed to get user data: {}", e.message, e)
            throw new WebUntisException("Fehler beim Abrufen der Benutzerdaten: ${e.message}", e)
        }
    }

    // ========== Helper Methods ==========

    /**
     * Ensures that master data is loaded and valid in session
     */
    private void ensureUserData2017(WebUntisSession session) {
        if (!session.hasMasterData()) {
            log.debug("Master data not cached, loading getUserData2017...")
            loadUserData2017(session)
        } else if (!session.isMasterDataValid()) {
            log.debug("Master data cache expired, refreshing...")
            loadUserData2017(session)
        } else {
            log.debug("Using cached master data (age: {}ms)", session.masterDataAge)
        }
    }

    /**
     * Loads user data and master data without creating a new session
     */
    private void loadUserData2017(WebUntisSession session) {
        try {
            def uri = httpClient.buildJsonRpcInternUri(session.server, session.school)
            def currentTime = System.currentTimeMillis()
            def otp = otpGenerator.generateTOTP(session.appSecret, currentTime)

            def params = [[
                    masterDataTimestamp: currentTime.toString(),
                    type: "STUDENT",
                    auth: [
                            user: session.username,
                            otp: otp,
                            clientTime: currentTime.toString()
                    ]
            ]]

            def request = httpClient.createJsonRpcRequest("getUserData2017", params)
            def result = httpClient.executeJsonRpc(uri, request)

            // Update master data in session
            if (result.has("masterData")) {
                session.masterData = masterDataParser.parseMasterData(result.get("masterData"))
            }

            // Update person ID if needed
            if (result.has("userData")) {
                def userData = result.get("userData")
                if (userData.has("elemId")) {
                    def newPersonId = userData.get("elemId").asInt()
                    if (session.personId != newPersonId) {
                        log.debug("Updating personId from {} to {}", session.personId, newPersonId)
                        session.personId = newPersonId
                    }
                }
            }

            log.debug("Master data successfully loaded and cached")

        } catch (Exception e) {
            log.error("Failed to load user data: {}", e.message, e)
            throw new WebUntisException("Fehler beim Laden der Stammdaten: ${e.message}", e)
        }
    }

    // ========== Parser Methods for 2017 API ==========

    private List<Map> parseHomework2017WithMasterData(JsonNode result, Map masterData) {
        def homework = []

        if (result.has("homeWorks")) {
            result.get("homeWorks").each { hw ->
                def homeworkEntry = [
                        id: hw.get("id").asLong(),
                        lessonId: hw.has("lessonId") && !hw.get("lessonId").isNull() ? hw.get("lessonId").asLong() : null,
                        startDate: hw.has("startDate") && !hw.get("startDate").isNull() ? hw.get("startDate").asText() : null,
                        endDate: hw.has("endDate") && !hw.get("endDate").isNull() ? hw.get("endDate").asText() : null,
                        text: hw.has("text") && !hw.get("text").isNull() ? hw.get("text").asText() : "",
                        remark: hw.has("remark") && !hw.get("remark").isNull() ? hw.get("remark").asText() : null,
                        completed: hw.has("completed") ? hw.get("completed").asBoolean() : false,
                        attachments: hw.has("attachments") ? parseAttachments(hw.get("attachments")) : []
                ]

                // Resolve lesson information
                if (hw.has("lessonId") && !hw.get("lessonId").isNull() && result.has("lessonsById")) {
                    def lessonId = hw.get("lessonId").asLong().toString()
                    def lessonsById = result.get("lessonsById")

                    if (lessonsById.has(lessonId)) {
                        def lesson = lessonsById.get(lessonId)

                        // Resolve subject
                        if (lesson.has("subjectId") && !lesson.get("subjectId").isNull()) {
                            def subjectId = lesson.get("subjectId").asLong()
                            homeworkEntry.subject = masterData?.subjects?[subjectId]
                        }

                        // Resolve classes
                        if (lesson.has("klassenIds")) {
                            def classes = []
                            lesson.get("klassenIds").each { klassenIdNode ->
                                def klassenId = klassenIdNode.asLong()
                                def clazz = masterData?.klassen?[klassenId] ?: [id: klassenId, name: "Klasse-${klassenId}", longName: "Klasse-${klassenId}"]
                                classes << clazz
                            }
                            homeworkEntry.classes = classes
                        }

                        // Resolve teachers
                        if (lesson.has("teacherIds")) {
                            def teachers = []
                            lesson.get("teacherIds").each { teacherIdNode ->
                                def teacherId = teacherIdNode.asLong()
                                def teacher = masterData?.teachers?[teacherId] ?: [id: teacherId, name: "Lehrer-${teacherId}", firstName: "", lastName: ""]
                                teachers << teacher
                            }
                            homeworkEntry.teachers = teachers
                        }
                    }
                }

                homework << homeworkEntry
            }
        }

        log.debug("Parsed {} homework entries", homework.size())
        return homework
    }

    private List<Map> parseAttachments(JsonNode attachments) {
        def result = []
        attachments.each { attachment ->
            result << [
                    id: attachment.get("id").asLong(),
                    name: attachment.has("name") ? attachment.get("name").asText() : "",
                    url: attachment.has("url") ? attachment.get("url").asText() : null
            ]
        }
        return result
    }

    private List<Map> parseMessages(JsonNode result) {
        def messages = []
        if (result.has("messages")) {
            result.get("messages").each { message ->
                messages << [
                        id: message.get("id").asLong(),
                        subject: message.has("subject") ? message.get("subject").asText() : "",
                        text: message.has("text") ? message.get("text").asText() : "",
                        isRead: message.has("isRead") ? message.get("isRead").asBoolean() : false,
                        date: message.has("date") ? message.get("date").asInt() : null,
                        sender: message.has("sender") ? message.get("sender").asText() : ""
                ]
            }
        }
        return messages
    }

    private List<Map> parseAbsences(JsonNode result) {
        def absences = []
        if (result.has("absences")) {
            result.get("absences").each { absence ->
                absences << [
                        id: absence.get("id").asLong(),
                        startDate: absence.has("startDate") ? absence.get("startDate").asInt() : null,
                        endDate: absence.has("endDate") ? absence.get("endDate").asInt() : null,
                        startTime: absence.has("startTime") ? absence.get("startTime").asInt() : null,
                        endTime: absence.has("endTime") ? absence.get("endTime").asInt() : null,
                        excused: absence.has("excused") ? absence.get("excused").asBoolean() : false,
                        reason: absence.has("reason") ? absence.get("reason").asText() : "",
                        text: absence.has("text") ? absence.get("text").asText() : ""
                ]
            }
        }
        return absences
    }

    private Map parseUserData(JsonNode result) {
        def userData = [
                masterData: null,
                userData  : null,
                settings  : null
        ]

        // Master data
        if (result.has("masterData")) {
            userData.masterData = masterDataParser.parseMasterData(result.get("masterData"))
        }

        // User data
        if (result.has("userData")) {
            def user = result.get("userData")
            userData.userData = [
                    elemType    : user.has("elemType") ? user.get("elemType").asText() : null,
                    elemId      : user.get("elemId").asInt(),
                    displayName : user.has("displayName") ? user.get("displayName").asText() : "",
                    schoolName  : user.has("schoolName") ? user.get("schoolName").asText() : "",
                    departmentId: user.has("departmentId") ? user.get("departmentId").asInt() : 0,
                    children    : user.has("children") ? parseChildren(user.get("children")) : [],
                    klassenIds  : user.has("klassenIds") ? user.get("klassenIds").collect { it.asInt() } : [],
                    rights      : user.has("rights") ? parseUserRights(user.get("rights")) : []
            ]
        }

        return userData
    }

    private List<Map> parseChildren(JsonNode children) {
        def result = []
        children.each { child ->
            result << [
                    id         : child.get("id").asInt(),
                    firstName  : child.has("firstName") ? child.get("firstName").asText() : "",
                    lastName   : child.has("lastName") ? child.get("lastName").asText() : "",
                    displayName: child.has("displayName") ? child.get("displayName").asText() : ""
            ]
        }
        return result
    }

    private List<String> parseUserRights(JsonNode rights) {
        def result = []
        rights.each { right ->
            result << right.asText()
        }
        return result
    }
}
