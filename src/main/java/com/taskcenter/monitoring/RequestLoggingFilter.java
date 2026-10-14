package com.taskcenter.monitoring;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

@Component
@Order(1)
public class RequestLoggingFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(RequestLoggingFilter.class);

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        long start = System.currentTimeMillis();
        String requestId = UUID.randomUUID().toString().substring(0, 8);
        response.setHeader("X-Request-ID", requestId);

        try {
            filterChain.doFilter(request, response);
        } finally {
            long duration = System.currentTimeMillis() - start;
            String method = request.getMethod();
            String uri = request.getRequestURI();
            int status = response.getStatus();

            if (duration > 1000 || status >= 500) {
                log.error("[SLOW/ERROR] requestId={} method={} uri={} status={} duration={}ms",
                        requestId, method, uri, status, duration);
            } else if (status >= 400) {
                log.warn("[WARN] requestId={} method={} uri={} status={} duration={}ms",
                        requestId, method, uri, status, duration);
            } else {
                log.debug("[INFO] requestId={} method={} uri={} status={} duration={}ms",
                        requestId, method, uri, status, duration);
            }
        }
    }
}
