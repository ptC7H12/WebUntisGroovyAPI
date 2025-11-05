package de.c7h12.webuntis.service

import de.c7h12.webuntis.client.WebUntisClient
import de.c7h12.webuntis.client.WebUntisSession
import de.c7h12.webuntis.client.WebUntisException
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

    // ========== Enhanced 2017 API Methods ==========


    List<Map> getTimetable2017(String school, String username, String password, String server,
                               LocalDate startDate, LocalDate endDate, String elementType = "STUDENT", String appSecret) {
        WebUntisSession session = null
        try {
            if (!appSecret) {
                throw new WebUntisException("appSecret ist für 2017 API Methoden erforderlich")
            }

            // 2017 API mit App Secret
            session = webUntisClient.authenticateWithSecret(school, username, appSecret, server)
            println "DEBUG: Using 2017 API with app secret authentication"

            // Standard Timetable mit automatischem getUserData2017 Aufruf
            def timetable = webUntisClient.getTimetable2017(session, startDate, endDate, session.personId, elementType)

            return formatTimetableWithTimeInfo(timetable)

        } finally {
            if (session) {
                webUntisClient.logout(session)
            }
        }
    }

    List<Map> getHomework2017(String school, String username, String password, String server,
                              LocalDate startDate, LocalDate endDate, String appSecret) {
        WebUntisSession session = null
        try {
            if (!appSecret) {
                throw new WebUntisException("appSecret ist für 2017 API Methoden erforderlich")
            }

            session = webUntisClient.authenticateWithSecret(school, username, appSecret, server)

            // Homework mit automatischem getUserData2017 Aufruf
            def homework = webUntisClient.getHomework2017(session, startDate, endDate, session.personId)

            return formatHomeworkWithTimeInfo(homework)

        } finally {
            if (session) {
                webUntisClient.logout(session)
            }
        }
    }

    List<Map> getMessagesOfDay2017(String school, String username, String password, String server,
                                   LocalDate date = LocalDate.now(), String appSecret) {
        WebUntisSession session = null
        try {
            if (!appSecret) {
                throw new WebUntisException("appSecret ist für 2017 API Methoden erforderlich")
            }

            session = webUntisClient.authenticateWithSecret(school, username, appSecret, server)

            // Messages mit automatischem getUserData2017 Aufruf
            return webUntisClient.getMessagesOfDay2017(session, date)

        } finally {
            if (session) {
                webUntisClient.logout(session)
            }
        }
    }

    List<Map> getStudentAbsences2017(String school, String username, String password, String server,
                                     LocalDate startDate, LocalDate endDate, boolean includeExcused = true,
                                     boolean includeUnexcused = true, String appSecret) {
        WebUntisSession session = null
        try {
            if (!appSecret) {
                throw new WebUntisException("appSecret ist für 2017 API Methoden erforderlich")
            }

            session = webUntisClient.authenticateWithSecret(school, username, appSecret, server)

            // Absences mit automatischem getUserData2017 Aufruf
            def absences = webUntisClient.getStudentAbsences2017(session, startDate, endDate, includeExcused, includeUnexcused)

            return formatAbsencesWithTimeInfo(absences)

        } finally {
            if (session) {
                webUntisClient.logout(session)
            }
        }
    }

    Map getUserData2017(String school, String username, String password, String server, String appSecret) {
        WebUntisSession session = null
        try {
            if (!appSecret) {
                throw new WebUntisException("appSecret ist für 2017 API Methoden erforderlich")
            }

            session = webUntisClient.authenticateWithSecret(school, username, appSecret, server)

            // Expliziter getUserData2017 Aufruf
            return webUntisClient.getUserData2017(session)

        } finally {
            if (session) {
                webUntisClient.logout(session)
            }
        }
    }

    List<Map> getHolidays2017(String school, String username, String password, String server,
                              String appSecret, Integer schoolyearId = null) {
        WebUntisSession session = null
        try {
            if (!appSecret) {
                throw new WebUntisException("appSecret ist für 2017 API Methoden erforderlich")
            }

            session = webUntisClient.authenticateWithSecret(school, username, appSecret, server)

            // Holidays aus Master-Daten holen
            def holidays = session.getHolidays()
            def schoolyears = session.getSchoolYears()
            def today = LocalDate.now()

            if (schoolyearId == null) {
                // Standard: Ab heute bis Ende des nächsten Kalenderjahres
                def currentYear = today.getYear()
                def nextYear = currentYear + 1
                def endOfNextYear = LocalDate.of(nextYear, 12, 31)

                holidays = holidays.findAll { holiday ->
                    def holidayStart = LocalDate.parse(holiday.startDate as String)
                    def holidayEnd = LocalDate.parse(holiday.endDate as String)

                    // Ferien noch nicht komplett vorbei UND starten vor Ende des nächsten Jahres
                    return !holidayEnd.isBefore(today) && !holidayStart.isAfter(endOfNextYear)
                }

                println "DEBUG: Ferien von heute (${today}) bis Ende ${nextYear} (${endOfNextYear}): ${holidays.size()}"

            } else {
                // Spezifisches Schuljahr (optional weiterhin verfügbar)
                def schoolyear = schoolyears.find { it.id == schoolyearId }
                if (schoolyear) {
                    def startDate = LocalDate.parse(schoolyear.startDate as String)
                    def endDate = LocalDate.parse(schoolyear.endDate as String)

                    holidays = holidays.findAll { holiday ->
                        def holidayStart = LocalDate.parse(holiday.startDate as String)
                        def holidayEnd = LocalDate.parse(holiday.endDate as String)

                        // Im angegebenen Schuljahr UND noch nicht vorbei
                        return !(holidayEnd.isBefore(startDate) || holidayStart.isAfter(endDate))
                                && !holidayEnd.isBefore(today)
                    }

                    println "DEBUG: Ferien für Schuljahr ${schoolyear.name} (ab heute): ${holidays.size()}"
                }
            }

            return formatHolidaysWithTimeInfo(holidays, schoolyears)

        } finally {
            if (session) {
                webUntisClient.logout(session)
            }
        }
    }

    // ========== Formatting Methods ==========

    private List<Map> formatHolidaysWithTimeInfo(List<Map> holidays, List<Map> schoolyears) {
        return holidays.collect { holiday ->
            def startDate = LocalDate.parse(holiday.startDate as String)
            def endDate = LocalDate.parse(holiday.endDate as String)

            // Dauer berechnen
            def durationDays = java.time.temporal.ChronoUnit.DAYS.between(startDate, endDate) + 1

            // Status bestimmen
            def today = LocalDate.now()
            def status
            def statusDescription

            if (endDate.isBefore(today)) {
                // Sollte nicht mehr vorkommen durch Filterung
                status = "past"
                statusDescription = "Vergangen"
            } else if (startDate.isAfter(today)) {
                status = "upcoming"
                statusDescription = "Bevorstehend"
            } else {
                // Heute liegt zwischen Start und Ende
                status = "current"
                statusDescription = "Aktuell laufend"
            }

            // Tage bis Start/Ende
            def daysUntilStart = java.time.temporal.ChronoUnit.DAYS.between(today, startDate)
            def daysUntilEnd = java.time.temporal.ChronoUnit.DAYS.between(today, endDate)

            // Schuljahr ermitteln
            def schoolyear = schoolyears.find { sy ->
                def syStart = LocalDate.parse(sy.startDate as String)
                def syEnd = LocalDate.parse(sy.endDate as String)

                return !(endDate.isBefore(syStart) || startDate.isAfter(syEnd))
            }

            // Ferientyp erkennen
            def holidayType = categorizeHoliday(holiday.name as String, holiday.longName as String)

            return [
                    id: holiday.id,
                    name: holiday.name,
                    longName: holiday.longName,
                    startDate: holiday.startDate,
                    endDate: holiday.endDate,
                    durationDays: durationDays,
                    status: status,
                    statusDescription: statusDescription,
                    daysUntilStart: daysUntilStart > 0 ? daysUntilStart : null,
                    daysUntilEnd: daysUntilEnd >= 0 ? daysUntilEnd : null,
                    startWeekday: getWeekdayName(holiday.startDate as String),
                    endWeekday: getWeekdayName(holiday.endDate as String),
                    schoolyear: schoolyear?.name,
                    schoolyearId: schoolyear?.id,
                    holidayType: holidayType,
                    isLongWeekend: durationDays <= 3,
                    isWeekVacation: durationDays > 3 && durationDays <= 7,
                    isMultiWeekVacation: durationDays > 7
            ]
        }.sort { a, b ->
            // Chronologisch sortieren (früheste zuerst)
            LocalDate.parse(a.startDate as String) <=> LocalDate.parse(b.startDate as String)
        }
    }

    private String categorizeHoliday(String name, String longName) {
        def nameUpper = name.toUpperCase()
        def longNameUpper = longName.toUpperCase()

        // Hauptferien
        if (nameUpper.contains("HERBST") || longNameUpper.contains("HERBST")) {
            return "Herbstferien"
        }
        if (nameUpper.contains("WEIHNACHT") || longNameUpper.contains("WEIHNACHT")) {
            return "Weihnachtsferien"
        }
        if (nameUpper.contains("OSTER") || longNameUpper.contains("OSTER")) {
            return "Osterferien"
        }
        if (nameUpper.contains("PFINGST") || longNameUpper.contains("PFINGST")) {
            return "Pfingstferien"
        }
        if (nameUpper.contains("SOMMER") || longNameUpper.contains("SOMMER")) {
            return "Sommerferien"
        }

        // Feiertage
        if (nameUpper.contains("TAG DER ARBEIT") || nameUpper == "1.5.") {
            return "Feiertag - Tag der Arbeit"
        }
        if (nameUpper.contains("EINHEIT") || nameUpper == "3.10.") {
            return "Feiertag - Tag der Deutschen Einheit"
        }
        if (nameUpper.contains("ALLERHEILIGEN") || nameUpper == "1.11.") {
            return "Feiertag - Allerheiligen"
        }
        if (nameUpper.contains("HIMMELFAHRT") || longNameUpper.contains("HIMMELFAHRT")) {
            return "Feiertag - Christi Himmelfahrt"
        }
        if (nameUpper.contains("FRONLEICHNAM") || longNameUpper.contains("FRONLEICHNAM")) {
            return "Feiertag - Fronleichnam"
        }
        if (nameUpper.contains("PFINGSTMONTAG") || longNameUpper.contains("PFINGSTMONTAG")) {
            return "Feiertag - Pfingstmontag"
        }

        // Einzelne freie Tage
        if (longNameUpper.contains("ELTERNSPRECHTAG")) {
            return "Schulveranstaltung - Elternsprechtag"
        }
        if (longNameUpper.contains("ZEUGNIS")) {
            return "Schulveranstaltung - Zeugnisausgabe"
        }

        return "Sonstiger Ferientag"
    }



    // Formatierung für 2017 API mit Zeitinfos
    private List<Map> formatTimetableWithTimeInfo(List<Map> timetable) {
        return timetable.collect { entry ->
            // Datum und Zeiten formatieren
            def dateFormatted = formatWebUntisDate(entry.date as Integer)
            def startTimeFormatted = formatWebUntisTime(entry.startTime as Integer)
            def endTimeFormatted = formatWebUntisTime(entry.endTime as Integer)
            def duration = calculateDuration(entry.startTime as Integer, entry.endTime as Integer)

            // Lesson Code interpretieren
            def lessonStatus = interpretLessonCode(entry.code as String)

            // Formatierte Zeiten hinzufügen
            entry.dateFormatted = dateFormatted
            entry.startTimeFormatted = startTimeFormatted
            entry.endTimeFormatted = endTimeFormatted
            entry.timeRange = "${startTimeFormatted} - ${endTimeFormatted}"
            entry.durationMinutes = duration
            entry.weekday = getWeekdayName(dateFormatted)
            entry.lessonStatus = lessonStatus

            // Vertretungsinfo generieren falls Original-Daten vorhanden
            entry.substitutionInfo = generate2017SubstitutionInfo(entry)

            return entry
        }
    }

    private List<Map> formatHomeworkWithTimeInfo(List<Map> homework) {
        return homework.collect { hw ->
            // Datum formatieren
            if (hw.date) {
                hw.dateFormatted = formatWebUntisDate(hw.date as Integer)
            }
            if (hw.dueDate) {
                hw.dueDateFormatted = formatWebUntisDate(hw.dueDate as Integer)
            }

            // Status bestimmen
            if (hw.dueDate) {
                def dueDate = LocalDate.parse(formatWebUntisDate(hw.dueDate as Integer))
                def today = LocalDate.now()

                if (hw.completed) {
                    hw.status = "completed"
                    hw.statusDescription = "Erledigt"
                } else if (dueDate.isBefore(today)) {
                    hw.status = "overdue"
                    hw.statusDescription = "Überfällig"
                } else if (dueDate.isEqual(today)) {
                    hw.status = "due_today"
                    hw.statusDescription = "Heute fällig"
                } else {
                    hw.status = "pending"
                    hw.statusDescription = "Ausstehend"
                }
            }

            return hw
        }
    }

    private List<Map> formatAbsencesWithTimeInfo(List<Map> absences) {
        return absences.collect { absence ->
            // Datum formatieren
            if (absence.startDate) {
                absence.startDateFormatted = formatWebUntisDate(absence.startDate as Integer)
            }
            if (absence.endDate) {
                absence.endDateFormatted = formatWebUntisDate(absence.endDate as Integer)
            }

            // Zeit formatieren
            if (absence.startTime) {
                absence.startTimeFormatted = formatWebUntisTime(absence.startTime as Integer)
            }
            if (absence.endTime) {
                absence.endTimeFormatted = formatWebUntisTime(absence.endTime as Integer)
            }

            // Zeitbereich
            if (absence.startTimeFormatted && absence.endTimeFormatted) {
                absence.timeRange = "${absence.startTimeFormatted} - ${absence.endTimeFormatted}"
            }

            // Status-Info
            absence.statusDescription = absence.excused ? "Entschuldigt" : "Unentschuldigt"

            return absence
        }
    }

    // Erweiterte Vertretungsinfo-Generierung für 2017 API
    private String generate2017SubstitutionInfo(Map entry) {
        def infoTexts = []

        // Vertretungslehrer
        if (!entry.originalTeachers.isEmpty() && !entry.teachers.isEmpty()) {
            def originalTeacher = entry.originalTeachers[0]
            def currentTeacher = entry.teachers[0]
            if (originalTeacher.id != currentTeacher.id) {
                def originalName = originalTeacher.firstName ? "${originalTeacher.firstName} ${originalTeacher.lastName}".trim() : originalTeacher.name
                def currentName = currentTeacher.firstName ? "${currentTeacher.firstName} ${currentTeacher.lastName}".trim() : currentTeacher.name
                infoTexts << "Lehrer: ${originalName} → ${currentName}"
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

        // Zusätzliche 2017 API Infos
        if (entry.substText) {
            infoTexts << "Vertretung: ${entry.substText}"
        }

        if (entry.info) {
            infoTexts << "Info: ${entry.info}"
        }

        if (entry.lsText) {
            infoTexts << "Unterricht: ${entry.lsText}"
        }

        return infoTexts.join(" | ") ?: null
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