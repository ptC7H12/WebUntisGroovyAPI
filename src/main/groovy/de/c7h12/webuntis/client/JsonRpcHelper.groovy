package de.c7h12.webuntis.client

import de.c7h12.webuntis.constants.WebUntisConstants
import groovy.transform.CompileStatic
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType

/**
 * Helper class for JSON-RPC 2.0 communication with WebUntis API
 */
@CompileStatic
class JsonRpcHelper {

    private static int requestIdCounter = 1

    /**
     * Creates a JSON-RPC 2.0 request
     */
    static Map<String, Object> createJsonRpcRequest(String method, Map params) {
        return [
            jsonrpc: WebUntisConstants.JSONRPC_VERSION,
            id: String.valueOf(requestIdCounter++),
            method: method,
            params: params
        ] as Map<String, Object>
    }

    /**
     * Creates HTTP headers for authenticated WebUntis requests
     */
    static HttpHeaders createAuthenticatedHeaders(WebUntisSession session) {
        def headers = new HttpHeaders()
        headers.setContentType(MediaType.APPLICATION_JSON)
        headers.set(HttpHeaders.USER_AGENT, WebUntisConstants.USER_AGENT)

        if (session.cookies) {
            headers.set(HttpHeaders.COOKIE, "JSESSIONID=${session.sessionId}")
        }

        return headers
    }

    /**
     * Creates basic HTTP headers for non-authenticated requests
     */
    static HttpHeaders createBasicHeaders() {
        def headers = new HttpHeaders()
        headers.setContentType(MediaType.APPLICATION_JSON)
        headers.set(HttpHeaders.USER_AGENT, WebUntisConstants.USER_AGENT)
        return headers
    }

    /**
     * Normalizes server URL (removes https:// prefix)
     */
    static String normalizeServerUrl(String server) {
        def normalized = server
        if (normalized.startsWith("https://")) {
            normalized = normalized.substring(8)
        }
        if (normalized.startsWith("http://")) {
            normalized = normalized.substring(7)
        }
        return normalized
    }

    /**
     * Encodes string for URL query parameters
     */
    static String encodeForUrl(String value) {
        if (!value) return value

        // Check if already encoded
        if (value.contains("%")) {
            return value
        }

        try {
            return URLEncoder.encode(value, "UTF-8").replace("+", "%20")
        } catch (Exception e) {
            return value
        }
    }

    /**
     * Builds full API URL with school parameter
     */
    static String buildApiUrl(String server, String school, String endpoint = WebUntisConstants.JSONRPC_ENDPOINT) {
        def normalizedServer = normalizeServerUrl(server)
        def encodedSchool = encodeForUrl(school)
        return "https://${normalizedServer}${endpoint}?school=${encodedSchool}"
    }
}
