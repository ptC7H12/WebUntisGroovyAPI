package de.c7h12.webuntis.client

class WebUntisSession {
    String sessionId
    int personId
    String cookies
    String school
    String server

    WebUntisSession(String sessionId, int personId, String cookies, String school, String server) {
        this.sessionId = sessionId
        this.personId = personId
        this.cookies = cookies
        this.school = school
        this.server = server
    }
}