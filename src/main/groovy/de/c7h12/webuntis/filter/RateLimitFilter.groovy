package de.c7h12.webuntis.filter

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import io.github.bucket4j.Bandwidth
import io.github.bucket4j.Bucket
import io.github.bucket4j.Refill
import jakarta.servlet.Filter
import jakarta.servlet.FilterChain
import jakarta.servlet.ServletException
import jakarta.servlet.ServletRequest
import jakarta.servlet.ServletResponse
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.stereotype.Component

import java.time.Duration
import java.util.concurrent.ConcurrentHashMap

/**
 * Rate limiting filter using token bucket algorithm (Bucket4j)
 * Limits requests per IP address
 */
@Slf4j
@CompileStatic
@Component
class RateLimitFilter implements Filter {

    // Cache of buckets per IP address
    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>()

    // Rate limit: 100 requests per minute per IP
    private static final int REQUESTS_PER_MINUTE = 100
    private static final Duration REFILL_DURATION = Duration.ofMinutes(1)

    @Override
    void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest httpRequest = (HttpServletRequest) request
        HttpServletResponse httpResponse = (HttpServletResponse) response

        // Get client IP address
        String clientIp = getClientIp(httpRequest)

        // Get or create bucket for this IP
        Bucket bucket = buckets.computeIfAbsent(clientIp, { k -> createNewBucket() })

        // Try to consume 1 token from bucket
        if (bucket.tryConsume(1)) {
            // Request allowed - proceed with chain
            chain.doFilter(request, response)
        } else {
            // Rate limit exceeded - return 429 Too Many Requests
            log.warn("Rate limit exceeded for IP: {}", clientIp)

            httpResponse.status = HttpServletResponse.SC_TOO_MANY_REQUESTS
            httpResponse.contentType = "application/json"
            httpResponse.characterEncoding = "UTF-8"

            def errorResponse = [
                error: "Zu viele Anfragen. Bitte versuchen Sie es später erneut.",
                timestamp: new Date().toInstant().toString(),
                type: "RATE_LIMIT_EXCEEDED",
                limit: REQUESTS_PER_MINUTE,
                window: "1 minute"
            ]

            httpResponse.writer.write(groovy.json.JsonOutput.toJson(errorResponse))
            httpResponse.writer.flush()
        }
    }

    /**
     * Creates a new token bucket with configured limits
     */
    private static Bucket createNewBucket() {
        Bandwidth limit = Bandwidth.classic(
            REQUESTS_PER_MINUTE,
            Refill.intervally(REQUESTS_PER_MINUTE, REFILL_DURATION)
        )
        return Bucket.builder()
            .addLimit(limit)
            .build()
    }

    /**
     * Extracts client IP address from request, considering proxies
     */
    private static String getClientIp(HttpServletRequest request) {
        // Check X-Forwarded-For header (for proxies/load balancers)
        String xForwardedFor = request.getHeader("X-Forwarded-For")
        if (xForwardedFor) {
            // X-Forwarded-For can contain multiple IPs, take the first one
            return xForwardedFor.split(",")[0].trim()
        }

        // Check X-Real-IP header (common with nginx)
        String xRealIp = request.getHeader("X-Real-IP")
        if (xRealIp) {
            return xRealIp
        }

        // Fallback to remote address
        return request.remoteAddr
    }
}
