package de.c7h12.webuntis.client

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Component
import org.springframework.web.client.RestTemplate

import java.net.URLEncoder
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Component
class WebUntisClient {

    private final RestTemplate restTemplate = new RestTemplate()
    private final ObjectMapper objectMapper = new ObjectMapper()

    WebUntisSession authenticate(String school, String username, String password, String server) {
        // Server URL normalisieren - https:// entfernen falls vorhanden
        def normalizedServer = server.startsWith("https://") ? server.substring(8) : server
        normalizedServer = normalizedServer.startsWith("http://") ? normalizedServer.substring(7) : normalizedServer

        // Authentifizierung muss bereits mit school parameter erfolgen
        def encodedSchool = URLEncoder.encode(school, "UTF-8")
        def url = "https://${normalizedServer}/WebUntis/jsonrpc.do?school=${encodedSchool}"

        def params = [
                user: username,
                password: password,
                client: "SpringBootGroovyApp"
        ]

        // Manche WebUntis Installationen brauchen den school parameter im body
        if (!url.contains("?school=")) {
            params.school = school
        }

        def request = createJsonRpcRequest("authenticate", params)

        def headers = new HttpHeaders().tap {
            contentType = MediaType.APPLICATION_JSON
            set("User-Agent", "SpringBoot-Groovy-WebUntis-Client")
        }

        def entity = new HttpEntity<>(request, headers)

        try {
            // Debug logging
            println "DEBUG: Authenticating with URL: ${url}"
            println "DEBUG: Request payload: ${objectMapper.writeValueAsString(request)}"

            ResponseEntity<String> response = restTemplate.postForEntity(url, entity, String)
            JsonNode jsonResponse = objectMapper.readTree(response.body)

            if (jsonResponse.has("error")) {
                throw new WebUntisException("Authentication failed: ${jsonResponse.get("error").get("message").asText()}")
            }

            def result = jsonResponse.get("result")
            def sessionId = result.get("sessionId").asText()
            def personId = result.get("personId").asInt()
            def cookies = response.headers.getFirst("Set-Cookie")

            return new WebUntisSession(sessionId, personId, cookies, school, normalizedServer)

        } catch (Exception e) {
            throw new WebUntisException("Authentication error: ${e.message}")
        }
    }

    List<Map> getTimetable(WebUntisSession session, LocalDate startDate, LocalDate endDate, int elementId, int elementType) {
        def url = "https://${session.server}/WebUntis/jsonrpc.do?school=${session.school}"

        // Erweiterte Parameter wie im Java-Code
        def element = [
                type: elementType,
                id: elementId
        ]

        def options = [
                startDate: startDate.format(DateTimeFormatter.ofPattern("yyyyMMdd")),
                endDate: endDate.format(DateTimeFormatter.ofPattern("yyyyMMdd")),
                element: element,
                onlyBaseTimetable: false,
                showInfo: true,
                showSubstText: true,
                showLsText: true,
                showLsNumber: true,
                showStudentgroup: true
        ]

        def request = createJsonRpcRequest("getTimetable", [options: options])
        def headers = createAuthenticatedHeaders(session)
        def entity = new HttpEntity<>(request, headers)

        try {
            ResponseEntity<String> response = restTemplate.postForEntity(url, entity, String)
            JsonNode jsonResponse = objectMapper.readTree(response.body)

            if (jsonResponse.has("error")) {
                throw new WebUntisException("Timetable request failed: ${jsonResponse.get("error").get("message").asText()}")
            }

            return parseAdvancedTimetableEntries(jsonResponse.get("result"))

        } catch (Exception e) {
            throw new WebUntisException("Error getting timetable: ${e.message}")
        }
    }

    List<Map> getSubjects(WebUntisSession session) {
        def url = "https://${session.server}/WebUntis/jsonrpc.do?school=${session.school}"
        def request = createJsonRpcRequest("getSubjects", [:])
        def headers = createAuthenticatedHeaders(session)
        def entity = new HttpEntity<>(request, headers)

        try {
            ResponseEntity<String> response = restTemplate.postForEntity(url, entity, String)
            JsonNode jsonResponse = objectMapper.readTree(response.body)

            if (jsonResponse.has("error")) {
                throw new WebUntisException("Subjects request failed: ${jsonResponse.get("error").get("message").asText()}")
            }

            return parseSubjects(jsonResponse.get("result"))

        } catch (Exception e) {
            throw new WebUntisException("Error getting subjects: ${e.message}")
        }
    }

    List<Map> getTeachers(WebUntisSession session) {
        def url = "https://${session.server}/WebUntis/jsonrpc.do?school=${session.school}"
        def request = createJsonRpcRequest("getTeachers", [:])
        def headers = createAuthenticatedHeaders(session)
        def entity = new HttpEntity<>(request, headers)

        try {
            ResponseEntity<String> response = restTemplate.postForEntity(url, entity, String)
            JsonNode jsonResponse = objectMapper.readTree(response.body)

            if (jsonResponse.has("error")) {
                throw new WebUntisException("Teachers request failed: ${jsonResponse.get("error").get("message").asText()}")
            }

            return parseTeachers(jsonResponse.get("result"))

        } catch (Exception e) {
            throw new WebUntisException("Error getting teachers: ${e.message}")
        }
    }

    List<Map> getRooms(WebUntisSession session) {
        def url = "https://${session.server}/WebUntis/jsonrpc.do?school=${session.school}"
        def request = createJsonRpcRequest("getRooms", [:])
        def headers = createAuthenticatedHeaders(session)
        def entity = new HttpEntity<>(request, headers)

        try {
            ResponseEntity<String> response = restTemplate.postForEntity(url, entity, String)
            JsonNode jsonResponse = objectMapper.readTree(response.body)

            if (jsonResponse.has("error")) {
                throw new WebUntisException("Rooms request failed: ${jsonResponse.get("error").get("message").asText()}")
            }

            return parseRooms(jsonResponse.get("result"))

        } catch (Exception e) {
            throw new WebUntisException("Error getting rooms: ${e.message}")
        }
    }

    List<Map> getClasses(WebUntisSession session) {
        def url = "https://${session.server}/WebUntis/jsonrpc.do?school=${session.school}"
        def request = createJsonRpcRequest("getKlassen", [:])
        def headers = createAuthenticatedHeaders(session)
        def entity = new HttpEntity<>(request, headers)

        try {
            ResponseEntity<String> response = restTemplate.postForEntity(url, entity, String)
            JsonNode jsonResponse = objectMapper.readTree(response.body)

            if (jsonResponse.has("error")) {
                throw new WebUntisException("Classes request failed: ${jsonResponse.get("error").get("message").asText()}")
            }

            return parseClasses(jsonResponse.get("result"))

        } catch (Exception e) {
            throw new WebUntisException("Error getting classes: ${e.message}")
        }
    }

    // ========== 2017 API Methods (Optional Enhanced Features) ==========

    List<Map> getTimetable2017(WebUntisSession session, LocalDate startDate, LocalDate endDate, int elementId, String elementType) {
        def url = "https://${session.server}/WebUntis/jsonrpc.do?school=${session.school}"

        def options = [
                id: elementId,
                type: elementType,
                startDate: startDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")),
                endDate: endDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
        ]

        def request = createJsonRpcRequest("getTimetable2017", options)
        def headers = createAuthenticatedHeaders(session)
        def entity = new HttpEntity<>(request, headers)

        try {
            ResponseEntity<String> response = restTemplate.postForEntity(url, entity, String)
            JsonNode jsonResponse = objectMapper.readTree(response.body)

            if (jsonResponse.has("error")) {
                throw new WebUntisException("Enhanced Timetable request failed: ${jsonResponse.get("error").get("message").asText()}")
            }

            return parseEnhancedTimetableEntries(jsonResponse.get("result"))

        } catch (Exception e) {
            throw new WebUntisException("Error getting enhanced timetable: ${e.message}")
        }
    }

    List<Map> getHomework2017(WebUntisSession session, LocalDate startDate, LocalDate endDate, int studentId) {
        def url = "https://${session.server}/WebUntis/jsonrpc.do?school=${session.school}"

        def options = [
                id: studentId,
                type: "STUDENT",
                startDate: startDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")),
                endDate: endDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
        ]

        def request = createJsonRpcRequest("getHomeWork2017", options)
        def headers = createAuthenticatedHeaders(session)
        def entity = new HttpEntity<>(request, headers)

        try {
            ResponseEntity<String> response = restTemplate.postForEntity(url, entity, String)
            JsonNode jsonResponse = objectMapper.readTree(response.body)

            if (jsonResponse.has("error")) {
                throw new WebUntisException("Homework request failed: ${jsonResponse.get("error").get("message").asText()}")
            }

            return parseHomework(jsonResponse.get("result"))

        } catch (Exception e) {
            throw new WebUntisException("Error getting homework: ${e.message}")
        }
    }

    List<Map> getMessagesOfDay2017(WebUntisSession session, LocalDate date) {
        def url = "https://${session.server}/WebUntis/jsonrpc.do?school=${session.school}"

        def options = [
                date: date.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
        ]

        def request = createJsonRpcRequest("getMessagesOfDay2017", options)
        def headers = createAuthenticatedHeaders(session)
        def entity = new HttpEntity<>(request, headers)

        try {
            ResponseEntity<String> response = restTemplate.postForEntity(url, entity, String)
            JsonNode jsonResponse = objectMapper.readTree(response.body)

            if (jsonResponse.has("error")) {
                throw new WebUntisException("Messages request failed: ${jsonResponse.get("error").get("message").asText()}")
            }

            return parseMessages(jsonResponse.get("result"))

        } catch (Exception e) {
            throw new WebUntisException("Error getting messages: ${e.message}")
        }
    }

    List<Map> getStudentAbsences2017(WebUntisSession session, LocalDate startDate, LocalDate endDate, boolean includeExcused = true, boolean includeUnexcused = true) {
        def url = "https://${session.server}/WebUntis/jsonrpc.do?school=${session.school}"

        def options = [
                startDate: startDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")),
                endDate: endDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")),
                includeExcused: includeExcused,
                includeUnExcused: includeUnexcused
        ]

        def request = createJsonRpcRequest("getStudentAbsences2017", options)
        def headers = createAuthenticatedHeaders(session)
        def entity = new HttpEntity<>(request, headers)

        try {
            ResponseEntity<String> response = restTemplate.postForEntity(url, entity, String)
            JsonNode jsonResponse = objectMapper.readTree(response.body)

            if (jsonResponse.has("error")) {
                throw new WebUntisException("Absences request failed: ${jsonResponse.get("error").get("message").asText()}")
            }

            return parseAbsences(jsonResponse.get("result"))

        } catch (Exception e) {
            throw new WebUntisException("Error getting absences: ${e.message}")
        }
    }

    Map getUserData2017(WebUntisSession session) {
        def url = "https://${session.server}/WebUntis/jsonrpc.do?school=${session.school}"

        def request = createJsonRpcRequest("getUserData2017", [:])
        def headers = createAuthenticatedHeaders(session)
        def entity = new HttpEntity<>(request, headers)

        try {
            ResponseEntity<String> response = restTemplate.postForEntity(url, entity, String)
            JsonNode jsonResponse = objectMapper.readTree(response.body)

            if (jsonResponse.has("error")) {
                throw new WebUntisException("UserData request failed: ${jsonResponse.get("error").get("message").asText()}")
            }

            return parseUserData(jsonResponse.get("result"))

        } catch (Exception e) {
            throw new WebUntisException("Error getting user data: ${e.message}")
        }
    }

    void logout(WebUntisSession session) {
        try {
            def url = "https://${session.server}/WebUntis/jsonrpc.do?school=${session.school}"
            def request = createJsonRpcRequest("logout", [:])
            def headers = createAuthenticatedHeaders(session)
            def entity = new HttpEntity<>(request, headers)

            restTemplate.postForEntity(url, entity, String)

        } catch (Exception e) {
            println "Logout warning: ${e.message}"
        }
    }

    private Map createJsonRpcRequest(String method, Map params) {
        return [
                id: UUID.randomUUID().toString(),
                method: method,
                params: params,
                jsonrpc: "2.0"
        ]
    }

    private HttpHeaders createAuthenticatedHeaders(WebUntisSession session) {
        def headers = new HttpHeaders().tap {
            contentType = MediaType.APPLICATION_JSON
            set("User-Agent", "SpringBoot-Groovy-WebUntis-Client")
            if (session.cookies) {
                set("Cookie", session.cookies)
            }
        }
        return headers
    }

    private List<Map> parseTimetableEntries(JsonNode result) {
        def entries = []

        result.each { entry ->
            def timetableEntry = [
                    id: entry.get("id").asLong(),
                    date: entry.get("date").asInt(),
                    startTime: entry.get("startTime").asInt(),
                    endTime: entry.get("endTime").asInt(),
                    subjects: [],
                    teachers: [],
                    rooms: [],
                    classes: []
            ]

            // Subjects
            if (entry.has("su")) {
                entry.get("su").each { su ->
                    timetableEntry.subjects << [
                            id: su.get("id").asLong(),
                            name: su.has("name") ? su.get("name").asText() : "",
                            longName: su.has("longname") ? su.get("longname").asText() : ""
                    ]
                }
            }

            // Teachers
            if (entry.has("te")) {
                entry.get("te").each { te ->
                    timetableEntry.teachers << [
                            id: te.get("id").asLong(),
                            name: te.has("name") ? te.get("name").asText() : "",
                            foreName: te.has("forename") ? te.get("forename").asText() : "",
                            longName: te.has("longname") ? te.get("longname").asText() : ""
                    ]
                }
            }

            // Rooms
            if (entry.has("ro")) {
                entry.get("ro").each { ro ->
                    timetableEntry.rooms << [
                            id: ro.get("id").asLong(),
                            name: ro.has("name") ? ro.get("name").asText() : "",
                            longName: ro.has("longname") ? ro.get("longname").asText() : ""
                    ]
                }
            }

            // Classes
            if (entry.has("kl")) {
                entry.get("kl").each { kl ->
                    timetableEntry.classes << [
                            id: kl.get("id").asLong(),
                            name: kl.has("name") ? kl.get("name").asText() : "",
                            longName: kl.has("longname") ? kl.get("longname").asText() : ""
                    ]
                }
            }

            entries << timetableEntry
        }

        return entries
    }

    private List<Map> parseSubjects(JsonNode result) {
        def subjects = []

        result.each { subjectNode ->
            subjects << [
                    id: subjectNode.get("id").asLong(),
                    name: subjectNode.get("name").asText(),
                    longName: subjectNode.has("longName") ? subjectNode.get("longName").asText() : ""
            ]
        }

        return subjects
    }

    private List<Map> parseTeachers(JsonNode result) {
        def teachers = []

        result.each { teacherNode ->
            teachers << [
                    id: teacherNode.get("id").asLong(),
                    name: teacherNode.get("name").asText(),
                    foreName: teacherNode.has("foreName") ? teacherNode.get("foreName").asText() : "",
                    longName: teacherNode.has("longName") ? teacherNode.get("longName").asText() : ""
            ]
        }

        return teachers
    }

    private List<Map> parseRooms(JsonNode result) {
        def rooms = []

        result.each { roomNode ->
            rooms << [
                    id: roomNode.get("id").asLong(),
                    name: roomNode.get("name").asText(),
                    longName: roomNode.has("longName") ? roomNode.get("longName").asText() : ""
            ]
        }

        return rooms
    }

    private List<Map> parseClasses(JsonNode result) {
        def classes = []

        result.each { classNode ->
            classes << [
                    id: classNode.get("id").asLong(),
                    name: classNode.get("name").asText(),
                    longName: classNode.has("longName") ? classNode.get("longName").asText() : classNode.get("name").asText()
            ]
        }

        return classes
    }

    // ========== Enhanced Parser Methods for 2017 API ==========

    private List<Map> parseAdvancedTimetableEntries(JsonNode result) {
        def entries = []

        result.each { entry ->
            def timetableEntry = [
                    id: entry.get("id").asLong(),
                    date: entry.get("date").asInt(),
                    startTime: entry.get("startTime").asInt(),
                    endTime: entry.get("endTime").asInt(),

                    // Basis-Arrays für IDs
                    subjects: [],
                    teachers: [],
                    rooms: [],
                    classes: [],

                    // Original-Arrays (für Vertretungen)
                    originalSubjects: [],
                    originalTeachers: [],
                    originalRooms: [],
                    originalClasses: [],

                    // Erweiterte Informationen
                    code: entry.has("code") ? entry.get("code").asText().toUpperCase() : "REGULAR",
                    activityType: entry.has("activityType") ? entry.get("activityType").asText() : null,
                    info: entry.has("info") ? entry.get("info").asText() : null,
                    substText: entry.has("substText") ? entry.get("substText").asText() : null,
                    lsText: entry.has("lstext") ? entry.get("lstext").asText() : null,
                    lsNumber: entry.has("lsnumber") ? entry.get("lsnumber").asInt() : null,
                    studentGroup: entry.has("sg") ? entry.get("sg").asText() : null,
                    statflags: entry.has("statflags") ? entry.get("statflags").asText() : null,
                    bkRemark: entry.has("bkRemark") ? entry.get("bkRemark").asText() : null,
                    bkText: entry.has("bkText") ? entry.get("bkText").asText() : null
            ]

            // Subjects (aktuelle und ursprüngliche)
            if (entry.has("su")) {
                entry.get("su").each { su ->
                    def subjectEntry = [
                            id: su.get("id").asLong(),
                            name: su.has("name") ? su.get("name").asText() : "",
                            longName: su.has("longname") ? su.get("longname").asText() : ""
                    ]
                    timetableEntry.subjects << subjectEntry

                    // Original-Fach bei Vertretungen
                    if (su.has("orgid")) {
                        timetableEntry.originalSubjects << [
                                id: su.get("orgid").asLong(),
                                name: "", // Wird später aufgelöst
                                longName: ""
                        ]
                    }
                }
            }

            // Teachers (aktuelle und ursprüngliche)
            if (entry.has("te")) {
                entry.get("te").each { te ->
                    def teacherEntry = [
                            id: te.get("id").asLong(),
                            name: te.has("name") ? te.get("name").asText() : "",
                            foreName: te.has("forename") ? te.get("forename").asText() : "",
                            longName: te.has("longname") ? te.get("longname").asText() : ""
                    ]
                    timetableEntry.teachers << teacherEntry

                    // Original-Lehrer bei Vertretungen
                    if (te.has("orgid")) {
                        timetableEntry.originalTeachers << [
                                id: te.get("orgid").asLong(),
                                name: "",
                                foreName: "",
                                longName: ""
                        ]
                    }
                }
            }

            // Rooms (aktuelle und ursprüngliche)
            if (entry.has("ro")) {
                entry.get("ro").each { ro ->
                    def roomEntry = [
                            id: ro.get("id").asLong(),
                            name: ro.has("name") ? ro.get("name").asText() : "",
                            longName: ro.has("longname") ? ro.get("longname").asText() : ""
                    ]
                    timetableEntry.rooms << roomEntry

                    // Original-Raum bei Vertretungen
                    if (ro.has("orgid")) {
                        timetableEntry.originalRooms << [
                                id: ro.get("orgid").asLong(),
                                name: "",
                                longName: ""
                        ]
                    }
                }
            }

            // Classes (aktuelle und ursprüngliche)
            if (entry.has("kl")) {
                entry.get("kl").each { kl ->
                    def classEntry = [
                            id: kl.get("id").asLong(),
                            name: kl.has("name") ? kl.get("name").asText() : "",
                            longName: kl.has("longname") ? kl.get("longname").asText() : ""
                    ]
                    timetableEntry.classes << classEntry

                    // Original-Klasse bei Vertretungen
                    if (kl.has("orgid")) {
                        timetableEntry.originalClasses << [
                                id: kl.get("orgid").asLong(),
                                name: "",
                                longName: ""
                        ]
                    }
                }
            }

            entries << timetableEntry
        }

        return entries
    }

    private List<Map> parseEnhancedTimetableEntries(JsonNode result) {
        def entries = []

        result.each { entry ->
            def timetableEntry = [
                    id: entry.get("id").asLong(),
                    date: entry.get("date").asInt(),
                    startTime: entry.get("startTime").asInt(),
                    endTime: entry.get("endTime").asInt(),
                    subjects: [],
                    teachers: [],
                    rooms: [],
                    classes: [],
                    // Enhanced 2017 fields
                    activityType: entry.has("activityType") ? entry.get("activityType").asText() : null,
                    code: entry.has("code") ? entry.get("code").asText() : null,
                    info: entry.has("info") ? entry.get("info").asText() : null,
                    substText: entry.has("substText") ? entry.get("substText").asText() : null,
                    lstext: entry.has("lstext") ? entry.get("lstext").asText() : null,
                    lsnumber: entry.has("lsnumber") ? entry.get("lsnumber").asInt() : null,
                    statflags: entry.has("statflags") ? entry.get("statflags").asText() : null,
                    sg: entry.has("sg") ? entry.get("sg").asText() : null,
                    bkRemark: entry.has("bkRemark") ? entry.get("bkRemark").asText() : null,
                    bkText: entry.has("bkText") ? entry.get("bkText").asText() : null
            ]

            // Parse subjects, teachers, rooms, classes (same as standard method)
            if (entry.has("su")) {
                entry.get("su").each { su ->
                    timetableEntry.subjects << [
                            id: su.get("id").asLong(),
                            name: su.has("name") ? su.get("name").asText() : "",
                            longName: su.has("longname") ? su.get("longname").asText() : ""
                    ]
                }
            }

            if (entry.has("te")) {
                entry.get("te").each { te ->
                    timetableEntry.teachers << [
                            id: te.get("id").asLong(),
                            name: te.has("name") ? te.get("name").asText() : "",
                            foreName: te.has("forename") ? te.get("forename").asText() : "",
                            longName: te.has("longname") ? te.get("longname").asText() : ""
                    ]
                }
            }

            if (entry.has("ro")) {
                entry.get("ro").each { ro ->
                    timetableEntry.rooms << [
                            id: ro.get("id").asLong(),
                            name: ro.has("name") ? ro.get("name").asText() : "",
                            longName: ro.has("longname") ? ro.get("longname").asText() : ""
                    ]
                }
            }

            if (entry.has("kl")) {
                entry.get("kl").each { kl ->
                    timetableEntry.classes << [
                            id: kl.get("id").asLong(),
                            name: kl.has("name") ? kl.get("name").asText() : "",
                            longName: kl.has("longname") ? kl.get("longname").asText() : ""
                    ]
                }
            }

            entries << timetableEntry
        }

        return entries
    }

    private List<Map> parseHomework(JsonNode result) {
        def homework = []

        result.each { hw ->
            homework << [
                    id: hw.get("id").asLong(),
                    lessonId: hw.has("lessonId") ? hw.get("lessonId").asLong() : null,
                    date: hw.has("date") ? hw.get("date").asInt() : null,
                    dueDate: hw.has("dueDate") ? hw.get("dueDate").asInt() : null,
                    text: hw.has("text") ? hw.get("text").asText() : "",
                    remark: hw.has("remark") ? hw.get("remark").asText() : "",
                    completed: hw.has("completed") ? hw.get("completed").asBoolean() : false,
                    attachments: hw.has("attachments") ? parseAttachments(hw.get("attachments")) : []
            ]
        }

        return homework
    }

    private List<Map> parseAttachments(JsonNode attachments) {
        def attachmentList = []

        attachments.each { attachment ->
            attachmentList << [
                    id: attachment.get("id").asLong(),
                    name: attachment.has("name") ? attachment.get("name").asText() : "",
                    url: attachment.has("url") ? attachment.get("url").asText() : ""
            ]
        }

        return attachmentList
    }

    private List<Map> parseMessages(JsonNode result) {
        def messages = []

        result.each { msg ->
            messages << [
                    id: msg.get("id").asLong(),
                    subject: msg.has("subject") ? msg.get("subject").asText() : "",
                    text: msg.has("text") ? msg.get("text").asText() : "",
                    isStudentMessage: msg.has("isStudentMessage") ? msg.get("isStudentMessage").asBoolean() : false,
                    attachments: msg.has("attachments") ? parseAttachments(msg.get("attachments")) : []
            ]
        }

        return messages
    }

    private List<Map> parseAbsences(JsonNode result) {
        def absences = []

        result.each { absence ->
            absences << [
                    id: absence.get("id").asLong(),
                    startDate: absence.has("startDate") ? absence.get("startDate").asInt() : null,
                    endDate: absence.has("endDate") ? absence.get("endDate").asInt() : null,
                    startTime: absence.has("startTime") ? absence.get("startTime").asInt() : null,
                    endTime: absence.has("endTime") ? absence.get("endTime").asInt() : null,
                    createDate: absence.has("createDate") ? absence.get("createDate").asLong() : null,
                    lastUpdate: absence.has("lastUpdate") ? absence.get("lastUpdate").asLong() : null,
                    reasonId: absence.has("reasonId") ? absence.get("reasonId").asInt() : null,
                    text: absence.has("text") ? absence.get("text").asText() : "",
                    excuse: absence.has("excuse") ? parseExcuse(absence.get("excuse")) : null,
                    studentId: absence.has("studentId") ? absence.get("studentId").asLong() : null,
                    excused: absence.has("excused") ? absence.get("excused").asBoolean() : false,
                    interrupted: absence.has("interrupted") ? absence.get("interrupted").asBoolean() : false
            ]
        }

        return absences
    }

    private Map parseExcuse(JsonNode excuse) {
        return [
                id: excuse.get("id").asLong(),
                text: excuse.has("text") ? excuse.get("text").asText() : "",
                excuseDate: excuse.has("excuseDate") ? excuse.get("excuseDate").asLong() : null,
                excuseStatus: excuse.has("excuseStatus") ? excuse.get("excuseStatus").asText() : ""
        ]
    }

    private Map parseUserData(JsonNode result) {
        return [
                masterData: result.has("masterData") ? parseMasterData(result.get("masterData")) : [:],
                userData: result.has("userData") ? parseUserDataDetails(result.get("userData")) : [:],
                settings: result.has("settings") ? parseSettings(result.get("settings")) : [:],
                messengerSettings: result.has("messengerSettings") ? parseMessengerSettings(result.get("messengerSettings")) : [:]
        ]
    }

    private Map parseMasterData(JsonNode masterData) {
        def data = [:]

        if (masterData.has("timeUnits")) {
            data.timeUnits = []
            masterData.get("timeUnits").each { tu ->
                data.timeUnits << [
                        name: tu.has("name") ? tu.get("name").asText() : "",
                        startTime: tu.has("startTime") ? tu.get("startTime").asInt() : null,
                        endTime: tu.has("endTime") ? tu.get("endTime").asInt() : null
                ]
            }
        }

        return data
    }

    private Map parseUserDataDetails(JsonNode userData) {
        return [
                elemId: userData.has("elemId") ? userData.get("elemId").asLong() : null,
                elemType: userData.has("elemType") ? userData.get("elemType").asInt() : null,
                displayName: userData.has("displayName") ? userData.get("displayName").asText() : "",
                schoolName: userData.has("schoolName") ? userData.get("schoolName").asText() : ""
        ]
    }

    private Map parseSettings(JsonNode settings) {
        return [
                showAbsenceReason: settings.has("showAbsenceReason") ? settings.get("showAbsenceReason").asBoolean() : false,
                showAbsenceText: settings.has("showAbsenceText") ? settings.get("showAbsenceText").asBoolean() : false
        ]
    }

    private Map parseMessengerSettings(JsonNode messengerSettings) {
        return [
                messengerEnabled: messengerSettings.has("messengerEnabled") ? messengerSettings.get("messengerEnabled").asBoolean() : false,
                serverUrl: messengerSettings.has("serverUrl") ? messengerSettings.get("serverUrl").asText() : ""
        ]
    }
}
