package de.c7h12.webuntis.client

class WebUntisSession {
    String sessionId
    int personId
    String cookies
    String school
    String server
    String appSecret  // Für erweiterte Authentifizierung

    WebUntisSession(String sessionId, int personId, String cookies, String school, String server) {
        this.sessionId = sessionId
        this.personId = personId
        this.cookies = cookies
        this.school = school
        this.server = server
        this.appSecret = null
    }

    WebUntisSession(String sessionId, int personId, String cookies, String school, String server, String appSecret) {
        this.sessionId = sessionId
        this.personId = personId
        this.cookies = cookies
        this.school = school
        this.server = server
        this.appSecret = appSecret
    }
}
