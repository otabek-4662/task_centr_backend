package com.taskcenter.security;

import io.github.bucket4j.Bucket;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Map;

@Component
public class RateLimitFilter extends OncePerRequestFilter {

    @Autowired
    private Map<String, Bucket> bucketCache;

    @Value("${ratelimit.auth.enabled:true}")
    private boolean enabled;

    @Value("${ratelimit.api.enabled:true}")
    private boolean apiEnabled;

    private Bucket resolveBucket(String key, int capacity, int refillTokens, java.time.Duration refillDuration) {
        return bucketCache.computeIfAbsent(key, k -> io.github.bucket4j.Bucket.builder()
                .addLimit(io.github.bucket4j.Bandwidth.classic(capacity, io.github.bucket4j.Refill.greedy(refillTokens, refillDuration)))
                .build());
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws IOException, jakarta.servlet.ServletException {
        if (!enabled && !apiEnabled) {
            filterChain.doFilter(request, response);
            return;
        }

        String path = request.getRequestURI();

        // Auth endpoints: stricter limit (10 req/min)
        if (enabled && path.startsWith("/api/auth/")) {
            String clientIp = getClientIp(request);
            Bucket bucket = resolveBucket("auth:" + clientIp, 10, 10, java.time.Duration.ofMinutes(1));

            if (!bucket.tryConsume(1)) {
                response.setStatus(429);
                response.setContentType("application/json");
                response.getWriter().write("{\"success\":false,\"message\":\"Too many requests. Please try again later.\",\"data\":null}");
                return;
            }
        }

        // All other API endpoints: moderate limit (100 req/min)
        if (apiEnabled && path.startsWith("/api/") && !path.startsWith("/api/auth/")) {
            String clientIp = getClientIp(request);
            Bucket bucket = resolveBucket("api:" + clientIp, 100, 100, java.time.Duration.ofMinutes(1));

            if (!bucket.tryConsume(1)) {
                response.setStatus(429);
                response.setContentType("application/json");
                response.getWriter().write("{\"success\":false,\"message\":\"Too many requests. Please try again later.\",\"data\":null}");
                return;
            }
        }

        filterChain.doFilter(request, response);
    }

    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }
        return request.getRemoteAddr();
    }
}