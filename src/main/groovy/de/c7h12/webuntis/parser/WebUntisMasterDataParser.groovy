package de.c7h12.webuntis.parser

import com.fasterxml.jackson.databind.JsonNode
import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import org.springframework.stereotype.Component

/**
 * Parser for WebUntis master data and API responses
 * Extracts and transforms JSON data into structured Maps
 */
@Slf4j
@CompileStatic
@Component
class WebUntisMasterDataParser {

    /**
     * Parses master data from WebUntis 2017 getUserData response
     */
    Map parseMasterData(JsonNode masterData) {
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

        // Timestamp
        if (masterData.has("timeStamp")) {
            parsedData.timestamp = masterData.get("timeStamp").asLong()
        }

        // Subjects
        if (masterData.has("subjects")) {
            masterData.get("subjects").each { su ->
                parsedData.subjects[su.get("id").asLong()] = parseSubject(su)
            }
        }

        // Teachers
        if (masterData.has("teachers")) {
            masterData.get("teachers").each { te ->
                parsedData.teachers[te.get("id").asLong()] = parseTeacher(te)
            }
        }

        // Rooms
        if (masterData.has("rooms")) {
            masterData.get("rooms").each { ro ->
                parsedData.rooms[ro.get("id").asLong()] = parseRoom(ro)
            }
        }

        // Classes (Klassen)
        if (masterData.has("klassen")) {
            masterData.get("klassen").each { kl ->
                parsedData.klassen[kl.get("id").asLong()] = parseClass(kl)
            }
        }

        // TimeGrid
        if (masterData.has("timeGrid")) {
            parsedData.timeGrid = parseTimeGrid(masterData.get("timeGrid"))
        }

        // School years
        if (masterData.has("schoolyears")) {
            masterData.get("schoolyears").each { sy ->
                parsedData.schoolyears << parseSchoolYear(sy)
            }
        }

        // Holidays
        if (masterData.has("holidays")) {
            masterData.get("holidays").each { ho ->
                parsedData.holidays << parseHoliday(ho)
            }
        }

        // Departments
        if (masterData.has("departments")) {
            masterData.get("departments").each { dep ->
                parsedData.departments[dep.get("id").asInt()] = parseDepartment(dep)
            }
        }

        // Absence Reasons
        if (masterData.has("absenceReasons")) {
            masterData.get("absenceReasons").each { ar ->
                parsedData.absenceReasons[ar.get("id").asInt()] = parseAbsenceReason(ar)
            }
        }

        // Excuse Statuses
        if (masterData.has("excuseStatuses")) {
            masterData.get("excuseStatuses").each { es ->
                parsedData.excuseStatuses[es.get("id").asInt()] = parseExcuseStatus(es)
            }
        }

        // Duties
        if (masterData.has("duties")) {
            masterData.get("duties").each { duty ->
                parsedData.duties[duty.get("id").asInt()] = parseDuty(duty)
            }
        }

        // Event Reasons
        if (masterData.has("eventReasons")) {
            masterData.get("eventReasons").each { er ->
                parsedData.eventReasons[er.get("id").asInt()] = parseEventReason(er)
            }
        }

        // Teaching Methods
        if (masterData.has("teachingMethods")) {
            masterData.get("teachingMethods").each { tm ->
                parsedData.teachingMethods << parseTeachingMethod(tm)
            }
        }

        logMasterDataStats(parsedData)
        return parsedData
    }

    /**
     * Parses a subject from JSON
     */
    private Map parseSubject(JsonNode su) {
        return [
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

    /**
     * Parses a teacher from JSON
     */
    private Map parseTeacher(JsonNode te) {
        return [
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

    /**
     * Parses a room from JSON
     */
    private Map parseRoom(JsonNode ro) {
        return [
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

    /**
     * Parses a class from JSON
     */
    private Map parseClass(JsonNode kl) {
        return [
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

    /**
     * Parses time grid from JSON
     */
    private Map parseTimeGrid(JsonNode timeGrid) {
        def days = [:]
        if (timeGrid.has("days")) {
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
        }
        return days
    }

    /**
     * Parses a school year from JSON
     */
    private Map parseSchoolYear(JsonNode sy) {
        return [
                id       : sy.get("id").asInt(),
                name     : sy.get("name").asText(),
                startDate: sy.get("startDate").asText(),
                endDate  : sy.get("endDate").asText()
        ]
    }

    /**
     * Parses a holiday from JSON
     */
    private Map parseHoliday(JsonNode ho) {
        return [
                id       : ho.get("id").asInt(),
                name     : ho.get("name").asText(),
                longName : ho.get("longName").asText(),
                startDate: ho.get("startDate").asText(),
                endDate  : ho.get("endDate").asText()
        ]
    }

    /**
     * Parses a department from JSON
     */
    private Map parseDepartment(JsonNode dep) {
        return [
                id      : dep.get("id").asInt(),
                name    : dep.get("name").asText(),
                longName: dep.get("longName").asText()
        ]
    }

    /**
     * Parses an absence reason from JSON
     */
    private Map parseAbsenceReason(JsonNode ar) {
        return [
                id                           : ar.get("id").asInt(),
                name                         : ar.get("name").asText(),
                longName                     : ar.get("longName").asText(),
                active                       : ar.has("active") ? ar.get("active").asBoolean() : true,
                automaticNotificationEnabled : ar.has("automaticNotificationEnabled") ? ar.get("automaticNotificationEnabled").asBoolean() : false
        ]
    }

    /**
     * Parses an excuse status from JSON
     */
    private Map parseExcuseStatus(JsonNode es) {
        return [
                id      : es.get("id").asInt(),
                name    : es.get("name").asText(),
                longName: es.get("longName").asText(),
                excused : es.has("excused") ? es.get("excused").asBoolean() : false,
                active  : es.has("active") ? es.get("active").asBoolean() : true
        ]
    }

    /**
     * Parses a duty from JSON
     */
    private Map parseDuty(JsonNode duty) {
        return [
                id      : duty.get("id").asInt(),
                name    : duty.get("name").asText(),
                longName: duty.get("longName").asText(),
                type    : duty.has("type") ? duty.get("type").asText() : null
        ]
    }

    /**
     * Parses an event reason from JSON
     */
    private Map parseEventReason(JsonNode er) {
        return [
                id         : er.get("id").asInt(),
                name       : er.get("name").asText(),
                longName   : er.get("longName").asText(),
                elementType: er.has("elementType") ? er.get("elementType").asText() : null,
                groupId    : er.has("groupId") ? er.get("groupId").asInt() : -1,
                active     : er.has("active") ? er.get("active").asBoolean() : true
        ]
    }

    /**
     * Parses a teaching method from JSON
     */
    private Map parseTeachingMethod(JsonNode tm) {
        return [
                id      : tm.get("id").asInt(),
                name    : tm.get("name").asText(),
                longName: tm.get("longName").asText()
        ]
    }

    /**
     * Logs statistics about parsed master data
     */
    private void logMasterDataStats(Map masterData) {
        log.debug("Master Data Statistics:")
        log.debug("  - Subjects: {}", masterData.subjects?.size() ?: 0)
        log.debug("  - Teachers: {}", masterData.teachers?.size() ?: 0)
        log.debug("  - Rooms: {}", masterData.rooms?.size() ?: 0)
        log.debug("  - Classes: {}", masterData.klassen?.size() ?: 0)
        log.debug("  - Departments: {}", masterData.departments?.size() ?: 0)
        log.debug("  - Absence Reasons: {}", masterData.absenceReasons?.size() ?: 0)
        log.debug("  - Excuse Statuses: {}", masterData.excuseStatuses?.size() ?: 0)
        log.debug("  - Holidays: {}", masterData.holidays?.size() ?: 0)
        log.debug("  - School Years: {}", masterData.schoolyears?.size() ?: 0)
        log.debug("  - TimeGrid Days: {}", masterData.timeGrid?.size() ?: 0)

        if (masterData.timestamp) {
            log.debug("  - Timestamp: {}", new Date(masterData.timestamp as Long))
        }
    }

    /**
     * Parses standard API subjects list
     */
    List<Map> parseSubjects(JsonNode result) {
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

    /**
     * Parses standard API teachers list
     */
    List<Map> parseTeachers(JsonNode result) {
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

    /**
     * Parses standard API rooms list
     */
    List<Map> parseRooms(JsonNode result) {
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

    /**
     * Parses standard API classes list
     */
    List<Map> parseClasses(JsonNode result) {
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
}
