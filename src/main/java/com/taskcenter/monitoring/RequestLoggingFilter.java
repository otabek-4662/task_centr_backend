package com.taskcenter.monitoring;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

@Component
@Order(1)
public class RequestLoggingFilter extends OncePerRequestFilter {

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
                System.err.printf("[SLOW/ERROR] requestId=%s method=%s uri=%s status=%d duration=%dms%n",
                        requestId, method, uri, status, duration);
            } else if (status >= 400) {
                System.out.printf("[WARN] requestId=%s method=%s uri=%s status=%d duration=%dms%n",
                        requestId, method, uri, status, duration);
            } else {
                System.out.printf("[INFO] requestId=%s method=%s uri=%s status=%d duration=%dms%n",
                        requestId, method, uri, status, duration);
            }
        }
    }
}