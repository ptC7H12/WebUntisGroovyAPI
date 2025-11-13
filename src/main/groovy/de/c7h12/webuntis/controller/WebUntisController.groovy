package de.c7h12.webuntis.controller

import de.c7h12.webuntis.dto.*
import de.c7h12.webuntis.service.WebUntisService
import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

import java.time.LocalDate

/**
 * REST Controller for WebUntis API integration
 * Provides both standard and enhanced 2017 API endpoints
 */
@Slf4j
@CompileStatic
@RestController
@RequestMapping("/api/webuntis")
@CrossOrigin(origins = "*")
@Tag(name = "WebUntis API", description = "Standard and Enhanced WebUntis API endpoints")
class WebUntisControllerV2 {

    @Autowired
    WebUntisService webUntisService

    // ========== Standard WebUntis API Endpoints ==========

    @Operation(
        summary = "Heutigen Stundenplan abrufen",
        description = "Ruft den Stundenplan für den aktuellen Tag ab (Standard WebUntis API)"
    )
    @ApiResponses([
        @ApiResponse(responseCode = "200", description = "Stundenplan erfolgreich abgerufen"),
        @ApiResponse(responseCode = "400", description = "Ungültige Anfrage oder Authentifizierung fehlgeschlagen"),
        @ApiResponse(responseCode = "429", description = "Rate Limit überschritten")
    ])
    @PostMapping("/timetable/today")
    ResponseEntity<List<Map>> getTimetableToday(@Valid @RequestBody AuthenticationRequest request) {
        log.info("Fetching timetable for today - school: {}, user: {}", request.school, request.username)

        def timetable = webUntisService.getTimetableToday(
            request.school,
            request.username,
            request.password,
            request.server
        )

        log.info("Successfully fetched {} timetable entries", timetable.size())
        return ResponseEntity.ok(timetable)
    }

    @Operation(
        summary = "Wochenstundenplan abrufen",
        description = "Ruft den Stundenplan für die aktuelle Woche (Montag-Freitag) ab"
    )
    @ApiResponses([
        @ApiResponse(responseCode = "200", description = "Wochenstundenplan erfolgreich abgerufen"),
        @ApiResponse(responseCode = "400", description = "Ungültige Anfrage"),
        @ApiResponse(responseCode = "429", description = "Rate Limit überschritten")
    ])
    @PostMapping("/timetable/week")
    ResponseEntity<List<Map>> getTimetableWeek(@Valid @RequestBody AuthenticationRequest request) {
        log.info("Fetching timetable for current week - school: {}, user: {}", request.school, request.username)

        def timetable = webUntisService.getTimetableCurrentWeek(
            request.school,
            request.username,
            request.password,
            request.server
        )

        log.info("Successfully fetched {} weekly timetable entries", timetable.size())
        return ResponseEntity.ok(timetable)
    }

    @Operation(
        summary = "Stundenplan für Zeitraum abrufen",
        description = "Ruft den Stundenplan für einen benutzerdefinierten Zeitraum ab"
    )
    @ApiResponses([
        @ApiResponse(responseCode = "200", description = "Stundenplan erfolgreich abgerufen"),
        @ApiResponse(responseCode = "400", description = "Ungültige Anfrage oder Datumsformat"),
        @ApiResponse(responseCode = "429", description = "Rate Limit überschritten")
    ])
    @PostMapping("/timetable/range")
    ResponseEntity<List<Map>> getTimetableRange(@Valid @RequestBody TimetableRangeRequest request) {
        log.info("Fetching timetable for date range: {} to {}", request.startDate, request.endDate)

        def startDate = LocalDate.parse(request.startDate)
        def endDate = LocalDate.parse(request.endDate)

        def timetable = webUntisService.getTimetableForDateRange(
            request.school,
            request.username,
            request.password,
            request.server,
            startDate,
            endDate
        )

        log.info("Successfully fetched {} timetable entries for date range", timetable.size())
        return ResponseEntity.ok(timetable)
    }

    @Operation(
        summary = "Fächer abrufen",
        description = "Ruft die Liste aller Fächer/Unterrichtsfächer ab"
    )
    @ApiResponses([
        @ApiResponse(responseCode = "200", description = "Fächer erfolgreich abgerufen"),
        @ApiResponse(responseCode = "400", description = "Ungültige Anfrage"),
        @ApiResponse(responseCode = "429", description = "Rate Limit überschritten")
    ])
    @PostMapping("/subjects")
    ResponseEntity<List<Map>> getSubjects(@Valid @RequestBody AuthenticationRequest request) {
        log.info("Fetching subjects - school: {}", request.school)

        def subjects = webUntisService.getSubjects(
            request.school,
            request.username,
            request.password,
            request.server
        )

        log.info("Successfully fetched {} subjects", subjects.size())
        return ResponseEntity.ok(subjects)
    }

    @Operation(
        summary = "Lehrer abrufen",
        description = "Ruft die Liste aller Lehrer ab"
    )
    @ApiResponses([
        @ApiResponse(responseCode = "200", description = "Lehrer erfolgreich abgerufen"),
        @ApiResponse(responseCode = "400", description = "Ungültige Anfrage"),
        @ApiResponse(responseCode = "429", description = "Rate Limit überschritten")
    ])
    @PostMapping("/teachers")
    ResponseEntity<List<Map>> getTeachers(@Valid @RequestBody AuthenticationRequest request) {
        log.info("Fetching teachers - school: {}", request.school)

        def teachers = webUntisService.getTeachers(
            request.school,
            request.username,
            request.password,
            request.server
        )

        log.info("Successfully fetched {} teachers", teachers.size())
        return ResponseEntity.ok(teachers)
    }

    @Operation(
        summary = "Räume abrufen",
        description = "Ruft die Liste aller Räume ab"
    )
    @ApiResponses([
        @ApiResponse(responseCode = "200", description = "Räume erfolgreich abgerufen"),
        @ApiResponse(responseCode = "400", description = "Ungültige Anfrage"),
        @ApiResponse(responseCode = "429", description = "Rate Limit überschritten")
    ])
    @PostMapping("/rooms")
    ResponseEntity<List<Map>> getRooms(@Valid @RequestBody AuthenticationRequest request) {
        log.info("Fetching rooms - school: {}", request.school)

        def rooms = webUntisService.getRooms(
            request.school,
            request.username,
            request.password,
            request.server
        )

        log.info("Successfully fetched {} rooms", rooms.size())
        return ResponseEntity.ok(rooms)
    }

    // ========== Enhanced 2017 API Endpoints ==========

    @Operation(
        summary = "Erweiterten Stundenplan abrufen (2017 API)",
        description = "Ruft den erweiterten Stundenplan mit Hausaufgaben, Farben und zusätzlichen Informationen ab. Benötigt App Secret."
    )
    @ApiResponses([
        @ApiResponse(responseCode = "200", description = "Erweiterter Stundenplan erfolgreich abgerufen"),
        @ApiResponse(responseCode = "400", description = "Ungültige Anfrage oder App Secret fehlt"),
        @ApiResponse(responseCode = "429", description = "Rate Limit überschritten")
    ])
    @PostMapping("/v2017/timetable")
    ResponseEntity<Map<String, Object>> getTimetable2017(@Valid @RequestBody Timetable2017Request request) {
        log.info("Fetching enhanced 2017 timetable - school: {}, dateRange: {} to {}",
            request.school, request.startDate, request.endDate)

        def startDate = LocalDate.parse(request.startDate)
        def endDate = LocalDate.parse(request.endDate)

        def timetable = webUntisService.getTimetable2017(
            request.school,
            request.username,
            request.password,
            request.server,
            startDate,
            endDate,
            request.elementType ?: "STUDENT",
            request.appSecret
        )

        log.info("Successfully fetched {} enhanced timetable entries", timetable.size())

        return ResponseEntity.ok([
            status: "success",
            format: "2017-enhanced",
            dataCount: timetable.size(),
            data: timetable
        ] as Map<String, Object>)
    }

    @Operation(
        summary = "Hausaufgaben abrufen (2017 API)",
        description = "Ruft Hausaufgaben für einen Zeitraum ab. Benötigt App Secret."
    )
    @ApiResponses([
        @ApiResponse(responseCode = "200", description = "Hausaufgaben erfolgreich abgerufen"),
        @ApiResponse(responseCode = "400", description = "Ungültige Anfrage"),
        @ApiResponse(responseCode = "429", description = "Rate Limit überschritten")
    ])
    @PostMapping("/v2017/homework")
    ResponseEntity<List<Map>> getHomework2017(@Valid @RequestBody Homework2017Request request) {
        log.info("Fetching homework - school: {}, dateRange: {} to {}",
            request.school, request.startDate, request.endDate)

        def startDate = LocalDate.parse(request.startDate)
        def endDate = LocalDate.parse(request.endDate)

        def homework = webUntisService.getHomework2017(
            request.school,
            request.username,
            request.password,
            request.server,
            startDate,
            endDate,
            request.appSecret
        )

        log.info("Successfully fetched {} homework entries", homework.size())
        return ResponseEntity.ok(homework)
    }

    @Operation(
        summary = "Nachrichten des Tages abrufen (2017 API)",
        description = "Ruft Nachrichten für einen bestimmten Tag ab (Standard: heute). Benötigt App Secret."
    )
    @ApiResponses([
        @ApiResponse(responseCode = "200", description = "Nachrichten erfolgreich abgerufen"),
        @ApiResponse(responseCode = "400", description = "Ungültige Anfrage"),
        @ApiResponse(responseCode = "429", description = "Rate Limit überschritten")
    ])
    @PostMapping("/v2017/messages")
    ResponseEntity<List<Map>> getMessages2017(@Valid @RequestBody Messages2017Request request) {
        def date = request.date ? LocalDate.parse(request.date) : LocalDate.now()

        log.info("Fetching messages for date: {}", date)

        def messages = webUntisService.getMessagesOfDay2017(
            request.school,
            request.username,
            request.password,
            request.server,
            date,
            request.appSecret
        )

        log.info("Successfully fetched {} messages", messages.size())
        return ResponseEntity.ok(messages)
    }

    @Operation(
        summary = "Abwesenheiten abrufen (2017 API)",
        description = "Ruft Schülerabwesenheiten für einen Zeitraum ab. Benötigt App Secret."
    )
    @ApiResponses([
        @ApiResponse(responseCode = "200", description = "Abwesenheiten erfolgreich abgerufen"),
        @ApiResponse(responseCode = "400", description = "Ungültige Anfrage"),
        @ApiResponse(responseCode = "429", description = "Rate Limit überschritten")
    ])
    @PostMapping("/v2017/absences")
    ResponseEntity<List<Map>> getAbsences2017(@Valid @RequestBody Absences2017Request request) {
        log.info("Fetching absences - school: {}, dateRange: {} to {}",
            request.school, request.startDate, request.endDate)

        def startDate = LocalDate.parse(request.startDate)
        def endDate = LocalDate.parse(request.endDate)

        def absences = webUntisService.getStudentAbsences2017(
            request.school,
            request.username,
            request.password,
            request.server,
            startDate,
            endDate,
            request.includeExcused ?: true,
            request.includeUnexcused ?: true,
            request.appSecret
        )

        log.info("Successfully fetched {} absence entries", absences.size())
        return ResponseEntity.ok(absences)
    }

    @Operation(
        summary = "Ferien abrufen (2017 API)",
        description = "Ruft Schulferien ab, optional gefiltert nach Schuljahr. Benötigt App Secret."
    )
    @ApiResponses([
        @ApiResponse(responseCode = "200", description = "Ferien erfolgreich abgerufen"),
        @ApiResponse(responseCode = "400", description = "Ungültige Anfrage"),
        @ApiResponse(responseCode = "429", description = "Rate Limit überschritten")
    ])
    @PostMapping("/v2017/holidays")
    ResponseEntity<Map<String, Object>> getHolidays2017(@Valid @RequestBody Holidays2017Request request) {
        log.info("Fetching holidays - school: {}, schoolyearId: {}", request.school, request.schoolyearId)

        def holidays = webUntisService.getHolidays2017(
            request.school,
            request.username,
            request.password,
            request.server,
            request.appSecret,
            request.schoolyearId
        )

        log.info("Successfully fetched {} holiday entries", holidays.size())

        return ResponseEntity.ok([
            status: "success",
            count: holidays.size(),
            data: holidays
        ] as Map<String, Object>)
    }

    @Operation(
        summary = "Benutzerdaten abrufen (2017 API)",
        description = "Ruft vollständige Benutzerdaten und Master-Daten ab. Benötigt App Secret."
    )
    @ApiResponses([
        @ApiResponse(responseCode = "200", description = "Benutzerdaten erfolgreich abgerufen"),
        @ApiResponse(responseCode = "400", description = "Ungültige Anfrage"),
        @ApiResponse(responseCode = "429", description = "Rate Limit überschritten")
    ])
    @PostMapping("/v2017/userdata")
    ResponseEntity<Map> getUserData2017(@Valid @RequestBody Enhanced2017Request request) {
        log.info("Fetching user data - school: {}, user: {}", request.school, request.username)

        def userData = webUntisService.getUserData2017(
            request.school,
            request.username,
            request.password,
            request.server,
            request.appSecret
        )

        log.info("Successfully fetched user data")
        return ResponseEntity.ok(userData)
    }
}
