package de.c7h12.webuntis.parser

import com.fasterxml.jackson.databind.JsonNode
import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import org.springframework.stereotype.Component

/**
 * Parser for WebUntis timetable data
 * Handles both standard API and enhanced 2017 API timetable formats
 */
@Slf4j
@CompileStatic
@Component
class WebUntisTimetableParser {

    /**
     * Parses standard API timetable entries with detailed information
     */
    List<Map> parseAdvancedTimetableEntries(JsonNode result) {
        def entries = []

        result.each { entry ->
            def timetableEntry = [
                    id: entry.get("id").asLong(),
                    date: entry.get("date").asInt(),
                    startTime: entry.get("startTime").asInt(),
                    endTime: entry.get("endTime").asInt(),

                    // Arrays for resolved data
                    subjects: [],
                    teachers: [],
                    rooms: [],
                    classes: [],

                    // Original arrays (for substitutions)
                    originalSubjects: [],
                    originalTeachers: [],
                    originalRooms: [],
                    originalClasses: [],

                    // Standard information
                    code: entry.has("code") ? entry.get("code").asText().toUpperCase() : "REGULAR",
                    info: entry.has("info") ? entry.get("info").asText() : null,
                    substText: entry.has("substText") ? entry.get("substText").asText() : null,
                    lsText: entry.has("lstext") ? entry.get("lstext").asText() : null,
                    lsNumber: entry.has("lsnumber") ? entry.get("lsnumber").asInt() : null,
                    studentGroup: entry.has("sg") ? entry.get("sg").asText() : null
            ]

            // Parse subjects
            if (entry.has("su")) {
                entry.get("su").each { su ->
                    def subject = parseElement(su, "Fach")
                    timetableEntry.subjects << subject

                    // Original subject for substitutions
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

            // Parse teachers
            if (entry.has("te")) {
                entry.get("te").each { te ->
                    def teacher = parseTeacherElement(te)
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

            // Parse rooms
            if (entry.has("ro")) {
                entry.get("ro").each { ro ->
                    def room = parseElement(ro, "Raum")
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

            // Parse classes
            if (entry.has("kl")) {
                entry.get("kl").each { kl ->
                    def clazz = parseElement(kl, "Klasse")
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

    /**
     * Parses enhanced 2017 API timetable with master data resolution
     */
    List<Map> parseEnhanced2017Timetable(JsonNode result, Map masterData) {
        def entries = []

        // Extract timetable data (can be direct array or in "timetable" field)
        def timetableData = result.has("timetable") ? result.get("timetable") : result

        if (timetableData == null || timetableData.isNull() || timetableData.size() == 0) {
            log.debug("No timetable data found in response")
            return entries
        }

        timetableData.each { entry ->
            if (entry == null || entry.isNull() || !entry.has("id") || entry.get("id").isNull()) {
                log.warn("Skipping timetable entry without ID")
                return // continue to next iteration
            }

            def timetableEntry = buildTimetableEntry(entry, masterData)
            entries << timetableEntry
        }

        return entries
    }

    /**
     * Parses WebUntis 2017 "periods" format
     */
    List<Map> parseWebUntis2017PeriodsFormat(JsonNode periods, Map masterData) {
        def entries = []

        if (periods == null || periods.isNull() || periods.size() == 0) {
            log.debug("No periods data to parse")
            return entries
        }

        periods.each { period ->
            if (period == null || period.isNull() || !period.has("id")) {
                log.warn("Skipping period without ID")
                return
            }

            def timetableEntry = buildPeriodEntry(period, masterData)
            entries << timetableEntry
        }

        log.debug("Parsed {} timetable entries from 2017 periods format", entries.size())
        return entries
    }

    /**
     * Builds a timetable entry from JSON and master data
     */
    private Map buildTimetableEntry(JsonNode entry, Map masterData) {
        def timetableEntry = [
                id              : entry.get("id").asLong(),
                date            : entry.has("date") && !entry.get("date").isNull() ? entry.get("date").asInt() : 0,
                startTime       : entry.has("startTime") && !entry.get("startTime").isNull() ? entry.get("startTime").asInt() : 0,
                endTime         : entry.has("endTime") && !entry.get("endTime").isNull() ? entry.get("endTime").asInt() : 0,

                subjects        : [],
                teachers        : [],
                rooms           : [],
                classes         : [],

                originalSubjects: [],
                originalTeachers: [],
                originalRooms   : [],
                originalClasses : [],

                code            : entry.has("code") && !entry.get("code").isNull() ? entry.get("code").asText().toUpperCase() : "REGULAR",
                activityType    : entry.has("activityType") && !entry.get("activityType").isNull() ? entry.get("activityType").asText() : null,
                info            : entry.has("info") && !entry.get("info").isNull() ? entry.get("info").asText() : null,
                substText       : entry.has("substText") && !entry.get("substText").isNull() ? entry.get("substText").asText() : null,
                is2017Format    : true
        ]

        // Parse subjects with master data
        if (entry.has("su") && !entry.get("su").isNull()) {
            entry.get("su").each { su ->
                if (su != null && !su.isNull() && su.has("id") && !su.get("id").isNull()) {
                    def subjectId = su.get("id").asLong()
                    def subject = masterData?.subjects?[subjectId] ?: buildFallbackElement(su, subjectId, "Fach")
                    timetableEntry.subjects << subject

                    if (su.has("orgid") && !su.get("orgid").isNull()) {
                        def orgSubjectId = su.get("orgid").asLong()
                        def orgSubject = masterData?.subjects?[orgSubjectId] ?: [id: orgSubjectId, name: "Fach-${orgSubjectId}", longName: "Fach-${orgSubjectId}"]
                        timetableEntry.originalSubjects << orgSubject
                    }
                }
            }
        }

        // Parse teachers with master data
        if (entry.has("te") && !entry.get("te").isNull()) {
            entry.get("te").each { te ->
                if (te != null && !te.isNull() && te.has("id") && !te.get("id").isNull()) {
                    def teacherId = te.get("id").asLong()
                    def teacher = masterData?.teachers?[teacherId] ?: buildFallbackTeacher(te, teacherId)
                    timetableEntry.teachers << teacher

                    if (te.has("orgid") && !te.get("orgid").isNull()) {
                        def orgTeacherId = te.get("orgid").asLong()
                        def orgTeacher = masterData?.teachers?[orgTeacherId] ?: [id: orgTeacherId, name: "Lehrer-${orgTeacherId}", firstName: "", lastName: ""]
                        timetableEntry.originalTeachers << orgTeacher
                    }
                }
            }
        }

        // Parse rooms with master data
        if (entry.has("ro") && !entry.get("ro").isNull()) {
            entry.get("ro").each { ro ->
                if (ro != null && !ro.isNull() && ro.has("id") && !ro.get("id").isNull()) {
                    def roomId = ro.get("id").asLong()
                    def room = masterData?.rooms?[roomId] ?: buildFallbackElement(ro, roomId, "Raum")
                    timetableEntry.rooms << room

                    if (ro.has("orgid") && !ro.get("orgid").isNull()) {
                        def orgRoomId = ro.get("orgid").asLong()
                        def orgRoom = masterData?.rooms?[orgRoomId] ?: [id: orgRoomId, name: "Raum-${orgRoomId}", longName: "Raum-${orgRoomId}"]
                        timetableEntry.originalRooms << orgRoom
                    }
                }
            }
        }

        // Parse classes with master data
        if (entry.has("kl") && !entry.get("kl").isNull()) {
            entry.get("kl").each { kl ->
                if (kl != null && !kl.isNull() && kl.has("id") && !kl.get("id").isNull()) {
                    def classId = kl.get("id").asLong()
                    def clazz = masterData?.klassen?[classId] ?: buildFallbackElement(kl, classId, "Klasse")
                    timetableEntry.classes << clazz

                    if (kl.has("orgid") && !kl.get("orgid").isNull()) {
                        def orgClassId = kl.get("orgid").asLong()
                        def orgClass = masterData?.klassen?[orgClassId] ?: [id: orgClassId, name: "Klasse-${orgClassId}", longName: "Klasse-${orgClassId}"]
                        timetableEntry.originalClasses << orgClass
                    }
                }
            }
        }

        return timetableEntry
    }

    /**
     * Builds a period entry from 2017 API format
     */
    private Map buildPeriodEntry(JsonNode period, Map masterData) {
        // Parse DateTime
        def startDateTime = period.has("startDateTime") ? period.get("startDateTime").asText() : null
        def endDateTime = period.has("endDateTime") ? period.get("endDateTime").asText() : null

        def (date, startTime, endTime) = parseDateTimePeriod(startDateTime, endDateTime)

        def timetableEntry = [
                id              : period.get("id").asLong(),
                lessonId        : period.has("lessonId") ? period.get("lessonId").asLong() : null,
                date            : date,
                startTime       : startTime,
                endTime         : endTime,

                subjects        : [],
                teachers        : [],
                rooms           : [],
                classes         : [],

                originalSubjects: [],
                originalTeachers: [],
                originalRooms   : [],
                originalClasses : [],

                foreColor       : period.has("foreColor") ? period.get("foreColor").asText() : null,
                backColor       : period.has("backColor") ? period.get("backColor").asText() : null,
                isOnlinePeriod  : period.has("isOnlinePeriod") ? period.get("isOnlinePeriod").asBoolean() : false,
                code            : "REGULAR",
                is2017Format    : true
        ]

        // Extract status code
        if (period.has("is") && !period.get("is").isNull() && period.get("is").isArray()) {
            def isArray = period.get("is")
            if (isArray.size() > 0) {
                timetableEntry.code = isArray.get(0).asText().toUpperCase()
            }
        }

        // Extract text information
        if (period.has("text") && !period.get("text").isNull()) {
            def textObj = period.get("text")
            timetableEntry.lessonText = textObj.has("lesson") ? textObj.get("lesson").asText() : null
            timetableEntry.substitutionText = textObj.has("substitution") ? textObj.get("substitution").asText() : null
            timetableEntry.info = textObj.has("info") ? textObj.get("info").asText() : null
        }

        // Parse homework
        timetableEntry.homeWorks = parseHomeworks(period)

        // Parse elements (teachers, subjects, rooms, classes)
        if (period.has("elements") && !period.get("elements").isNull() && period.get("elements").isArray()) {
            parseElements(period.get("elements"), timetableEntry, masterData)
        }

        return timetableEntry
    }

    /**
     * Parses DateTime from ISO format
     */
    private List parseDateTimePeriod(String startDateTime, String endDateTime) {
        def date = 0
        def startTime = 0
        def endTime = 0

        if (startDateTime) {
            def dateParts = startDateTime.split("T")
            if (dateParts.length >= 2) {
                date = dateParts[0].replace("-", "").toInteger()
                def timePart = dateParts[1].replace("Z", "").replace(":", "")
                startTime = timePart.substring(0, Math.min(4, timePart.length())).toInteger()
            }
        }

        if (endDateTime) {
            def timePart = endDateTime.split("T")[1].replace("Z", "").replace(":", "")
            endTime = timePart.substring(0, Math.min(4, timePart.length())).toInteger()
        }

        return [date, startTime, endTime]
    }

    /**
     * Parses homework from period
     */
    private List parseHomeworks(JsonNode period) {
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
        return homeWorks
    }

    /**
     * Parses elements (teachers, subjects, rooms, classes) from period
     */
    private void parseElements(JsonNode elements, Map timetableEntry, Map masterData) {
        elements.each { element ->
            if (!element.has("type") || !element.has("id")) return

            def elementType = element.get("type").asText()
            def elementId = element.get("id").asLong()
            def orgId = element.has("orgId") ? element.get("orgId").asLong() : null

            switch (elementType) {
                case "TEACHER":
                    def teacher = masterData?.teachers?[elementId] ?: [id: elementId, name: "Lehrer-${elementId}", firstName: "", lastName: ""]
                    timetableEntry.teachers << teacher

                    if (orgId != null && orgId != elementId) {
                        def orgTeacher = masterData?.teachers?[orgId] ?: [id: orgId, name: "Lehrer-${orgId}", firstName: "", lastName: ""]
                        timetableEntry.originalTeachers << orgTeacher
                    }
                    break

                case "SUBJECT":
                    def subject = masterData?.subjects?[elementId] ?: [id: elementId, name: "Fach-${elementId}", longName: "Fach-${elementId}"]
                    timetableEntry.subjects << subject

                    if (orgId != null && orgId != elementId) {
                        def orgSubject = masterData?.subjects?[orgId] ?: [id: orgId, name: "Fach-${orgId}", longName: "Fach-${orgId}"]
                        timetableEntry.originalSubjects << orgSubject
                    }
                    break

                case "ROOM":
                    def room = masterData?.rooms?[elementId] ?: [id: elementId, name: "Raum-${elementId}", longName: "Raum-${elementId}"]
                    timetableEntry.rooms << room

                    if (orgId != null && orgId != elementId) {
                        def orgRoom = masterData?.rooms?[orgId] ?: [id: orgId, name: "Raum-${orgId}", longName: "Raum-${orgId}"]
                        timetableEntry.originalRooms << orgRoom
                    }
                    break

                case "CLASS":
                    def clazz = masterData?.klassen?[elementId] ?: [id: elementId, name: "Klasse-${elementId}", longName: "Klasse-${elementId}"]
                    timetableEntry.classes << clazz

                    if (orgId != null && orgId != elementId) {
                        def orgClass = masterData?.klassen?[orgId] ?: [id: orgId, name: "Klasse-${orgId}", longName: "Klasse-${orgId}"]
                        timetableEntry.originalClasses << orgClass
                    }
                    break
            }
        }
    }

    /**
     * Parses a generic element (subject, room, class)
     */
    private Map parseElement(JsonNode element, String prefix) {
        return [
                id: element.get("id").asLong(),
                name: element.has("name") ? element.get("name").asText() : "${prefix}-${element.get("id").asLong()}",
                longName: element.has("longname") ? element.get("longname").asText() : null
        ]
    }

    /**
     * Parses a teacher element
     */
    private Map parseTeacherElement(JsonNode te) {
        return [
                id: te.get("id").asLong(),
                name: te.has("name") ? te.get("name").asText() : "Lehrer-${te.get("id").asLong()}",
                foreName: te.has("forename") ? te.get("forename").asText() : "",
                longName: te.has("longname") ? te.get("longname").asText() : null
        ]
    }

    /**
     * Builds fallback element when master data is not available
     */
    private Map buildFallbackElement(JsonNode element, Long id, String prefix) {
        return [
                id      : id,
                name    : element.has("name") && !element.get("name").isNull() ? element.get("name").asText() : "${prefix}-${id}",
                longName: element.has("longname") && !element.get("longname").isNull() ? element.get("longname").asText() : "${prefix}-${id}"
        ]
    }

    /**
     * Builds fallback teacher when master data is not available
     */
    private Map buildFallbackTeacher(JsonNode element, Long id) {
        return [
                id       : id,
                name     : element.has("name") && !element.get("name").isNull() ? element.get("name").asText() : "Lehrer-${id}",
                firstName: element.has("firstName") && !element.get("firstName").isNull() ? element.get("firstName").asText() : "",
                lastName : element.has("lastName") && !element.get("lastName").isNull() ? element.get("lastName").asText() : ""
        ]
    }
}
