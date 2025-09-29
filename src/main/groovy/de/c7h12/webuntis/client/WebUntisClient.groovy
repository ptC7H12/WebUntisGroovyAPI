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

        // Korrekte 2017 API Struktur mit auth-Objekt
        def params = [[
                              masterDataTimestamp: currentTime.toString(),
                              type: "STUDENT",
                              auth: [
                                      user: username,
                                      otp: otp,
                                      clientTime: currentTime.toString()
                              ]
                      ]]

        def request = createJsonRpcRequest("getUserData2017", params)
        def headers = createStandardHeaders()
        def entity = new HttpEntity<>(request, headers)

        try {
            println "DEBUG: Authenticating with 2017 API..."
            println "DEBUG: URL: ${url}"
            println "DEBUG: User: ${username}"
            println "DEBUG: OTP: ${otp}"
            println "DEBUG: ClientTime: ${currentTime}"

            ResponseEntity<String> response = restTemplate.postForEntity(url, entity, String)
            JsonNode jsonResponse = objectMapper.readTree(response.body)

            if (jsonResponse.has("error")) {
                def error = jsonResponse.get("error")
                def errorMsg = error.get("message").asText()
                def errorCode = error.has("code") ? error.get("code").asInt() : -1
                throw new WebUntisException("Enhanced authentication failed [${errorCode}]: ${errorMsg}")
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

            logMasterDataStats(session.masterData)

            println "DEBUG: 2017 API authentication successful"
            println "DEBUG: Session ID: ${sessionId}"
            println "DEBUG: Person ID: ${personId}"

            if (userData.has("displayName")) {
                println "DEBUG: Authenticated as: ${userData.get("displayName").asText()}"
            }

            return session

        } catch (Exception e) {
            println "ERROR: 2017 API authentication failed: ${e.message}"
            throw new WebUntisException("Enhanced authentication error: ${e.message}")
        }
    }

    // Hilfsmethode um sicherzustellen dass getUserData2017 geladen ist
    private void ensureUserData2017(WebUntisSession session) {
        if (!session.hasMasterData()) {
            println "DEBUG: Master data not cached, loading getUserData2017..."
            loadUserData2017(session)
        } else {
            // Optional: Cache-Zeitvalidierung (Master-Daten älter als 5 Minuten?)
            if (session.masterData?.timestamp) {
                def cacheAge = System.currentTimeMillis() - session.masterData.timestamp
                def maxCacheAge = 5 * 60 * 1000 // 5 Minuten

                if (cacheAge > maxCacheAge) {
                    println "DEBUG: Master data cache is ${cacheAge / 1000}s old, refreshing..."
                    loadUserData2017(session)
                } else {
                    println "DEBUG: Using cached master data (${cacheAge / 1000}s old)"
                }
            } else {
                println "DEBUG: Using cached master data (no timestamp)"
            }
        }
    }

    // Interne Methode zum Laden der UserData2017 ohne neue Session zu erstellen
    private void loadUserData2017(WebUntisSession session) {
        def url = "https://${session.server}/WebUntis/jsonrpc_intern.do?school=${session.school}"

        def currentTime = System.currentTimeMillis()
        def otp = createOtp(session.appSecret)

        def params = [[
                              masterDataTimestamp: currentTime.toString(),
                              type: "STUDENT",
                              auth: [
                                      user: session.username,
                                      otp: otp,
                                      clientTime: currentTime.toString()
                              ]
                      ]]

        def request = createJsonRpcRequest("getUserData2017", params)
        def headers = createStandardHeaders()
        def entity = new HttpEntity<>(request, headers)

        try {
            println "DEBUG: Loading getUserData2017 for session..."

            ResponseEntity<String> response = restTemplate.postForEntity(url, entity, String)
            JsonNode jsonResponse = objectMapper.readTree(response.body)

            if (jsonResponse.has("error")) {
                throw new WebUntisException("UserData2017 loading failed: ${jsonResponse.get("error").get("message").asText()}")
            }

            def result = jsonResponse.get("result")

            // Master-Daten in Session speichern
            if (result.has("masterData")) {
                session.masterData = parseMasterDataFrom2017Response(result.get("masterData"))
                logMasterDataStats(session.masterData)
            }

            // PersonId und andere User-Daten aktualisieren
            if (result.has("userData")) {
                def userData = result.get("userData")

                // PersonId aktualisieren falls nötig
                if (userData.has("elemId")) {
                    def newPersonId = userData.get("elemId").asInt()
                    if (session.personId != newPersonId) {
                        println "DEBUG: Updating personId from ${session.personId} to ${newPersonId}"
                        session.personId = newPersonId
                    }
                }

                // Display Name loggen
                if (userData.has("displayName")) {
                    println "DEBUG: User: ${userData.get("displayName").asText()}"
                }

                // School Name loggen
                if (userData.has("schoolName")) {
                    println "DEBUG: School: ${userData.get("schoolName").asText()}"
                }
            }

            // Settings loggen falls vorhanden
            if (result.has("settings")) {
                def settings = result.get("settings")
                println "DEBUG: User settings loaded (${settings.size()} settings)"
            }

            println "DEBUG: getUserData2017 successfully loaded and cached"

        } catch (Exception e) {
            println "ERROR: Failed to load getUserData2017: ${e.message}"
            throw new WebUntisException("Error loading user data 2017: ${e.message}")
        }
    }



    void logout(WebUntisSession session) {
        if (!session?.sessionId) return

        try {
            def url = "https://${session.server}/WebUntis/jsonrpc.do?school=${session.school}"
            def request = createJsonRpcRequest("logout", [:])
            def headers = createAuthenticatedHeaders(session)
            def entity = new HttpEntity<>(request, headers)

            restTemplate.postForEntity(url, entity, String)
            println "DEBUG: Successfully logged out session ${session.sessionId}"
        } catch (Exception e) {
            println "DEBUG: Logout failed: ${e.message}"
        }
    }

    // ========== Utility Methoden ==========

    private Map createJsonRpcRequest(String method, Object params) {
        return [
                jsonrpc: "2.0",
                id: System.currentTimeMillis().toString(),
                method: method,
                params: params
        ]
    }

    private HttpHeaders createAuthenticatedHeaders(WebUntisSession session) {
        def headers = new HttpHeaders().tap {
            contentType = MediaType.APPLICATION_JSON
            set("User-Agent", "SpringBoot-Groovy-WebUntis-Client")
            if (session.cookies) {
                set("Cookie", session.cookies)
            }
            if (session.sessionId) {
                set("X-Session-Id", session.sessionId)
            }
        }
        return headers
    }

    private HttpHeaders createStandardHeaders() {
        return new HttpHeaders().tap {
            contentType = MediaType.APPLICATION_JSON
            set("User-Agent", "SpringBoot-Groovy-WebUntis-Client")
        }
    }

    // ========== Parser-Methoden ==========

    private List<Map> parseAdvancedTimetableEntries(JsonNode result) {
        def entries = []

        result.each { entry ->
            def timetableEntry = [
                    id: entry.get("id").asLong(),
                    date: entry.get("date").asInt(),
                    startTime: entry.get("startTime").asInt(),
                    endTime: entry.get("endTime").asInt(),

                    // Arrays für aufgelöste Daten
                    subjects: [],
                    teachers: [],
                    rooms: [],
                    classes: [],

                    // Original-Arrays (für Vertretungen)
                    originalSubjects: [],
                    originalTeachers: [],
                    originalRooms: [],
                    originalClasses: [],

                    // Standard Informationen
                    code: entry.has("code") ? entry.get("code").asText().toUpperCase() : "REGULAR",
                    info: entry.has("info") ? entry.get("info").asText() : null,
                    substText: entry.has("substText") ? entry.get("substText").asText() : null,
                    lsText: entry.has("lstext") ? entry.get("lstext").asText() : null,
                    lsNumber: entry.has("lsnumber") ? entry.get("lsnumber").asInt() : null,
                    studentGroup: entry.has("sg") ? entry.get("sg").asText() : null
            ]

            // Subjects verarbeiten
            if (entry.has("su")) {
                entry.get("su").each { su ->
                    def subject = [
                            id: su.get("id").asLong(),
                            name: su.has("name") ? su.get("name").asText() : "Fach-${su.get("id").asLong()}",
                            longName: su.has("longname") ? su.get("longname").asText() : null
                    ]
                    timetableEntry.subjects << subject

                    // Original-Fach bei Vertretungen
                    if (su.has("orgid")) {
                        def orgSubject = [
                                id: su.get("orgid").asLong(),
                                name: "Fach-${su.get("orgid").asLong()}",
                                longName: null
                        ]
                        timetableEntry.originalSubjects << orgSubject
                    }
                }
            }

            // Teachers verarbeiten
            if (entry.has("te")) {
                entry.get("te").each { te ->
                    def teacher = [
                            id: te.get("id").asLong(),
                            name: te.has("name") ? te.get("name").asText() : "Lehrer-${te.get("id").asLong()}",
                            foreName: te.has("forename") ? te.get("forename").asText() : "",
                            longName: te.has("longname") ? te.get("longname").asText() : null
                    ]
                    timetableEntry.teachers << teacher

                    if (te.has("orgid")) {
                        def orgTeacher = [
                                id: te.get("orgid").asLong(),
                                name: "Lehrer-${te.get("orgid").asLong()}",
                                foreName: "",
                                longName: null
                        ]
                        timetableEntry.originalTeachers << orgTeacher
                    }
                }
            }

            // Rooms verarbeiten
            if (entry.has("ro")) {
                entry.get("ro").each { ro ->
                    def room = [
                            id: ro.get("id").asLong(),
                            name: ro.has("name") ? ro.get("name").asText() : "Raum-${ro.get("id").asLong()}",
                            longName: ro.has("longname") ? ro.get("longname").asText() : null
                    ]
                    timetableEntry.rooms << room

                    if (ro.has("orgid")) {
                        def orgRoom = [
                                id: ro.get("orgid").asLong(),
                                name: "Raum-${ro.get("orgid").asLong()}",
                                longName: null
                        ]
                        timetableEntry.originalRooms << orgRoom
                    }
                }
            }

            // Classes verarbeiten
            if (entry.has("kl")) {
                entry.get("kl").each { kl ->
                    def clazz = [
                            id: kl.get("id").asLong(),
                            name: kl.has("name") ? kl.get("name").asText() : "Klasse-${kl.get("id").asLong()}",
                            longName: kl.has("longname") ? kl.get("longname").asText() : null
                    ]
                    timetableEntry.classes << clazz

                    if (kl.has("orgid")) {
                        def orgClass = [
                                id: kl.get("orgid").asLong(),
                                name: "Klasse-${kl.get("orgid").asLong()}",
                                longName: null
                        ]
                        timetableEntry.originalClasses << orgClass
                    }
                }
            }

            entries << timetableEntry
        }

        return entries
    }

    private List<Map> parseSubjects(JsonNode result) {
        def subjects = []
        result.each { subject ->
            subjects << [
                    id: subject.get("id").asLong(),
                    name: subject.get("name").asText(),
                    longName: subject.get("longName").asText(),
                    foreColor: subject.has("foreColor") ? subject.get("foreColor").asText() : null,
                    backColor: subject.has("backColor") ? subject.get("backColor").asText() : null,
                    active: subject.has("active") ? subject.get("active").asBoolean() : true
            ]
        }
        return subjects
    }

    private List<Map> parseTeachers(JsonNode result) {
        def teachers = []
        result.each { teacher ->
            teachers << [
                    id: teacher.get("id").asLong(),
                    name: teacher.get("name").asText(),
                    foreName: teacher.has("foreName") ? teacher.get("foreName").asText() : "",
                    longName: teacher.has("longName") ? teacher.get("longName").asText() : teacher.get("name").asText(),
                    active: teacher.has("active") ? teacher.get("active").asBoolean() : true
            ]
        }
        return teachers
    }

    private List<Map> parseRooms(JsonNode result) {
        def rooms = []
        result.each { room ->
            rooms << [
                    id: room.get("id").asLong(),
                    name: room.get("name").asText(),
                    longName: room.has("longName") ? room.get("longName").asText() : room.get("name").asText(),
                    active: room.has("active") ? room.get("active").asBoolean() : true
            ]
        }
        return rooms
    }

    private List<Map> parseClasses(JsonNode result) {
        def classes = []
        result.each { clazz ->
            classes << [
                    id: clazz.get("id").asLong(),
                    name: clazz.get("name").asText(),
                    longName: clazz.has("longName") ? clazz.get("longName").asText() : clazz.get("name").asText(),
                    active: clazz.has("active") ? clazz.get("active").asBoolean() : true
            ]
        }
        return classes
    }

    // ========== 2017 API Parser mit Master-Daten ==========

    // Neue Parser-Methode die Master-Daten aus der Session nutzt
    // In WebUntisClient.groovy - parseEnhanced2017TimetableWithMasterData ersetzen:

    private List<Map> parseEnhanced2017TimetableWithMasterData(JsonNode result, Map masterData) {
        def entries = []

        // Timetable-Daten extrahieren (kann direkt ein Array sein oder in einem "timetable" Feld)
        def timetableData = result.has("timetable") ? result.get("timetable") : result

        // WICHTIG: Prüfen ob timetableData null oder leer ist
        if (timetableData == null || timetableData.isNull() || timetableData.size() == 0) {
            println "DEBUG: No timetable data found in response"
            return entries
        }

        timetableData.each { entry ->
            // NULL-Check für entry
            if (entry == null || entry.isNull()) {
                return // continue zur nächsten Iteration
            }

            // NULL-Check für id - wenn keine ID, Entry überspringen
            if (!entry.has("id") || entry.get("id").isNull()) {
                println "WARN: Skipping timetable entry without ID"
                return // continue
            }

            def timetableEntry = [
                    id              : entry.get("id").asLong(),
                    date            : entry.has("date") && !entry.get("date").isNull() ? entry.get("date").asInt() : 0,
                    startTime       : entry.has("startTime") && !entry.get("startTime").isNull() ? entry.get("startTime").asInt() : 0,
                    endTime         : entry.has("endTime") && !entry.get("endTime").isNull() ? entry.get("endTime").asInt() : 0,

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
                    code            : entry.has("code") && !entry.get("code").isNull() ? entry.get("code").asText().toUpperCase() : "REGULAR",
                    activityType    : entry.has("activityType") && !entry.get("activityType").isNull() ? entry.get("activityType").asText() : null,
                    info            : entry.has("info") && !entry.get("info").isNull() ? entry.get("info").asText() : null,
                    substText       : entry.has("substText") && !entry.get("substText").isNull() ? entry.get("substText").asText() : null,
                    lsText          : entry.has("lstext") && !entry.get("lstext").isNull() ? entry.get("lstext").asText() : null,
                    lsNumber        : entry.has("lsnumber") && !entry.get("lsnumber").isNull() ? entry.get("lsnumber").asInt() : null,
                    studentGroup    : entry.has("sg") && !entry.get("sg").isNull() ? entry.get("sg").asText() : null,
                    statflags       : entry.has("statflags") && !entry.get("statflags").isNull() ? entry.get("statflags").asText() : null,
                    bkRemark        : entry.has("bkRemark") && !entry.get("bkRemark").isNull() ? entry.get("bkRemark").asText() : null,
                    bkText          : entry.has("bkText") && !entry.get("bkText").isNull() ? entry.get("bkText").asText() : null,
                    lstype          : entry.has("lstype") && !entry.get("lstype").isNull() ? entry.get("lstype").asText() : null,

                    // Weitere 2017 spezifische Felder
                    rescheduleInfo  : entry.has("rescheduleInfo") && !entry.get("rescheduleInfo").isNull() ? entry.get("rescheduleInfo").asText() : null,
                    periodInfo      : entry.has("periodInfo") && !entry.get("periodInfo").isNull() ? entry.get("periodInfo").asText() : null,
                    is2017Format    : true
            ]

            // Subjects mit Master-Daten auflösen
            if (entry.has("su") && !entry.get("su").isNull()) {
                entry.get("su").each { su ->
                    if (su != null && !su.isNull() && su.has("id") && !su.get("id").isNull()) {
                        def subjectId = su.get("id").asLong()
                        def subject = masterData?.subjects?[subjectId] ?: [
                                id      : subjectId,
                                name    : su.has("name") && !su.get("name").isNull() ? su.get("name").asText() : "Fach-${subjectId}",
                                longName: su.has("longname") && !su.get("longname").isNull() ? su.get("longname").asText() : "Fach-${subjectId}"
                        ]

                        timetableEntry.subjects << subject

                        // Original-Fach bei Vertretungen
                        if (su.has("orgid") && !su.get("orgid").isNull()) {
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
            }

            // Teachers mit Master-Daten auflösen
            if (entry.has("te") && !entry.get("te").isNull()) {
                entry.get("te").each { te ->
                    if (te != null && !te.isNull() && te.has("id") && !te.get("id").isNull()) {
                        def teacherId = te.get("id").asLong()
                        def teacher = masterData?.teachers?[teacherId] ?: [
                                id       : teacherId,
                                name     : te.has("name") && !te.get("name").isNull() ? te.get("name").asText() : "Lehrer-${teacherId}",
                                firstName: te.has("firstName") && !te.get("firstName").isNull() ? te.get("firstName").asText() : "",
                                lastName : te.has("lastName") && !te.get("lastName").isNull() ? te.get("lastName").asText() : ""
                        ]

                        timetableEntry.teachers << teacher

                        if (te.has("orgid") && !te.get("orgid").isNull()) {
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
            }

            // Rooms mit Master-Daten auflösen
            if (entry.has("ro") && !entry.get("ro").isNull()) {
                entry.get("ro").each { ro ->
                    if (ro != null && !ro.isNull() && ro.has("id") && !ro.get("id").isNull()) {
                        def roomId = ro.get("id").asLong()
                        def room = masterData?.rooms?[roomId] ?: [
                                id      : roomId,
                                name    : ro.has("name") && !ro.get("name").isNull() ? ro.get("name").asText() : "Raum-${roomId}",
                                longName: ro.has("longname") && !ro.get("longname").isNull() ? ro.get("longname").asText() : "Raum-${roomId}"
                        ]

                        timetableEntry.rooms << room

                        if (ro.has("orgid") && !ro.get("orgid").isNull()) {
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
            }

            // Classes mit Master-Daten auflösen
            if (entry.has("kl") && !entry.get("kl").isNull()) {
                entry.get("kl").each { kl ->
                    if (kl != null && !kl.isNull() && kl.has("id") && !kl.get("id").isNull()) {
                        def classId = kl.get("id").asLong()
                        def clazz = masterData?.klassen?[classId] ?: [
                                id      : classId,
                                name    : kl.has("name") && !kl.get("name").isNull() ? kl.get("name").asText() : "Klasse-${classId}",
                                longName: kl.has("longname") && !kl.get("longname").isNull() ? kl.get("longname").asText() : "Klasse-${classId}"
                        ]

                        timetableEntry.classes << clazz

                        if (kl.has("orgid") && !kl.get("orgid").isNull()) {
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
            }

            entries << timetableEntry
        }

        return entries
    }


    // Neuer Parser für WebUntis 2017 "periods" Format
    private List<Map> parseWebUntis2017PeriodsFormat(JsonNode periods, Map masterData) {
        def entries = []

        if (periods == null || periods.isNull() || periods.size() == 0) {
            println "DEBUG: No periods data to parse"
            return entries
        }

        periods.each { period ->
            if (period == null || period.isNull() || !period.has("id")) {
                println "WARN: Skipping period without ID"
                return
            }

            // DateTime parsen (ISO Format: "2025-10-02T13:40Z")
            def startDateTime = period.has("startDateTime") ? period.get("startDateTime").asText() : null
            def endDateTime = period.has("endDateTime") ? period.get("endDateTime").asText() : null

            // Datum und Zeit extrahieren
            def date = 0
            def startTime = 0
            def endTime = 0

            if (startDateTime) {
                // "2025-10-02T13:40Z" -> date: 20251002, time: 1340
                def dateParts = startDateTime.split("T")
                if (dateParts.length >= 2) {
                    date = dateParts[0].replace("-", "").toInteger() // "2025-10-02" -> 20251002
                    def timePart = dateParts[1].replace("Z", "").replace(":", "") // "13:40Z" -> "1340"
                    startTime = timePart.substring(0, Math.min(4, timePart.length())).toInteger()
                }
            }

            if (endDateTime) {
                def timePart = endDateTime.split("T")[1].replace("Z", "").replace(":", "")
                endTime = timePart.substring(0, Math.min(4, timePart.length())).toInteger()
            }

            def timetableEntry = [
                    id              : period.get("id").asLong(),
                    lessonId        : period.has("lessonId") ? period.get("lessonId").asLong() : null,
                    date            : date,
                    startTime       : startTime,
                    endTime         : endTime,

                    // Arrays für aufgelöste Daten
                    subjects        : [],
                    teachers        : [],
                    rooms           : [],
                    classes         : [],

                    // Original-Arrays (für Vertretungen)
                    originalSubjects: [],
                    originalTeachers: [],
                    originalRooms   : [],
                    originalClasses : [],

                    // 2017 Zusatzinfos
                    foreColor       : period.has("foreColor") ? period.get("foreColor").asText() : null,
                    backColor       : period.has("backColor") ? period.get("backColor").asText() : null,
                    isOnlinePeriod  : period.has("isOnlinePeriod") ? period.get("isOnlinePeriod").asBoolean() : false,

                    // Status aus "is" Array
                    code            : "REGULAR",

                    // Text-Informationen
                    lessonText      : null,
                    substitutionText: null,
                    info            : null,

                    is2017Format    : true
            ]

            // Status aus "is" Array extrahieren
            if (period.has("is") && !period.get("is").isNull() && period.get("is").isArray()) {
                def isArray = period.get("is")
                if (isArray.size() > 0) {
                    timetableEntry.code = isArray.get(0).asText().toUpperCase()
                }
            }

            // Text-Informationen extrahieren
            if (period.has("text") && !period.get("text").isNull()) {
                def textObj = period.get("text")
                timetableEntry.lessonText = textObj.has("lesson") ? textObj.get("lesson").asText() : null
                timetableEntry.substitutionText = textObj.has("substitution") ? textObj.get("substitution").asText() : null
                timetableEntry.info = textObj.has("info") ? textObj.get("info").asText() : null
            }

            // Hausaufgaben extrahieren
            def homeWorks = []
            if (period.has("homeWorks") && !period.get("homeWorks").isNull() && period.get("homeWorks").isArray()) {
                period.get("homeWorks").each { hw ->
                    homeWorks << [
                            id        : hw.has("id") ? hw.get("id").asLong() : null,
                            lessonId  : hw.has("lessonId") ? hw.get("lessonId").asLong() : null,
                            startDate : hw.has("startDate") ? hw.get("startDate").asText() : null,
                            endDate   : hw.has("endDate") ? hw.get("endDate").asText() : null,
                            text      : hw.has("text") ? hw.get("text").asText() : "",
                            remark    : hw.has("remark") && !hw.get("remark").isNull() ? hw.get("remark").asText() : null,
                            completed : hw.has("completed") ? hw.get("completed").asBoolean() : false
                    ]
                }
            }
            timetableEntry.homeWorks = homeWorks

            // Elements verarbeiten (teachers, subjects, rooms, classes)
            if (period.has("elements") && !period.get("elements").isNull() && period.get("elements").isArray()) {
                period.get("elements").each { element ->
                    if (!element.has("type") || !element.has("id")) return

                    def elementType = element.get("type").asText()
                    def elementId = element.get("id").asLong()
                    def orgId = element.has("orgId") ? element.get("orgId").asLong() : null

                    switch (elementType) {
                        case "TEACHER":
                            def teacher = masterData?.teachers?[elementId] ?: [
                                    id       : elementId,
                                    name     : "Lehrer-${elementId}",
                                    firstName: "",
                                    lastName : ""
                            ]
                            timetableEntry.teachers << teacher

                            // Original-Lehrer bei Vertretung
                            if (orgId != null && orgId != elementId) {
                                def orgTeacher = masterData?.teachers?[orgId] ?: [
                                        id       : orgId,
                                        name     : "Lehrer-${orgId}",
                                        firstName: "",
                                        lastName : ""
                                ]
                                timetableEntry.originalTeachers << orgTeacher
                            }
                            break

                        case "SUBJECT":
                            def subject = masterData?.subjects?[elementId] ?: [
                                    id      : elementId,
                                    name    : "Fach-${elementId}",
                                    longName: "Fach-${elementId}"
                            ]
                            timetableEntry.subjects << subject

                            if (orgId != null && orgId != elementId) {
                                def orgSubject = masterData?.subjects?[orgId] ?: [
                                        id      : orgId,
                                        name    : "Fach-${orgId}",
                                        longName: "Fach-${orgId}"
                                ]
                                timetableEntry.originalSubjects << orgSubject
                            }
                            break

                        case "ROOM":
                            def room = masterData?.rooms?[elementId] ?: [
                                    id      : elementId,
                                    name    : "Raum-${elementId}",
                                    longName: "Raum-${elementId}"
                            ]
                            timetableEntry.rooms << room

                            if (orgId != null && orgId != elementId) {
                                def orgRoom = masterData?.rooms?[orgId] ?: [
                                        id      : orgId,
                                        name    : "Raum-${orgId}",
                                        longName: "Raum-${orgId}"
                                ]
                                timetableEntry.originalRooms << orgRoom
                            }
                            break

                        case "CLASS":
                            def clazz = masterData?.klassen?[elementId] ?: [
                                    id      : elementId,
                                    name    : "Klasse-${elementId}",
                                    longName: "Klasse-${elementId}"
                            ]
                            timetableEntry.classes << clazz

                            if (orgId != null && orgId != elementId) {
                                def orgClass = masterData?.klassen?[orgId] ?: [
                                        id      : orgId,
                                        name    : "Klasse-${orgId}",
                                        longName: "Klasse-${orgId}"
                                ]
                                timetableEntry.originalClasses << orgClass
                            }
                            break
                    }
                }
            }

            entries << timetableEntry
        }

        println "DEBUG: Parsed ${entries.size()} timetable entries from 2017 periods format"
        return entries
    }


    // Master-Daten aus der 2017 getUserData Response extrahieren
    private Map parseMasterDataFrom2017Response(JsonNode masterData) {
        def parsedData = [
                subjects           : [:],
                teachers           : [:],
                rooms              : [:],
                klassen            : [:],
                timeGrid           : [:],
                schoolyears        : [],
                holidays           : [],
                departments        : [:],
                absenceReasons     : [:],
                excuseStatuses     : [:],
                duties             : [:],
                eventReasons       : [:],
                teachingMethods    : [],
                timestamp          : null
        ]

        // Timestamp extrahieren
        if (masterData.has("timeStamp")) {
            parsedData.timestamp = masterData.get("timeStamp").asLong()
        }

        // Subjects extrahieren
        if (masterData.has("subjects")) {
            masterData.get("subjects").each { su ->
                parsedData.subjects[su.get("id").asLong()] = [
                        id             : su.get("id").asLong(),
                        name           : su.get("name").asText(),
                        longName       : su.get("longName").asText(),
                        departmentIds  : su.has("departmentIds") ? su.get("departmentIds").collect { it.asInt() } : [],
                        foreColor      : su.has("foreColor") && !su.get("foreColor").isNull() ? su.get("foreColor").asText() : null,
                        backColor      : su.has("backColor") && !su.get("backColor").isNull() ? su.get("backColor").asText() : null,
                        active         : su.has("active") ? su.get("active").asBoolean() : true,
                        displayAllowed : su.has("displayAllowed") ? su.get("displayAllowed").asBoolean() : true
                ]
            }
        }

        // Teachers extrahieren
        if (masterData.has("teachers")) {
            masterData.get("teachers").each { te ->
                parsedData.teachers[te.get("id").asLong()] = [
                        id             : te.get("id").asLong(),
                        name           : te.get("name").asText(),
                        firstName      : te.has("firstName") ? te.get("firstName").asText() : "",
                        lastName       : te.has("lastName") ? te.get("lastName").asText() : "",
                        departmentIds  : te.has("departmentIds") ? te.get("departmentIds").collect { it.asInt() } : [],
                        foreColor      : te.has("foreColor") && !te.get("foreColor").isNull() ? te.get("foreColor").asText() : null,
                        backColor      : te.has("backColor") && !te.get("backColor").isNull() ? te.get("backColor").asText() : null,
                        entryDate      : te.has("entryDate") && !te.get("entryDate").isNull() ? te.get("entryDate").asText() : null,
                        exitDate       : te.has("exitDate") && !te.get("exitDate").isNull() ? te.get("exitDate").asText() : null,
                        active         : te.has("active") ? te.get("active").asBoolean() : true,
                        displayAllowed : te.has("displayAllowed") ? te.get("displayAllowed").asBoolean() : true
                ]
            }
        }

        // Rooms extrahieren
        if (masterData.has("rooms")) {
            masterData.get("rooms").each { ro ->
                parsedData.rooms[ro.get("id").asLong()] = [
                        id             : ro.get("id").asLong(),
                        name           : ro.get("name").asText(),
                        longName       : ro.get("longName").asText(),
                        departmentId   : ro.has("departmentId") ? ro.get("departmentId").asInt() : 0,
                        foreColor      : ro.has("foreColor") && !ro.get("foreColor").isNull() ? ro.get("foreColor").asText() : null,
                        backColor      : ro.has("backColor") && !ro.get("backColor").isNull() ? ro.get("backColor").asText() : null,
                        active         : ro.has("active") ? ro.get("active").asBoolean() : true,
                        displayAllowed : ro.has("displayAllowed") ? ro.get("displayAllowed").asBoolean() : true
                ]
            }
        }

        // Klassen extrahieren
        if (masterData.has("klassen")) {
            masterData.get("klassen").each { kl ->
                parsedData.klassen[kl.get("id").asLong()] = [
                        id             : kl.get("id").asLong(),
                        name           : kl.get("name").asText(),
                        longName       : kl.get("longName").asText(),
                        departmentId   : kl.has("departmentId") ? kl.get("departmentId").asInt() : 0,
                        startDate      : kl.has("startDate") ? kl.get("startDate").asText() : null,
                        endDate        : kl.has("endDate") ? kl.get("endDate").asText() : null,
                        foreColor      : kl.has("foreColor") && !kl.get("foreColor").isNull() ? kl.get("foreColor").asText() : null,
                        backColor      : kl.has("backColor") && !kl.get("backColor").isNull() ? kl.get("backColor").asText() : null,
                        active         : kl.has("active") ? kl.get("active").asBoolean() : true,
                        displayable    : kl.has("displayable") ? kl.get("displayable").asBoolean() : true
                ]
            }
        }

        // TimeGrid extrahieren (korrigierte Struktur)
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

        // Schuljahre extrahieren
        if (masterData.has("schoolyears")) {
            masterData.get("schoolyears").each { sy ->
                parsedData.schoolyears << [
                        id       : sy.get("id").asInt(),
                        name     : sy.get("name").asText(),
                        startDate: sy.get("startDate").asText(),
                        endDate  : sy.get("endDate").asText()
                ]
            }
        }

        // Ferien extrahieren
        if (masterData.has("holidays")) {
            masterData.get("holidays").each { ho ->
                parsedData.holidays << [
                        id       : ho.get("id").asInt(),
                        name     : ho.get("name").asText(),
                        longName : ho.get("longName").asText(),
                        startDate: ho.get("startDate").asText(),
                        endDate  : ho.get("endDate").asText()
                ]
            }
        }

        // Departments extrahieren
        if (masterData.has("departments")) {
            masterData.get("departments").each { dep ->
                parsedData.departments[dep.get("id").asInt()] = [
                        id      : dep.get("id").asInt(),
                        name    : dep.get("name").asText(),
                        longName: dep.get("longName").asText()
                ]
            }
        }

        // Absence Reasons extrahieren
        if (masterData.has("absenceReasons")) {
            masterData.get("absenceReasons").each { ar ->
                parsedData.absenceReasons[ar.get("id").asInt()] = [
                        id                           : ar.get("id").asInt(),
                        name                         : ar.get("name").asText(),
                        longName                     : ar.get("longName").asText(),
                        active                       : ar.has("active") ? ar.get("active").asBoolean() : true,
                        automaticNotificationEnabled : ar.has("automaticNotificationEnabled") ? ar.get("automaticNotificationEnabled").asBoolean() : false
                ]
            }
        }

        // Excuse Statuses extrahieren
        if (masterData.has("excuseStatuses")) {
            masterData.get("excuseStatuses").each { es ->
                parsedData.excuseStatuses[es.get("id").asInt()] = [
                        id      : es.get("id").asInt(),
                        name    : es.get("name").asText(),
                        longName: es.get("longName").asText(),
                        excused : es.has("excused") ? es.get("excused").asBoolean() : false,
                        active  : es.has("active") ? es.get("active").asBoolean() : true
                ]
            }
        }

        // Duties extrahieren
        if (masterData.has("duties")) {
            masterData.get("duties").each { duty ->
                parsedData.duties[duty.get("id").asInt()] = [
                        id      : duty.get("id").asInt(),
                        name    : duty.get("name").asText(),
                        longName: duty.get("longName").asText(),
                        type    : duty.has("type") ? duty.get("type").asText() : null
                ]
            }
        }

        // Event Reasons extrahieren
        if (masterData.has("eventReasons")) {
            masterData.get("eventReasons").each { er ->
                parsedData.eventReasons[er.get("id").asInt()] = [
                        id         : er.get("id").asInt(),
                        name       : er.get("name").asText(),
                        longName   : er.get("longName").asText(),
                        elementType: er.has("elementType") ? er.get("elementType").asText() : null,
                        groupId    : er.has("groupId") ? er.get("groupId").asInt() : -1,
                        active     : er.has("active") ? er.get("active").asBoolean() : true
                ]
            }
        }

        // Teaching Methods extrahieren (falls vorhanden)
        if (masterData.has("teachingMethods")) {
            masterData.get("teachingMethods").each { tm ->
                parsedData.teachingMethods << [
                        id      : tm.get("id").asInt(),
                        name    : tm.get("name").asText(),
                        longName: tm.get("longName").asText()
                ]
            }
        }

        return parsedData
    }

    // Erweiterte Homework-Parser mit Master-Daten
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

                // Lesson-Informationen aus lessonsById ermitteln
                if (hw.has("lessonId") && !hw.get("lessonId").isNull() && result.has("lessonsById")) {
                    def lessonId = hw.get("lessonId").asLong().toString()
                    def lessonsById = result.get("lessonsById")

                    if (lessonsById.has(lessonId)) {
                        def lesson = lessonsById.get(lessonId)

                        // Subject auflösen
                        if (lesson.has("subjectId") && !lesson.get("subjectId").isNull()) {
                            def subjectId = lesson.get("subjectId").asLong()
                            def subject = masterData?.subjects?[subjectId]
                            if (subject) {
                                homeworkEntry.subject = subject
                            }
                        }

                        // Klassen auflösen
                        if (lesson.has("klassenIds")) {
                            def classes = []
                            lesson.get("klassenIds").each { klassenIdNode ->
                                def klassenId = klassenIdNode.asLong()
                                def clazz = masterData?.klassen?[klassenId] ?: [
                                        id: klassenId,
                                        name: "Klasse-${klassenId}",
                                        longName: "Klasse-${klassenId}"
                                ]
                                classes << clazz
                            }
                            homeworkEntry.classes = classes
                        }

                        // Lehrer auflösen
                        if (lesson.has("teacherIds")) {
                            def teachers = []
                            lesson.get("teacherIds").each { teacherIdNode ->
                                def teacherId = teacherIdNode.asLong()
                                def teacher = masterData?.teachers?[teacherId] ?: [
                                        id: teacherId,
                                        name: "Lehrer-${teacherId}",
                                        firstName: "",
                                        lastName: ""
                                ]
                                teachers << teacher
                            }
                            homeworkEntry.teachers = teachers
                        }
                    }
                }

                homework << homeworkEntry
            }
        }

        println "DEBUG: Parsed ${homework.size()} homework entries"
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

        // Master-Daten parsen
        if (result.has("masterData")) {
            userData.masterData = parseMasterDataFrom2017Response(result.get("masterData"))
        }

        // User-Daten extrahieren
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

    // Hilfsmethode um Master-Daten-Statistiken zu loggen
    private void logMasterDataStats(Map masterData) {
        println "DEBUG: Master-Data Statistics:"
        println "  - Subjects: ${masterData.subjects?.size() ?: 0}"
        println "  - Teachers: ${masterData.teachers?.size() ?: 0}"
        println "  - Rooms: ${masterData.rooms?.size() ?: 0}"
        println "  - Classes: ${masterData.klassen?.size() ?: 0}"
        println "  - Departments: ${masterData.departments?.size() ?: 0}"
        println "  - Absence Reasons: ${masterData.absenceReasons?.size() ?: 0}"
        println "  - Excuse Statuses: ${masterData.excuseStatuses?.size() ?: 0}"
        println "  - Holidays: ${masterData.holidays?.size() ?: 0}"
        println "  - School Years: ${masterData.schoolyears?.size() ?: 0}"
        println "  - TimeGrid Days: ${masterData.timeGrid?.size() ?: 0}"

        if (masterData.timestamp) {
            println "  - Timestamp: ${new Date(masterData.timestamp)}"
        }
    }



    List<Map> getTimetable2017(WebUntisSession session, LocalDate startDate, LocalDate endDate, int elementId, String elementType) {
        // Sicherstellen dass Master-Daten verfügbar sind
        ensureUserData2017(session)

        def url = "https://${session.server}/WebUntis/jsonrpc_intern.do?school=${session.school}"

        def currentTime = System.currentTimeMillis()
        def otp = createOtp(session.appSecret)

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

        def request = createJsonRpcRequest("getTimetable2017", params)
        def headers = createStandardHeaders()
        def entity = new HttpEntity<>(request, headers)

        try {
            println "DEBUG: Requesting timetable for elementId=${elementId}, type=${elementType}, dates=${startDate} to ${endDate}"

            ResponseEntity<String> response = restTemplate.postForEntity(url, entity, String)
            JsonNode jsonResponse = objectMapper.readTree(response.body)

            // DEBUG: Response loggen
            println "DEBUG: Raw getTimetable2017 response: ${objectMapper.writeValueAsString(jsonResponse)}"

            if (jsonResponse.has("error")) {
                throw new WebUntisException("Timetable 2017 request failed: ${jsonResponse.get("error").get("message").asText()}")
            }

            def result = jsonResponse.get("result")

            // WICHTIG: WebUntis 2017 API gibt result.timetable.periods zurück!
            def timetableData
            if (result.has("timetable")) {
                def timetable = result.get("timetable")
                if (timetable.has("periods")) {
                    timetableData = timetable.get("periods")
                    println "DEBUG: Found ${timetableData.size()} periods in timetable"
                } else {
                    timetableData = timetable
                    println "DEBUG: Using timetable directly (${timetableData.size()} entries)"
                }
            } else {
                timetableData = result
                println "DEBUG: Using result directly"
            }

            return parseWebUntis2017PeriodsFormat(timetableData, session.masterData)

        } catch (Exception e) {
            println "ERROR: getTimetable2017 failed: ${e.message}"
            e.printStackTrace()
            throw new WebUntisException("Error getting timetable 2017: ${e.message}")
        }
    }


    List<Map> getHomework2017(WebUntisSession session, LocalDate startDate, LocalDate endDate, int studentId) {
        // getUserData2017 vor Homework-Aufruf
        ensureUserData2017(session)

        def url = "https://${session.server}/WebUntis/jsonrpc_intern.do?school=${session.school}"

        def currentTime = System.currentTimeMillis()
        def otp = createOtp(session.appSecret)

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

        def request = createJsonRpcRequest("getHomeWork2017", params)
        def headers = createStandardHeaders()
        def entity = new HttpEntity<>(request, headers)

        try {
            println "DEBUG: Requesting homework for studentId=${studentId}, dates=${startDate} to ${endDate}"

            ResponseEntity<String> response = restTemplate.postForEntity(url, entity, String)
            JsonNode jsonResponse = objectMapper.readTree(response.body)

            // DEBUG: Response loggen
            println "DEBUG: Raw getHomeWork2017 response: ${objectMapper.writeValueAsString(jsonResponse)}"

            if (jsonResponse.has("error")) {
                throw new WebUntisException("Homework request failed: ${jsonResponse.get("error").get("message").asText()}")
            }

            def result = jsonResponse.get("result")
            println "DEBUG: Homework result structure: ${result}"

            return parseHomework2017WithMasterData(result, session.masterData)

        } catch (Exception e) {
            println "ERROR: getHomework2017 failed: ${e.message}"
            e.printStackTrace()
            throw new WebUntisException("Error getting homework: ${e.message}")
        }
    }



    List<Map> getMessagesOfDay2017(WebUntisSession session, LocalDate date) {
        // getUserData2017 vor Messages-Aufruf
        ensureUserData2017(session)

        def url = "https://${session.server}/WebUntis/jsonrpc_intern.do?school=${session.school}"

        def currentTime = System.currentTimeMillis()
        def otp = createOtp(session.appSecret)

        def params = [[
                              date: date.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")),
                              masterDataTimestamp: currentTime.toString(),
                              auth: [
                                      user: session.username,
                                      otp: otp,
                                      clientTime: currentTime.toString()
                              ]
                      ]]

        def request = createJsonRpcRequest("getMessagesOfDay2017", params)
        def headers = createStandardHeaders()
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

    List<Map> getStudentAbsences2017(WebUntisSession session, LocalDate startDate, LocalDate endDate, boolean includeExcused, boolean includeUnexcused) {
        // getUserData2017 vor Absences-Aufruf
        ensureUserData2017(session)

        def url = "https://${session.server}/WebUntis/jsonrpc_intern.do?school=${session.school}"

        def currentTime = System.currentTimeMillis()
        def otp = createOtp(session.appSecret)

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

        def request = createJsonRpcRequest("getStudentAbsences2017", params)
        def headers = createStandardHeaders()
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

    // Öffentliche getUserData2017 Methode (für explizite Aufrufe)
    Map getUserData2017(WebUntisSession session) {
        def url = "https://${session.server}/WebUntis/jsonrpc_intern.do?school=${session.school}"

        def currentTime = System.currentTimeMillis()
        def otp = createOtp(session.appSecret)

        def params = [[
                              masterDataTimestamp: currentTime.toString(),
                              type: "STUDENT",
                              auth: [
                                      user: session.username,
                                      otp: otp,
                                      clientTime: currentTime.toString()
                              ]
                      ]]

        def request = createJsonRpcRequest("getUserData2017", params)
        def headers = createStandardHeaders()
        def entity = new HttpEntity<>(request, headers)

        try {
            ResponseEntity<String> response = restTemplate.postForEntity(url, entity, String)
            JsonNode jsonResponse = objectMapper.readTree(response.body)

            if (jsonResponse.has("error")) {
                throw new WebUntisException("UserData request failed: ${jsonResponse.get("error").get("message").asText()}")
            }

            def result = parseUserData(jsonResponse.get("result"))

            // Master-Daten auch in Session speichern für spätere Verwendung
            if (jsonResponse.get("result").has("masterData")) {
                session.masterData = parseMasterDataFrom2017Response(jsonResponse.get("result").get("masterData"))
            }

            return result

        } catch (Exception e) {
            throw new WebUntisException("Error getting user data: ${e.message}")
        }
    }
}