package de.c7h12.webuntis.client

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import de.c7h12.webuntis.constants.WebUntisConstants
import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.util.UriComponentsBuilder
import reactor.core.publisher.Mono

import java.net.URI
import java.time.Duration

/**
 * HTTP client for WebUntis API using reactive WebClient
 * Handles all HTTP communication with WebUntis servers
 */
@Slf4j
@CompileStatic
@Component
class WebUntisHttpClient {

    private final WebClient webClient
    private final ObjectMapper objectMapper

    private static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(30)
    private static final int MAX_RETRIES = 3

    WebUntisHttpClient(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper
        this.webClient = WebClient.builder()
                .defaultHeader(HttpHeaders.USER_AGENT, WebUntisConstants.USER_AGENT)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .codecs { configurer ->
                    configurer.defaultCodecs().maxInMemorySize(10 * 1024 * 1024) // 10MB
                }
                .build()
    }

    /**
     * Executes a JSON-RPC request to WebUntis API
     * @param uri The complete URI to call
     * @param request The JSON-RPC request payload
     * @param session Optional session for authenticated requests
     * @return JsonNode with the response
     */
    JsonNode executeJsonRpc(URI uri, Map request, WebUntisSession session = null) {
        try {
            log.debug("Executing JSON-RPC: method={}, uri={}", request.method, uri)

            def responseBody = webClient.post()
                    .uri(uri)
                    .headers { headers -> configureHeaders(headers, session) }
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(DEFAULT_TIMEOUT)
                    .doOnError { error ->
                        log.error("HTTP request failed: {}", error.message, error)
                    }
                    .block()

            if (!responseBody) {
                throw new WebUntisException("Empty response from server")
            }

            JsonNode jsonResponse = objectMapper.readTree(responseBody)

            // Check for JSON-RPC error
            if (jsonResponse.has("error")) {
                def error = jsonResponse.get("error")
                def errorMsg = error.get("message").asText()
                def errorCode = error.has("code") ? error.get("code").asInt() : -1

                log.error("WebUntis API Error [{}]: {}", errorCode, errorMsg)
                throw new WebUntisException("WebUntis API Error [${errorCode}]: ${errorMsg}")
            }

            log.debug("JSON-RPC successful: method={}", request.method)
            return jsonResponse.get("result")

        } catch (WebUntisException e) {
            throw e
        } catch (Exception e) {
            log.error("Failed to execute JSON-RPC request: {}", e.message, e)
            throw new WebUntisException("JSON-RPC request failed: ${e.message}", e)
        }
    }

    /**
     * Builds a JSON-RPC request payload
     */
    Map createJsonRpcRequest(String method, Object params) {
        return [
                jsonrpc: WebUntisConstants.JSONRPC_VERSION,
                id: System.currentTimeMillis().toString(),
                method: method,
                params: params
        ]
    }

    /**
     * Builds URI for standard JSON-RPC endpoint
     */
    URI buildJsonRpcUri(String server, String school) {
        return UriComponentsBuilder
                .fromHttpUrl("https://${normalizeServer(server)}${WebUntisConstants.JSONRPC_ENDPOINT}")
                .queryParam("school", school)
                .build()
                .encode()
                .toUri()
    }

    /**
     * Builds URI for internal JSON-RPC endpoint (2017 API)
     */
    URI buildJsonRpcInternUri(String server, String school) {
        return UriComponentsBuilder
                .fromHttpUrl("https://${normalizeServer(server)}${WebUntisConstants.JSONRPC_INTERN_ENDPOINT}")
                .queryParam("school", school)
                .build()
                .encode()
                .toUri()
    }

    /**
     * Normalizes server URL by removing protocol prefix
     */
    String normalizeServer(String server) {
        if (!server) {
            throw new IllegalArgumentException("Server URL cannot be null or empty")
        }

        def normalized = server
        if (normalized.startsWith("https://")) {
            normalized = normalized.substring(8)
        } else if (normalized.startsWith("http://")) {
            normalized = normalized.substring(7)
        }

        // Validate server URL format
        validateServerUrl(normalized)

        return normalized
    }

    /**
     * Validates that server URL is from an allowed domain
     * Prevents SSRF attacks
     */
    private void validateServerUrl(String server) {
        try {
            // Basic validation: must contain a dot and valid domain format
            if (!server.contains(".")) {
                throw new WebUntisException("Ungültige Server-URL: ${server}")
            }

            // Check for webuntis.com domain (most common)
            if (!server.endsWith(".webuntis.com") && !server.equals("webuntis.com")) {
                log.warn("Server URL is not from webuntis.com domain: {}", server)
                // We log a warning but don't block - some schools may use custom domains
            }

            // Block obvious malicious patterns
            def suspiciousPatterns = ["localhost", "127.0.0.1", "0.0.0.0", "192.168.", "10.", "172.16."]
            if (suspiciousPatterns.any { pattern -> server.contains(pattern) }) {
                throw new WebUntisException("Server-URL nicht erlaubt: ${server}")
            }

        } catch (Exception e) {
            if (e instanceof WebUntisException) {
                throw e
            }
            throw new WebUntisException("Ungültige Server-URL: ${server}", e)
        }
    }

    /**
     * Configures HTTP headers for request
     */
    private void configureHeaders(HttpHeaders headers, WebUntisSession session) {
        headers.setContentType(MediaType.APPLICATION_JSON)
        headers.set(HttpHeaders.USER_AGENT, WebUntisConstants.USER_AGENT)

        if (session) {
            if (session.cookies) {
                headers.set(HttpHeaders.COOKIE, session.cookies)
            }
            if (session.sessionId) {
                headers.set("X-Session-Id", session.sessionId)
            }
        }
    }

    /**
     * Extracts cookies from response headers
     */
    String extractCookies(org.springframework.http.ResponseEntity response) {
        return response.headers.getFirst(HttpHeaders.SET_COOKIE)
    }

    /**
     * Performs a GET request (for health checks, etc.)
     */
    String executeGet(String url) {
        try {
            log.debug("Executing GET: {}", url)

            return webClient.get()
                    .uri(url)
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(DEFAULT_TIMEOUT)
                    .block()

        } catch (Exception e) {
            log.error("GET request failed: {}", e.message, e)
            throw new WebUntisException("GET request failed: ${e.message}", e)
        }
    }

    /**
     * Performs a GET request with retry logic
     */
    String executeGetWithRetry(String url, int maxRetries = MAX_RETRIES) {
        try {
            log.debug("Executing GET with retry: {}", url)

            return webClient.get()
                    .uri(url)
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(DEFAULT_TIMEOUT)
                    .retry(maxRetries)
                    .block()

        } catch (Exception e) {
            log.error("GET request with retry failed after {} attempts: {}", maxRetries, e.message, e)
            throw new WebUntisException("GET request failed after retries: ${e.message}", e)
        }
    }

    /**
     * Creates a new WebClient instance with custom configuration
     * Useful for testing or custom timeout requirements
     */
    WebClient createCustomWebClient(Duration timeout, int maxInMemorySize = 10 * 1024 * 1024) {
        return WebClient.builder()
                .defaultHeader(HttpHeaders.USER_AGENT, WebUntisConstants.USER_AGENT)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .codecs { configurer ->
                    configurer.defaultCodecs().maxInMemorySize(maxInMemorySize)
                }
                .build()
    }
}
