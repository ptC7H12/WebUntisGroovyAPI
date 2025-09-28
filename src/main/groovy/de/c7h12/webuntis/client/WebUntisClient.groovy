package de.c7h12.webuntis.client

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Component
import org.springframework.web.client.RestTemplate

import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import java.net.URLEncoder
import java.nio.ByteBuffer
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import org.apache.commons.codec.binary.Base32
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import java.time.Clock

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
                user    : username,
                password: password,
                client  : "SpringBootGroovyApp"
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

            // Cookies aus der Response extrahieren
            def cookies = response.getHeaders().getFirst("Set-Cookie")

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

    // ========== Enhanced 2017 API Methods ==========

    def base32Decode(String base32) {
        int i = 0
        int index = 0
        int offset = 0
        int digit
        byte[] bytes = new byte[(base32.length() * 5) / 8]

        int[] base32Lookup = [
                0xFF, 0xFF, 0x1A, 0x1B, 0x1C, 0x1D, 0x1E, 0x1F,
                0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF,
                0xFF, 0x00, 0x01, 0x02, 0x03, 0x04, 0x05, 0x06,
                0x07, 0x08, 0x09, 0x0A, 0x0B, 0x0C, 0x0D, 0x0E,
                0x0F, 0x10, 0x11, 0x12, 0x13, 0x14, 0x15, 0x16,
                0x17, 0x18, 0x19, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF,
                0xFF, 0x00, 0x01, 0x02, 0x03, 0x04, 0x05, 0x06,
                0x07, 0x08, 0x09, 0x0A, 0x0B, 0x0C, 0x0D, 0x0E,
                0x0F, 0x10, 0x11, 0x12, 0x13, 0x14, 0x15, 0x16,
                0x17, 0x18, 0x19, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF
        ] as int[]

        while (i < base32.length()) {
            // FIX: in Groovy muss '0' explizit als charCode behandelt werden
            int lookup = (int)base32.charAt(i) - (int)'0'

            if (lookup < 0 || lookup >= base32Lookup.length) {
                i++
                continue
            }

            digit = base32Lookup[lookup]
            if (digit == 0xFF) {
                i++
                continue
            }

            if (index <= 3) {
                index = (index + 5) % 8
                if (index == 0) {
                    bytes[offset] = (byte)(bytes[offset] | digit)
                    offset++
                    if (offset >= bytes.length) break
                } else {
                    bytes[offset] = (byte)(bytes[offset] | (digit << (8 - index)))
                }
            } else {
                index = (index + 5) % 8
                bytes[offset] = (byte)(bytes[offset] | (digit >>> index))
                offset++
                if (offset >= bytes.length) break
                bytes[offset] = (byte)(bytes[offset] | (digit << (8 - index)))
            }
            i++
        }
        return bytes
    }

// --- OTP Generator ---
    def createOtp(String secret, Clock clock = Clock.systemDefaultZone()) {
        if (!secret) return "000000"
        try {
            println "DEBUG: secret (raw)   = ${secret}"
            byte[] key = base32Decode(secret.toUpperCase(Locale.ROOT))
            println "DEBUG: decoded key    = " + key.collect { String.format("%02X", it) }.join(" ")

            long timeWindow = clock.millis() / 30_000L
            println "DEBUG: timeWindow     = ${timeWindow}"

            // HMAC-SHA1 vorbereiten
            byte[] data = new byte[8]
            long value = timeWindow
            for (int i = 7; i >= 0; i--) {
                data[i] = (byte)(value & 0xFF)
                value >>>= 8
            }
            println "DEBUG: message (hex)  = " + data.collect { String.format("%02X", it) }.join(" ")

            Mac mac = Mac.getInstance("HmacSHA1")
            mac.init(new SecretKeySpec(key, "HmacSHA1"))
            byte[] hash = mac.doFinal(data)
            println "DEBUG: hmac (hex)     = " + hash.collect { String.format("%02X", it) }.join(" ")

            int offset = hash[hash.length - 1] & 0xF
            long binary =
                    ((hash[offset] & 0x7F) << 24) |
                            ((hash[offset + 1] & 0xFF) << 16) |
                            ((hash[offset + 2] & 0xFF) << 8) |
                            (hash[offset + 3] & 0xFF)

            int code = (int)(binary % 1_000_000L)
            def otp = String.format("%06d", code)
            println "DEBUG: final OTP      = ${otp}"
            return otp
        } catch (Exception e) {
            e.printStackTrace()
            return "000000"
        }
    }

    // WebUntis Secret-basierte Authentifizierung für 2017 API mit Master-Daten
    WebUntisSession authenticateWithSecret(String school, String username, String appSecret, String server) {
        // Server URL normalisieren
        def normalizedServer = server.startsWith("https://") ? server.substring(8) : server
        normalizedServer = normalizedServer.startsWith("http://") ? normalizedServer.substring(7) : normalizedServer

        def encodedSchool = URLEncoder.encode(school, "UTF-8")
        def url = "https://${normalizedServer}/WebUntis/jsonrpc_intern.do?school=${encodedSchool}"

        // Zeitbasierter OTP-Code generieren
        def currentTime = System.currentTimeMillis()
        def otp = createOtp(appSecret)

        def authParams = [
                user      : username,
                otp       : otp,
                clientTime: currentTime.toString()
        ]

        def params = [[
                              masterDataTimestamp: currentTime.toString(),
                              type               : "STUDENT",
                              auth               : authParams
                      ]]

        def request = createJsonRpcRequest("getUserData2017", params)
        def headers = createStandardHeaders()
        def entity = new HttpEntity<>(request, headers)

        try {
            println "DEBUG: Enhanced Auth with URL: ${url}"
            println "DEBUG: OTP: ${otp}, ClientTime: ${currentTime}"
            println "DEBUG: Request: ${objectMapper.writeValueAsString(request)}"

            ResponseEntity<String> response = restTemplate.postForEntity(url, entity, String)
            JsonNode jsonResponse = objectMapper.readTree(response.body)

            if (jsonResponse.has("error")) {
                throw new WebUntisException("Enhanced authentication failed: ${jsonResponse.get("error").get("message").asText()}")
            }

            // Session aus getUserData2017 Response extrahieren
            def result = jsonResponse.get("result")
            def userData = result.get("userData")
            def masterData = result.get("masterData")

            def sessionId = "enhanced-session-${System.currentTimeMillis()}"
            def personId = userData.get("elemId").asInt()

            def cookies = response.getHeaders().getFirst("Set-Cookie")
            def session = new WebUntisSession(sessionId, personId, cookies, school, normalizedServer, appSecret, username)

            // Master-Daten in Session speichern für späteres Mapping
            session.masterData = parseMasterDataFrom2017Response(masterData)

            return session

        } catch (Exception e) {
            throw new WebUntisException("Enhanced authentication error: ${e.message}")
        }
    }

    // Master-Daten aus der 2017 getUserData Response extrahieren
    private Map parseMasterDataFrom2017Response(JsonNode masterData) {
        def parsedData = [
                subjects   : [:],
                teachers   : [:],
                rooms      : [:],
                klassen    : [:],
                timeGrid   : [:],
                schoolyears: [],
                holidays   : []
        ]

        // Subjects extrahieren
        if (masterData.has("subjects")) {
            masterData.get("subjects").each { su ->
                parsedData.subjects[su.get("id").asLong()] = [
                        id           : su.get("id").asLong(),
                        name         : su.get("name").asText(),
                        longName     : su.get("longName").asText(),
                        departmentIds: su.has("departmentIds") ? su.get("departmentIds").collect { it.asInt() } : [],
                        foreColor    : su.has("foreColor") ? su.get("foreColor").asText() : null,
                        backColor    : su.has("backColor") ? su.get("backColor").asText() : null,
                        active       : su.has("active") ? su.get("active").asBoolean() : true
                ]
            }
        }

        // Teachers extrahieren
        if (masterData.has("teachers")) {
            masterData.get("teachers").each { te ->
                parsedData.teachers[te.get("id").asLong()] = [
                        id           : te.get("id").asLong(),
                        name         : te.get("name").asText(),
                        firstName    : te.has("firstName") ? te.get("firstName").asText() : "",
                        lastName     : te.has("lastName") ? te.get("lastName").asText() : "",
                        departmentIds: te.has("departmentIds") ? te.get("departmentIds").collect { it.asInt() } : [],
                        active       : te.has("active") ? te.get("active").asBoolean() : true
                ]
            }
        }

        // Rooms extrahieren
        if (masterData.has("rooms")) {
            masterData.get("rooms").each { ro ->
                parsedData.rooms[ro.get("id").asLong()] = [
                        id          : ro.get("id").asLong(),
                        name        : ro.get("name").asText(),
                        longName    : ro.get("longName").asText(),
                        departmentId: ro.has("departmentId") ? ro.get("departmentId").asInt() : 0,
                        active      : ro.has("active") ? ro.get("active").asBoolean() : true
                ]
            }
        }

        // Klassen extrahieren
        if (masterData.has("klassen")) {
            masterData.get("klassen").each { kl ->
                parsedData.klassen[kl.get("id").asLong()] = [
                        id          : kl.get("id").asLong(),
                        name        : kl.get("name").asText(),
                        longName    : kl.get("longName").asText(),
                        departmentId: kl.has("departmentId") ? kl.get("departmentId").asInt() : 0,
                        startDate   : kl.has("startDate") ? kl.get("startDate").asText() : null,
                        endDate     : kl.has("endDate") ? kl.get("endDate").asText() : null,
                        active      : kl.has("active") ? kl.get("active").asBoolean() : true
                ]
            }
        }

        // TimeGrid extrahieren
        if (masterData.has("timeGrid")) {
            def timeGrid = masterData.get("timeGrid")
            if (timeGrid.has("days")) {
                def days = [:]
                timeGrid.get("days").each { day ->
                    def dayName = day.get("day").asText()
                    def units = []
                    if (day.has("units")) {
                        day.get("units").each { unit ->
                            units << [
                                    label    : unit.get("label").asText(),
                                    startTime: unit.get("startTime").asText(),
                                    endTime  : unit.get("endTime").asText()
                            ]
                        }
                    }
                    days[dayName] = units
                }
                parsedData.timeGrid = days
            }
        }

        return parsedData
    }

    List<Map> getTimetable2017Enhanced(WebUntisSession session, LocalDate startDate, LocalDate endDate, int elementId, String elementType) {
        // 2017 API verwendet jsonrpc_intern.do
        def url = "https://${session.server}/WebUntis/jsonrpc_intern.do?school=${session.school}"

        def authParams = [
                user      : session.username,
                otp       : createOtp(session.appSecret),
                clientTime: System.currentTimeMillis().toString()
        ]

        def params = [[
                              element                : [
                                      id     : elementId,
                                      type   : elementType,
                                      keyType: "id"
                              ],
                              startDate              : startDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")),
                              endDate                : endDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")),
                              klasseFields           : ["id", "name", "longname", "externalkey"],
                              roomFields             : ["id", "name", "longname", "externalkey"],
                              subjectFields          : ["id", "name", "longname", "externalkey"],
                              teacherFields          : ["id", "name", "longname", "externalkey"],
                              timetableFields        : ["id", "date", "startTime", "endTime", "kl", "te", "su", "ro", "lstype", "code", "info", "substText", "lstext", "lsnumber", "activityType", "statflags", "sg", "bkRemark", "bkText"],
                              timetableFieldsRequired: true,
                              showLsText             : true,
                              showStudentgroup       : true,
                              showLsNumber           : true,
                              showSubstText          : true,
                              showInfo               : true,
                              showBooking            : true,
                              showTopic              : true,
                              showHomework           : false,
                              masterDataTimestamp    : System.currentTimeMillis().toString(),
                              auth                   : authParams
                      ]]

        def request = createJsonRpcRequest("getTimetable2017", params)
        def headers = createStandardHeaders()
        def entity = new HttpEntity<>(request, headers)

        try {
            ResponseEntity<String> response = restTemplate.postForEntity(url, entity, String)
            JsonNode jsonResponse = objectMapper.readTree(response.body)

            if (jsonResponse.has("error")) {
                throw new WebUntisException("Enhanced Timetable 2017 request failed: ${jsonResponse.get("error").get("message").asText()}")
            }

            return parseEnhanced2017TimetableWithMasterData(jsonResponse.get("result"), session.masterData)

        } catch (Exception e) {
            throw new WebUntisException("Error getting enhanced timetable 2017: ${e.message}")
        }
    }

    List<Map> getTimetable2017(WebUntisSession session, LocalDate startDate, LocalDate endDate, int elementId, String elementType) {
        def url = "https://${session.server}/WebUntis/jsonrpc_intern.do?school=${session.school}"

        def authParams = [
                user      : session.username ?: "defaultUser",
                otp       : session.appSecret ? createOtp(session.appSecret) : "000000",
                clientTime: System.currentTimeMillis().toString()
        ]

        def params = [[
                              id                 : elementId,
                              type               : elementType,
                              startDate          : startDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")),
                              endDate            : endDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")),
                              masterDataTimestamp: System.currentTimeMillis().toString(),
                              auth               : authParams
                      ]]

        def request = createJsonRpcRequest("getTimetable2017", params)
        def headers = createStandardHeaders()
        def entity = new HttpEntity<>(request, headers)

        try {
            ResponseEntity<String> response = restTemplate.postForEntity(url, entity, String)
            JsonNode jsonResponse = objectMapper.readTree(response.body)

            if (jsonResponse.has("error")) {
                throw new WebUntisException("Enhanced Timetable request failed: ${jsonResponse.get("error").get("message").asText()}")
            }

            return parseEnhanced2017TimetableWithMasterData(jsonResponse.get("result"), session.masterData)

        } catch (Exception e) {
            throw new WebUntisException("Error getting enhanced timetable: ${e.message}")
        }
    }

    // Neue Parser-Methode die Master-Daten aus der Session nutzt
    private List<Map> parseEnhanced2017TimetableWithMasterData(JsonNode result, Map masterData) {
        def entries = []

        // Timetable-Daten extrahieren (kann direkt ein Array sein oder in einem "timetable" Feld)
        def timetableData = result.has("timetable") ? result.get("timetable") : result

        timetableData.each { entry ->
            def timetableEntry = [
                    id              : entry.get("id").asLong(),
                    date            : entry.get("date").asInt(),
                    startTime       : entry.get("startTime").asInt(),
                    endTime         : entry.get("endTime").asInt(),

                    // Basis-Arrays für aufgelöste Daten
                    subjects        : [],
                    teachers        : [],
                    rooms           : [],
                    classes         : [],

                    // Original-Arrays (für Vertretungen)
                    originalSubjects: [],
                    originalTeachers: [],
                    originalRooms   : [],
                    originalClasses : [],

                    // Erweiterte 2017 Informationen
                    code            : entry.has("code") ? entry.get("code").asText().toUpperCase() : "REGULAR",
                    activityType    : entry.has("activityType") ? entry.get("activityType").asText() : null,
                    info            : entry.has("info") ? entry.get("info").asText() : null,
                    substText       : entry.has("substText") ? entry.get("substText").asText() : null,
                    lsText          : entry.has("lstext") ? entry.get("lstext").asText() : null,
                    lsNumber        : entry.has("lsnumber") ? entry.get("lsnumber").asInt() : null,
                    studentGroup    : entry.has("sg") ? entry.get("sg").asText() : null,
                    statflags       : entry.has("statflags") ? entry.get("statflags").asText() : null,
                    bkRemark        : entry.has("bkRemark") ? entry.get("bkRemark").asText() : null,
                    bkText          : entry.has("bkText") ? entry.get("bkText").asText() : null,
                    lstype          : entry.has("lstype") ? entry.get("lstype").asText() : null,

                    // Weitere 2017 spezifische Felder
                    rescheduleInfo  : entry.has("rescheduleInfo") ? entry.get("rescheduleInfo").asText() : null,
                    periodInfo      : entry.has("periodInfo") ? entry.get("periodInfo").asText() : null,
                    is2017Format    : true
            ]

            // Subjects mit Master-Daten auflösen
            if (entry.has("su")) {
                entry.get("su").each { su ->
                    def subjectId = su.get("id").asLong()
                    def subject = masterData?.subjects?[subjectId] ?: [
                            id      : subjectId,
                            name    : su.has("name") ? su.get("name").asText() : "Fach-${subjectId}",
                            longName: su.has("longname") ? su.get("longname").asText() : "Fach-${subjectId}"
                    ]

                    timetableEntry.subjects << subject

                    // Original-Fach bei Vertretungen
                    if (su.has("orgid")) {
                        def orgSubjectId = su.get("orgid").asLong()
                        def orgSubject = masterData?.subjects?[orgSubjectId] ?: [
                                id      : orgSubjectId,
                                name    : "Fach-${orgSubjectId}",
                                longName: "Fach-${orgSubjectId}"
                        ]
                        timetableEntry.originalSubjects << orgSubject
                    }
                }
            }

            // Teachers mit Master-Daten auflösen
            if (entry.has("te")) {
                entry.get("te").each { te ->
                    def teacherId = te.get("id").asLong()
                    def teacher = masterData?.teachers?[teacherId] ?: [
                            id       : teacherId,
                            name     : te.has("name") ? te.get("name").asText() : "Lehrer-${teacherId}",
                            firstName: te.has("firstName") ? te.get("firstName").asText() : "",
                            lastName : te.has("lastName") ? te.get("lastName").asText() : ""
                    ]

                    timetableEntry.teachers << teacher

                    if (te.has("orgid")) {
                        def orgTeacherId = te.get("orgid").asLong()
                        def orgTeacher = masterData?.teachers?[orgTeacherId] ?: [
                                id       : orgTeacherId,
                                name     : "Lehrer-${orgTeacherId}",
                                firstName: "",
                                lastName : ""
                        ]
                        timetableEntry.originalTeachers << orgTeacher
                    }
                }
            }

            // Rooms mit Master-Daten auflösen
            if (entry.has("ro")) {
                entry.get("ro").each { ro ->
                    def roomId = ro.get("id").asLong()
                    def room = masterData?.rooms?[roomId] ?: [
                            id      : roomId,
                            name    : ro.has("name") ? ro.get("name").asText() : "Raum-${roomId}",
                            longName: ro.has("longname") ? ro.get("longname").asText() : "Raum-${roomId}"
                    ]

                    timetableEntry.rooms << room

                    if (ro.has("orgid")) {
                        def orgRoomId = ro.get("orgid").asLong()
                        def orgRoom = masterData?.rooms?[orgRoomId] ?: [
                                id      : orgRoomId,
                                name    : "Raum-${orgRoomId}",
                                longName: "Raum-${orgRoomId}"
                        ]
                        timetableEntry.originalRooms << orgRoom
                    }
                }
            }

            // Classes mit Master-Daten auflösen
            if (entry.has("kl")) {
                entry.get("kl").each { kl ->
                    def classId = kl.get("id").asLong()
                    def clazz = masterData?.klassen?[classId] ?: [
                            id      : classId,
                            name    : kl.has("name") ? kl.get("name").asText() : "Klasse-${classId}",
                            longName: kl.has("longname") ? kl.get("longname").asText() : "Klasse-${classId}"
                    ]

                    timetableEntry.classes << clazz

                    if (kl.has("orgid")) {
                        def orgClassId = kl.get("orgid").asLong()
                        def orgClass = masterData?.klassen?[orgClassId] ?: [
                                id      : orgClassId,
                                name    : "Klasse-${orgClassId}",
                                longName: "Klasse-${orgClassId}"
                        ]
                        timetableEntry.originalClasses << orgClass
                    }
                }
            }

            entries << timetableEntry
        }

        return entries
    }

    List<Map> getHomework2017(WebUntisSession session, LocalDate startDate, LocalDate endDate, int studentId) {
        def url = "https://${session.server}/WebUntis/jsonrpc_intern.do?school=${session.school}"

        def authParams = [
                user      : session.username ?: "defaultUser",
                otp       : session.appSecret ? createOtp(session.appSecret) : "000000",
                clientTime: System.currentTimeMillis().toString()
        ]

        def params = [[
                              id                 : studentId,
                              type               : "STUDENT",
                              startDate          : startDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")),
                              endDate            : endDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")),
                              masterDataTimestamp: System.currentTimeMillis().toString(),
                              auth               : authParams
                      ]]

        def request = createJsonRpcRequest("getHomeWork2017", params)
        def headers = createStandardHeaders()
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
}