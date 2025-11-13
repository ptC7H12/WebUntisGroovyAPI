package de.c7h12.webuntis.client

import de.c7h12.webuntis.constants.WebUntisConstants
import groovy.transform.CompileStatic

/**
 * Represents a WebUntis API session with authentication and master data caching
 */
@CompileStatic
class WebUntisSession {
    String sessionId
    int personId
    String cookies
    String school
    String server
    String appSecret     // For enhanced 2017 API authentication
    String username      // For 2017 API
    Map masterData       // Master data cache

    private WebUntisSession() {
        // Private constructor - use builder
    }

    /**
     * Creates a new builder for WebUntisSession
     */
    static Builder builder() {
        return new Builder()
    }

    /**
     * Checks if session is compatible with 2017 API
     */
    boolean is2017Compatible() {
        return appSecret != null && username != null
    }

    /**
     * Checks if master data is available in cache
     */
    boolean hasMasterData() {
        return masterData != null && !masterData.isEmpty()
    }

    /**
     * Returns age of cached master data in milliseconds
     */
    long getMasterDataAge() {
        if (masterData?.timestamp) {
            return System.currentTimeMillis() - (masterData.timestamp as Long)
        }
        return Long.MAX_VALUE // Very old if no timestamp
    }

    /**
     * Checks if master data cache is still valid
     */
    boolean isMasterDataValid() {
        return hasMasterData() && getMasterDataAge() < WebUntisConstants.MASTER_DATA_CACHE_VALIDITY_MS
    }

    // Helper methods to retrieve specific master data
    Map getSubjectsMap() {
        return masterData?.subjects as Map ?: [:]
    }

    Map getTeachersMap() {
        return masterData?.teachers as Map ?: [:]
    }

    Map getRoomsMap() {
        return masterData?.rooms as Map ?: [:]
    }

    Map getClassesMap() {
        return masterData?.klassen as Map ?: [:]
    }

    Map getDepartmentsMap() {
        return masterData?.departments as Map ?: [:]
    }

    List getHolidays() {
        return masterData?.holidays as List ?: []
    }

    List getSchoolYears() {
        return masterData?.schoolyears as List ?: []
    }

    Map getTimeGrid() {
        return masterData?.timeGrid as Map ?: [:]
    }

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

    /**
     * Builder for WebUntisSession
     */
    static class Builder {
        private String sessionId
        private int personId
        private String cookies
        private String school
        private String server
        private String appSecret
        private String username
        private Map masterData

        Builder sessionId(String sessionId) {
            this.sessionId = sessionId
            return this
        }

        Builder personId(int personId) {
            this.personId = personId
            return this
        }

        Builder cookies(String cookies) {
            this.cookies = cookies
            return this
        }

        Builder school(String school) {
            this.school = school
            return this
        }

        Builder server(String server) {
            this.server = server
            return this
        }

        Builder appSecret(String appSecret) {
            this.appSecret = appSecret
            return this
        }

        Builder username(String username) {
            this.username = username
            return this
        }

        Builder masterData(Map masterData) {
            this.masterData = masterData
            return this
        }

        WebUntisSession build() {
            def session = new WebUntisSession()
            session.sessionId = this.sessionId
            session.personId = this.personId
            session.cookies = this.cookies
            session.school = this.school
            session.server = this.server
            session.appSecret = this.appSecret
            session.username = this.username
            session.masterData = this.masterData
            return session
        }
    }
}