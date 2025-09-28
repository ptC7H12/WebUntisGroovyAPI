package de.c7h12.webuntis.client

class WebUntisSession {
    String sessionId
    int personId
    String cookies
    String school
    String server
    String appSecret     // Für erweiterte Authentifizierung
    String username      // Für 2017 API
    Map masterData       // Für Master-Daten Cache

    // Standard-Konstruktor (bisherige API)
    WebUntisSession(String sessionId, int personId, String cookies, String school, String server) {
        this.sessionId = sessionId
        this.personId = personId
        this.cookies = cookies
        this.school = school
        this.server = server
        this.appSecret = null
        this.username = null
        this.masterData = null
    }

    // Erweiterte Konstruktor mit appSecret (für 2017 API)
    WebUntisSession(String sessionId, int personId, String cookies, String school, String server, String appSecret) {
        this.sessionId = sessionId
        this.personId = personId
        this.cookies = cookies
        this.school = school
        this.server = server
        this.appSecret = appSecret
        this.username = null
        this.masterData = null
    }

    // Vollständiger Konstruktor mit username (für 2017 API mit OTP)
    WebUntisSession(String sessionId, int personId, String cookies, String school, String server, String appSecret, String username) {
        this.sessionId = sessionId
        this.personId = personId
        this.cookies = cookies
        this.school = school
        this.server = server
        this.appSecret = appSecret
        this.username = username
        this.masterData = null
    }

    // Hilfsmethode um zu prüfen ob Session für 2017 API geeignet ist
    boolean is2017Compatible() {
        return appSecret != null && username != null
    }

    // Hilfsmethode um zu prüfen ob Master-Daten verfügbar sind
    boolean hasMasterData() {
        return masterData != null && !masterData.isEmpty()
    }

    // Hilfsmethode um Master-Data Cache-Alter zu prüfen
    long getMasterDataAge() {
        if (masterData?.timestamp) {
            return System.currentTimeMillis() - masterData.timestamp
        }
        return Long.MAX_VALUE // Sehr alt wenn kein timestamp
    }

    // Hilfsmethode um spezifische Master-Daten zu holen
    Map getSubjectsMap() {
        return masterData?.subjects ?: [:]
    }

    Map getTeachersMap() {
        return masterData?.teachers ?: [:]
    }

    Map getRoomsMap() {
        return masterData?.rooms ?: [:]
    }

    Map getClassesMap() {
        return masterData?.klassen ?: [:]
    }

    Map getDepartmentsMap() {
        return masterData?.departments ?: [:]
    }

    List getHolidays() {
        return masterData?.holidays ?: []
    }

    List getSchoolYears() {
        return masterData?.schoolyears ?: []
    }

    Map getTimeGrid() {
        return masterData?.timeGrid ?: [:]
    }

    // Debug-String für Logging
    @Override
    String toString() {
        def sb = new StringBuilder()
        sb.append("WebUntisSession{")
        sb.append("sessionId='").append(sessionId).append("'")
        sb.append(", personId=").append(personId)
        sb.append(", school='").append(school).append("'")
        sb.append(", server='").append(server).append("'")
        sb.append(", username='").append(username ?: "null").append("'")
        sb.append(", has2017Auth=").append(is2017Compatible())
        sb.append(", hasMasterData=").append(hasMasterData())
        if (hasMasterData()) {
            sb.append(", masterDataAge=").append(getMasterDataAge()).append("ms")
        }
        sb.append("}")
        return sb.toString()
    }
}