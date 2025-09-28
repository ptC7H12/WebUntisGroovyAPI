package de.c7h12.webuntis.controller

import de.c7h12.webuntis.service.WebUntisService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

import java.time.LocalDate

@RestController
@RequestMapping("/api/webuntis")
@CrossOrigin(origins = "*")
class WebUntisController {

    @Autowired
    WebUntisService webUntisService

    @PostMapping("/timetable/today")
    ResponseEntity<?> getTimetableToday(@RequestBody Map request) {
        try {
            def timetable = webUntisService.getTimetableToday(
                    request.school as String,
                    request.username as String,
                    request.password as String,
                    request.server as String
            )
            return ResponseEntity.ok(timetable)
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body([error: "Fehler beim Abrufen des Stundenplans: ${e.message}"])
        }
    }

    @PostMapping("/timetable/week")
    ResponseEntity<?> getTimetableWeek(@RequestBody Map request) {
        try {
            def timetable = webUntisService.getTimetableCurrentWeek(
                    request.school as String,
                    request.username as String,
                    request.password as String,
                    request.server as String
            )
            return ResponseEntity.ok(timetable)
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body([error: "Fehler beim Abrufen des Wochenstundenplans: ${e.message}"])
        }
    }

    @PostMapping("/timetable/range")
    ResponseEntity<?> getTimetableRange(@RequestBody Map request) {
        try {
            def startDate = LocalDate.parse(request.startDate as String)
            def endDate = LocalDate.parse(request.endDate as String)

            def timetable = webUntisService.getTimetableForDateRange(
                    request.school as String,
                    request.username as String,
                    request.password as String,
                    request.server as String,
                    startDate,
                    endDate
            )
            return ResponseEntity.ok(timetable)
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body([error: "Fehler beim Abrufen des Stundenplans für Zeitraum: ${e.message}"])
        }
    }

    @PostMapping("/subjects")
    ResponseEntity<?> getSubjects(@RequestBody Map request) {
        try {
            def subjects = webUntisService.getSubjects(
                    request.school as String,
                    request.username as String,
                    request.password as String,
                    request.server as String
            )
            return ResponseEntity.ok(subjects)
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body([error: "Fehler beim Abrufen der Fächer: ${e.message}"])
        }
    }

    @PostMapping("/teachers")
    ResponseEntity<?> getTeachers(@RequestBody Map request) {
        try {
            def teachers = webUntisService.getTeachers(
                    request.school as String,
                    request.username as String,
                    request.password as String,
                    request.server as String
            )
            return ResponseEntity.ok(teachers)
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body([error: "Fehler beim Abrufen der Lehrer: ${e.message}"])
        }
    }

    @PostMapping("/rooms")
    ResponseEntity<?> getRooms(@RequestBody Map request) {
        try {
            def rooms = webUntisService.getRooms(
                    request.school as String,
                    request.username as String,
                    request.password as String,
                    request.server as String
            )
            return ResponseEntity.ok(rooms)
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body([error: "Fehler beim Abrufen der Räume: ${e.message}"])
        }
    }

    // ========== Enhanced 2017 API Endpoints (Alle benötigen appSecret) ==========

    @PostMapping("/v2017/timetable/enhanced")
    ResponseEntity<?> getTimetableEnhanced2017(@RequestBody Map request) {
        try {
            def startDate = LocalDate.parse(request.startDate as String)
            def endDate = LocalDate.parse(request.endDate as String)
            def elementType = request.elementType as String ?: "STUDENT"
            def appSecret = request.appSecret as String

            if (!appSecret) {
                return ResponseEntity.badRequest()
                        .body([error: "appSecret ist für 2017 API Methoden erforderlich"])
            }

            def timetable = webUntisService.getTimetable2017Enhanced(
                    request.school as String,
                    request.username as String,
                    request.password as String,
                    request.server as String,
                    startDate,
                    endDate,
                    elementType,
                    appSecret
            )
            return ResponseEntity.ok([
                    status: "success",
                    format: "2017-enhanced",
                    dataCount: timetable.size(),
                    data: timetable
            ])
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body([error: "Fehler beim Abrufen des erweiterten 2017 Stundenplans: ${e.message}"])
        }
    }

    @PostMapping("/v2017/timetable")
    ResponseEntity<?> getTimetableEnhanced(@RequestBody Map request) {
        try {
            def startDate = LocalDate.parse(request.startDate as String)
            def endDate = LocalDate.parse(request.endDate as String)
            def elementType = request.elementType as String ?: "STUDENT"
            def appSecret = request.appSecret as String

            if (!appSecret) {
                return ResponseEntity.badRequest()
                        .body([error: "appSecret ist für 2017 API Methoden erforderlich"])
            }

            def timetable = webUntisService.getTimetable2017(
                    request.school as String,
                    request.username as String,
                    request.password as String,
                    request.server as String,
                    startDate,
                    endDate,
                    elementType,
                    appSecret
            )
            return ResponseEntity.ok([
                    status: "success",
                    format: "2017-standard",
                    dataCount: timetable.size(),
                    data: timetable
            ])
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body([error: "Fehler beim Abrufen des erweiterten Stundenplans: ${e.message}"])
        }
    }

    @PostMapping("/v2017/homework")
    ResponseEntity<?> getHomework(@RequestBody Map request) {
        try {
            def startDate = LocalDate.parse(request.startDate as String)
            def endDate = LocalDate.parse(request.endDate as String)
            def appSecret = request.appSecret as String

            if (!appSecret) {
                return ResponseEntity.badRequest()
                        .body([error: "appSecret ist für 2017 API Methoden erforderlich"])
            }

            def homework = webUntisService.getHomework2017(
                    request.school as String,
                    request.username as String,
                    request.password as String,
                    request.server as String,
                    startDate,
                    endDate,
                    appSecret
            )
            return ResponseEntity.ok(homework)
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body([error: "Fehler beim Abrufen der Hausaufgaben: ${e.message}"])
        }
    }

    @PostMapping("/v2017/messages")
    ResponseEntity<?> getMessages(@RequestBody Map request) {
        try {
            def date = request.date ? LocalDate.parse(request.date as String) : LocalDate.now()
            def appSecret = request.appSecret as String

            if (!appSecret) {
                return ResponseEntity.badRequest()
                        .body([error: "appSecret ist für 2017 API Methoden erforderlich"])
            }

            def messages = webUntisService.getMessagesOfDay2017(
                    request.school as String,
                    request.username as String,
                    request.password as String,
                    request.server as String,
                    date,
                    appSecret
            )
            return ResponseEntity.ok(messages)
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body([error: "Fehler beim Abrufen der Nachrichten: ${e.message}"])
        }
    }

    @PostMapping("/v2017/absences")
    ResponseEntity<?> getAbsences(@RequestBody Map request) {
        try {
            def startDate = LocalDate.parse(request.startDate as String)
            def endDate = LocalDate.parse(request.endDate as String)
            def includeExcused = request.includeExcused != null ? request.includeExcused as boolean : true
            def includeUnexcused = request.includeUnexcused != null ? request.includeUnexcused as boolean : true
            def appSecret = request.appSecret as String

            if (!appSecret) {
                return ResponseEntity.badRequest()
                        .body([error: "appSecret ist für 2017 API Methoden erforderlich"])
            }

            def absences = webUntisService.getStudentAbsences2017(
                    request.school as String,
                    request.username as String,
                    request.password as String,
                    request.server as String,
                    startDate,
                    endDate,
                    includeExcused,
                    includeUnexcused,
                    appSecret
            )
            return ResponseEntity.ok(absences)
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body([error: "Fehler beim Abrufen der Abwesenheiten: ${e.message}"])
        }
    }

    @PostMapping("/v2017/userdata")
    ResponseEntity<?> getUserData(@RequestBody Map request) {
        try {
            def appSecret = request.appSecret as String

            if (!appSecret) {
                return ResponseEntity.badRequest()
                        .body([error: "appSecret ist für 2017 API Methoden erforderlich"])
            }

            def userData = webUntisService.getUserData2017(
                    request.school as String,
                    request.username as String,
                    request.password as String,
                    request.server as String,
                    appSecret
            )
            return ResponseEntity.ok(userData)
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body([error: "Fehler beim Abrufen der Benutzerdaten: ${e.message}"])
        }
    }
}