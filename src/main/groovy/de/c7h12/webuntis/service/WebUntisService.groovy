package de.c7h12.webuntis.service

import de.c7h12.webuntis.client.WebUntisClient
import de.c7h12.webuntis.client.WebUntisSession
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

import java.time.LocalDate

@Service
class WebUntisService {

    @Autowired
    WebUntisClient webUntisClient

    List<Map> getTimetableToday(String school, String username, String password, String server) {
        WebUntisSession session = null
        try {
            session = webUntisClient.authenticate(school, username, password, server)
            def today = LocalDate.now()

            // Stammdaten laden (falls verfügbar)
            def subjects = safeGetSubjects(session)
            def teachers = safeGetTeachers(session)
            def rooms = safeGetRooms(session)
            def classes = safeGetClasses(session)

            // Dann Stundenplan mit erweiterten Informationen
            def timetable = webUntisClient.getTimetable(session, today, today, session.personId, 5)

            return resolveAdvancedTimetableNames(timetable, subjects, teachers, rooms, classes)

        } finally {
            if (session) {
                webUntisClient.logout(session)
            }
        }
    }

    List<Map> getTimetableCurrentWeek(String school, String username, String password, String server) {
        WebUntisSession session = null
        try {
            session = webUntisClient.authenticate(school, username, password, server)

            def today = LocalDate.now()
            def monday = today.minusDays(today.dayOfWeek.value - 1)
            def friday = monday.plusDays(4)

            // Stammdaten laden (falls verfügbar)
            def subjects = safeGetSubjects(session)
            def teachers = safeGetTeachers(session)
            def rooms = safeGetRooms(session)
            def classes = safeGetClasses(session)

            def timetable = webUntisClient.getTimetable(session, monday, friday, session.personId, 5)

            return resolveAdvancedTimetableNames(timetable, subjects, teachers, rooms, classes)

        } finally {
            if (session) {
                webUntisClient.logout(session)
            }
        }
    }

    List<Map> getTimetableForDateRange(String school, String username, String password, String server, LocalDate startDate, LocalDate endDate) {
        WebUntisSession session = null
        try {
            session = webUntisClient.authenticate(school, username, password, server)

            // Stammdaten laden (falls verfügbar)
            def subjects = safeGetSubjects(session)
            def teachers = safeGetTeachers(session)
            def rooms = safeGetRooms(session)
            def classes = safeGetClasses(session)

            def timetable = webUntisClient.getTimetable(session, startDate, endDate, session.personId, 5)

            return resolveAdvancedTimetableNames(timetable, subjects, teachers, rooms, classes)

        } finally {
            if (session) {
                webUntisClient.logout(session)
            }
        }
    }

    // Sichere Methoden die bei fehlenden Berechtigungen leere Listen zurückgeben
    private List<Map> safeGetSubjects(WebUntisSession session) {
        try {
            return webUntisClient.getSubjects(session)
        } catch (Exception e) {
            println "INFO: Cannot load subjects (no permission): ${e.message}"
            return []
        }
    }

    private List<Map> safeGetTeachers(WebUntisSession session) {
        try {
            return webUntisClient.getTeachers(session)
        } catch (Exception e) {
            println "INFO: Cannot load teachers (no permission): ${e.message}"
            return []
        }
    }

    private List<Map> safeGetRooms(WebUntisSession session) {
        try {
            return webUntisClient.getRooms(session)
        } catch (Exception e) {
            println "INFO: Cannot load rooms (no permission): ${e.message}"
            return []
        }
    }

    private List<Map> safeGetClasses(WebUntisSession session) {
        try {
            return webUntisClient.getClasses(session)
        } catch (Exception e) {
            println "INFO: Cannot load classes (no permission): ${e.message}"
            return []
        }
    }

    // Hilfsmethode zum Auflösen der IDs zu Namen
    private List<Map> resolveTimetableNames(List<Map> timetable, List<Map> subjects, List<Map> teachers, List<Map> rooms, List<Map> classes) {
        def subjectMap = subjects.collectEntries { [(it.id): it] }
        def teacherMap = teachers.collectEntries { [(it.id): it] }
        def roomMap = rooms.collectEntries { [(it.id): it] }
        def classMap = classes.collectEntries { [(it.id): it] }

        return timetable.collect { entry ->
            // Datum und Zeiten formatieren
            def dateFormatted = formatWebUntisDate(entry.date as Integer)
            def startTimeFormatted = formatWebUntisTime(entry.startTime as Integer)
            def endTimeFormatted = formatWebUntisTime(entry.endTime as Integer)
            def duration = calculateDuration(entry.startTime as Integer, entry.endTime as Integer)

            // Subjects auflösen
            entry.subjects = entry.subjects.collect { su ->
                def subject = subjectMap[su.id]
                return [
                        id: su.id,
                        name: subject?.name ?: "Unbekannt",
                        longName: subject?.longName ?: subject?.name ?: "Unbekannt"
                ]
            }

            // Teachers auflösen (falls verfügbar)
            entry.teachers = entry.teachers.collect { te ->
                def teacher = teacherMap[te.id]
                return [
                        id: te.id,
                        name: teacher?.name ?: "Lehrer-${te.id}",
                        foreName: teacher?.foreName ?: "",
                        longName: teacher?.longName ?: teacher?.name ?: "Lehrer-${te.id}"
                ]
            }

            // Rooms auflösen (falls verfügbar)
            entry.rooms = entry.rooms.collect { ro ->
                def room = roomMap[ro.id]
                return [
                        id: ro.id,
                        name: room?.name ?: "Raum-${ro.id}",
                        longName: room?.longName ?: room?.name ?: "Raum-${ro.id}"
                ]
            }

            // Classes auflösen (falls verfügbar)
            entry.classes = entry.classes.collect { kl ->
                def clazz = classMap[kl.id]
                return [
                        id: kl.id,
                        name: clazz?.name ?: "Klasse-${kl.id}",
                        longName: clazz?.longName ?: clazz?.name ?: "Klasse-${kl.id}"
                ]
            }

            // Formatierte Zeiten hinzufügen (behalte auch die Originalwerte)
            entry.dateFormatted = dateFormatted
            entry.startTimeFormatted = startTimeFormatted
            entry.endTimeFormatted = endTimeFormatted
            entry.timeRange = "${startTimeFormatted} - ${endTimeFormatted}"
            entry.durationMinutes = duration
            entry.weekday = getWeekdayName(dateFormatted)

            return entry
        }
    }



    List<Map> getSubjects(String school, String username, String password, String server) {
        WebUntisSession session = null
        try {
            session = webUntisClient.authenticate(school, username, password, server)
            return webUntisClient.getSubjects(session)

        } finally {
            if (session) {
                webUntisClient.logout(session)
            }
        }
    }

    List<Map> getTeachers(String school, String username, String password, String server) {
        WebUntisSession session = null
        try {
            session = webUntisClient.authenticate(school, username, password, server)
            return webUntisClient.getTeachers(session)

        } finally {
            if (session) {
                webUntisClient.logout(session)
            }
        }
    }

    List<Map> getRooms(String school, String username, String password, String server) {
        WebUntisSession session = null
        try {
            session = webUntisClient.authenticate(school, username, password, server)
            return webUntisClient.getRooms(session)

        } finally {
            if (session) {
                webUntisClient.logout(session)
            }
        }
    }

    // ========== Enhanced 2017 API Methods (Optional) ==========

    List<Map> getTimetable2017(String school, String username, String password, String server, LocalDate startDate, LocalDate endDate, String elementType = "STUDENT") {
        WebUntisSession session = null
        try {
            session = webUntisClient.authenticate(school, username, password, server)

            return webUntisClient.getTimetable2017(session, startDate, endDate, session.personId, elementType)

        } finally {
            if (session) {
                webUntisClient.logout(session)
            }
        }
    }

    List<Map> getHomework2017(String school, String username, String password, String server, LocalDate startDate, LocalDate endDate) {
        WebUntisSession session = null
        try {
            session = webUntisClient.authenticate(school, username, password, server)

            return webUntisClient.getHomework2017(session, startDate, endDate, session.personId)

        } finally {
            if (session) {
                webUntisClient.logout(session)
            }
        }
    }

    List<Map> getMessagesOfDay2017(String school, String username, String password, String server, LocalDate date = LocalDate.now()) {
        WebUntisSession session = null
        try {
            session = webUntisClient.authenticate(school, username, password, server)

            return webUntisClient.getMessagesOfDay2017(session, date)

        } finally {
            if (session) {
                webUntisClient.logout(session)
            }
        }
    }

    List<Map> getStudentAbsences2017(String school, String username, String password, String server, LocalDate startDate, LocalDate endDate, boolean includeExcused = true, boolean includeUnexcused = true) {
        WebUntisSession session = null
        try {
            session = webUntisClient.authenticate(school, username, password, server)

            return webUntisClient.getStudentAbsences2017(session, startDate, endDate, includeExcused, includeUnexcused)

        } finally {
            if (session) {
                webUntisClient.logout(session)
            }
        }
    }

    Map getUserData2017(String school, String username, String password, String server) {
        WebUntisSession session = null
        try {
            session = webUntisClient.authenticate(school, username, password, server)

            return webUntisClient.getUserData2017(session)

        } finally {
            if (session) {
                webUntisClient.logout(session)
            }
        }
    }

    // Erweiterte Hilfsmethode zum Auflösen der IDs zu Namen mit Vertretungsinfos
    private List<Map> resolveAdvancedTimetableNames(List<Map> timetable, List<Map> subjects, List<Map> teachers, List<Map> rooms, List<Map> classes) {
        def subjectMap = subjects.collectEntries { [(it.id): it] }
        def teacherMap = teachers.collectEntries { [(it.id): it] }
        def roomMap = rooms.collectEntries { [(it.id): it] }
        def classMap = classes.collectEntries { [(it.id): it] }

        return timetable.collect { entry ->
            // Datum und Zeiten formatieren
            def dateFormatted = formatWebUntisDate(entry.date as Integer)
            def startTimeFormatted = formatWebUntisTime(entry.startTime as Integer)
            def endTimeFormatted = formatWebUntisTime(entry.endTime as Integer)
            def duration = calculateDuration(entry.startTime as Integer, entry.endTime as Integer)

            // Lesson Code interpretieren
            def lessonStatus = interpretLessonCode(entry.code as String)

            // Subjects auflösen (aktuelle)
            entry.subjects = entry.subjects.collect { su ->
                def subject = subjectMap[su.id]
                return [
                        id: su.id,
                        name: subject?.name ?: "Fach-${su.id}",
                        longName: subject?.longName ?: subject?.name ?: "Fach-${su.id}"
                ]
            }

            // Original-Subjects auflösen (bei Vertretungen)
            if (entry.originalSubjects == null) entry.originalSubjects = []
            entry.originalSubjects = entry.originalSubjects.collect { su ->
                def subject = subjectMap[su.id]
                return [
                        id: su.id,
                        name: subject?.name ?: "Fach-${su.id}",
                        longName: subject?.longName ?: subject?.name ?: "Fach-${su.id}"
                ]
            }

            // Teachers auflösen (aktuelle)
            entry.teachers = entry.teachers.collect { te ->
                def teacher = teacherMap[te.id]
                return [
                        id: te.id,
                        name: teacher?.name ?: "Lehrer-${te.id}",
                        foreName: teacher?.foreName ?: "",
                        longName: teacher?.longName ?: teacher?.name ?: "Lehrer-${te.id}"
                ]
            }

            // Original-Teachers auflösen (bei Vertretungen)
            if (entry.originalTeachers == null) entry.originalTeachers = []
            entry.originalTeachers = entry.originalTeachers.collect { te ->
                def teacher = teacherMap[te.id]
                return [
                        id: te.id,
                        name: teacher?.name ?: "Lehrer-${te.id}",
                        foreName: teacher?.foreName ?: "",
                        longName: teacher?.longName ?: teacher?.name ?: "Lehrer-${te.id}"
                ]
            }

            // Rooms auflösen (aktuelle)
            entry.rooms = entry.rooms.collect { ro ->
                def room = roomMap[ro.id]
                return [
                        id: ro.id,
                        name: room?.name ?: "Raum-${ro.id}",
                        longName: room?.longName ?: room?.name ?: "Raum-${ro.id}"
                ]
            }

            // Original-Rooms auflösen (bei Vertretungen)
            if (entry.originalRooms == null) entry.originalRooms = []
            entry.originalRooms = entry.originalRooms.collect { ro ->
                def room = roomMap[ro.id]
                return [
                        id: ro.id,
                        name: room?.name ?: "Raum-${ro.id}",
                        longName: room?.longName ?: room?.name ?: "Raum-${ro.id}"
                ]
            }

            // Classes auflösen (aktuelle)
            entry.classes = entry.classes.collect { kl ->
                def clazz = classMap[kl.id]
                return [
                        id: kl.id,
                        name: clazz?.name ?: "Klasse-${kl.id}",
                        longName: clazz?.longName ?: clazz?.name ?: "Klasse-${kl.id}"
                ]
            }

            // Original-Classes auflösen (bei Vertretungen)
            if (entry.originalClasses == null) entry.originalClasses = []
            entry.originalClasses = entry.originalClasses.collect { kl ->
                def clazz = classMap[kl.id]
                return [
                        id: kl.id,
                        name: clazz?.name ?: "Klasse-${kl.id}",
                        longName: clazz?.longName ?: clazz?.name ?: "Klasse-${kl.id}"
                ]
            }

            // Formatierte Zeiten hinzufügen
            entry.dateFormatted = dateFormatted
            entry.startTimeFormatted = startTimeFormatted
            entry.endTimeFormatted = endTimeFormatted
            entry.timeRange = "${startTimeFormatted} - ${endTimeFormatted}"
            entry.durationMinutes = duration
            entry.weekday = getWeekdayName(dateFormatted)
            entry.lessonStatus = lessonStatus

            // Vertretungsinfo generieren
            entry.substitutionInfo = generateSubstitutionInfo(entry)

            return entry
        }
    }

    // Hilfsmethode zum Interpretieren des Lesson Codes
    private Map interpretLessonCode(String code) {
        def codeMap = [
                'REGULAR': [status: 'normal', description: 'Regulärer Unterricht', color: 'green'],
                'CANCELLED': [status: 'cancelled', description: 'Stunde entfällt', color: 'red'],
                'IRREGULAR': [status: 'irregular', description: 'Unregelmäßiger Unterricht', color: 'orange'],
                'SUBSTITUTION': [status: 'substitution', description: 'Vertretung', color: 'blue'],
                'ROOMSUBSTITUTION': [status: 'roomSubstitution', description: 'Raumvertretung', color: 'yellow'],
                'EXAM': [status: 'exam', description: 'Prüfung', color: 'purple']
        ]

        return codeMap[code] ?: [status: 'unknown', description: code ?: 'Unbekannt', color: 'gray']
    }

    // Hilfsmethode zum Generieren von Vertretungsinfos
    private String generateSubstitutionInfo(Map entry) {
        def infoTexts = []

        // Vertretungslehrer
        if (!entry.originalTeachers.isEmpty() && !entry.teachers.isEmpty()) {
            def originalTeacher = entry.originalTeachers[0]
            def currentTeacher = entry.teachers[0]
            if (originalTeacher.id != currentTeacher.id) {
                infoTexts << "Lehrer: ${originalTeacher.name} → ${currentTeacher.name}"
            }
        }

        // Raumänderung
        if (!entry.originalRooms.isEmpty() && !entry.rooms.isEmpty()) {
            def originalRoom = entry.originalRooms[0]
            def currentRoom = entry.rooms[0]
            if (originalRoom.id != currentRoom.id) {
                infoTexts << "Raum: ${originalRoom.name} → ${currentRoom.name}"
            }
        }

        // Fachänderung
        if (!entry.originalSubjects.isEmpty() && !entry.subjects.isEmpty()) {
            def originalSubject = entry.originalSubjects[0]
            def currentSubject = entry.subjects[0]
            if (originalSubject.id != currentSubject.id) {
                infoTexts << "Fach: ${originalSubject.name} → ${currentSubject.name}"
            }
        }

        // Zusätzliche Infos
        if (entry.substText) {
            infoTexts << "Info: ${entry.substText}"
        }

        return infoTexts.join(" | ") ?: null
    }

    // Hilfsmethoden für Zeitformatierung
    private String formatWebUntisDate(Integer webUntisDate) {
        // Format: 20250925 -> 2025-09-25
        def dateStr = webUntisDate.toString()
        def year = dateStr.substring(0, 4)
        def month = dateStr.substring(4, 6)
        def day = dateStr.substring(6, 8)
        return "${year}-${month}-${day}"
    }

    private String formatWebUntisTime(Integer webUntisTime) {
        // Format: 1340 -> 13:40, 800 -> 08:00
        def timeStr = String.format("%04d", webUntisTime)
        def hours = timeStr.substring(0, 2)
        def minutes = timeStr.substring(2, 4)
        return "${hours}:${minutes}"
    }

    private Integer calculateDuration(Integer startTime, Integer endTime) {
        def startHours = Math.floor(startTime / 100)
        def startMinutes = startTime % 100
        def endHours = Math.floor(endTime / 100)
        def endMinutes = endTime % 100

        def totalStartMinutes = startHours * 60 + startMinutes
        def totalEndMinutes = endHours * 60 + endMinutes

        return totalEndMinutes - totalStartMinutes
    }

    private String getWeekdayName(String dateStr) {
        try {
            def date = LocalDate.parse(dateStr)
            def weekdays = ['Montag', 'Dienstag', 'Mittwoch', 'Donnerstag', 'Freitag', 'Samstag', 'Sonntag']
            return weekdays[date.dayOfWeek.value - 1]
        } catch (Exception e) {
            return "Unbekannt"
        }
    }
}