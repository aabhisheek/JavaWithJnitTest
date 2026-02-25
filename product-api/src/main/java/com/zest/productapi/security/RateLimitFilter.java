package com.zest.productapi.security;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Rate-limits the login and register endpoints to 10 requests / minute per IP.
 * Uses Bucket4j token-bucket algorithm – no external cache required.
 */
@Component
@Slf4j
public class RateLimitFilter extends OncePerRequestFilter {

    private static final int    CAPACITY        = 10;
    private static final long   REFILL_MINUTES  = 1;
    private static final String[] RATE_LIMIT_PATHS = {
        "/api/v1/auth/login",
        "/api/v1/auth/register"
    };

    /** One bucket per IP address – entries accumulate in memory. */
    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();

    @Override
    protected void doFilterInternal(HttpServletRequest  request,
                                    HttpServletResponse response,
                                    FilterChain         chain)
            throws ServletException, IOException {

        if (isRateLimitedPath(request.getRequestURI())) {
            String ip     = resolveClientIp(request);
            Bucket bucket = buckets.computeIfAbsent(ip, this::newBucket);

            if (!bucket.tryConsume(1)) {
                log.warn("Rate limit exceeded for IP={} path={}", ip, request.getRequestURI());
                response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
                response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                response.getWriter().write(
                    "{\"success\":false,\"message\":\"Too many requests – try again later\"}");
                return;
            }
        }

        chain.doFilter(request, response);
    }

    private boolean isRateLimitedPath(String uri) {
        for (String path : RATE_LIMIT_PATHS) {
            if (uri.equals(path)) return true;
        }
        return false;
    }

    private Bucket newBucket(String ip) {
        Bandwidth limit = Bandwidth.classic(
            CAPACITY,
            Refill.intervally(CAPACITY, Duration.ofMinutes(REFILL_MINUTES)));
        return Bucket.builder().addLimit(limit).build();
    }

    /** Respect X-Forwarded-For header when behind a proxy/load-balancer. */
    private String resolveClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
