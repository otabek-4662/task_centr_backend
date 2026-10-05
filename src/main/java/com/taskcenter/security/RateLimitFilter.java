package com.taskcenter.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.taskcenter.dto.ApiResponse;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class RateLimitFilter extends OncePerRequestFilter {

    private final RateLimitingService rateLimitingService;
    private final ObjectMapper objectMapper;
    private final boolean enabled;
    private final boolean apiEnabled;

    @Value("${ratelimit.login.ip.max:10}")
    private int loginIpMax;
    @Value("${ratelimit.login.ip.window-minutes:5}")
    private int loginIpWindow;

    @Value("${ratelimit.telegram.ip.max:30}")
    private int telegramIpMax;
    @Value("${ratelimit.telegram.ip.window-minutes:1}")
    private int telegramIpWindow;

    @Value("${ratelimit.register.ip.max:5}")
    private int registerIpMax;
    @Value("${ratelimit.register.ip.window-minutes:10}")
    private int registerIpWindow;

    public RateLimitFilter(
            RateLimitingService rateLimitingService,
            ObjectMapper objectMapper,
            @Value("${ratelimit.auth.enabled:true}") boolean enabled,
            @Value("${ratelimit.api.enabled:true}") boolean apiEnabled) {
        this.rateLimitingService = rateLimitingService;
        this.objectMapper = objectMapper;
        this.enabled = enabled;
        this.apiEnabled = apiEnabled;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws IOException, ServletException {
        if (!enabled && !apiEnabled) {
            filterChain.doFilter(request, response);
            return;
        }

        String path = request.getRequestURI();
        String method = request.getMethod();

        if (enabled && "POST".equalsIgnoreCase(method)) {
            String clientIp = getClientIp(request);
            long waitTime = 0;

            if (path.equals("/api/auth/login")) {
                waitTime = rateLimitingService.tryConsumeIpLimit("login", clientIp, loginIpMax, loginIpWindow);
            } else if (path.equals("/api/v1/auth/telegram")) {
                waitTime = rateLimitingService.tryConsumeIpLimit("telegram-auth", clientIp, telegramIpMax, telegramIpWindow);
            } else if (path.equals("/api/auth/register") || path.equals("/api/auth/forgot-password") || path.equals("/api/auth/reset-password")) {
                waitTime = rateLimitingService.tryConsumeIpLimit("register", clientIp, registerIpMax, registerIpWindow);
            }

            if (waitTime > 0) {
                sendRateLimitResponse(response, waitTime);
                return;
            }
        }

        filterChain.doFilter(request, response);
    }

    private void sendRateLimitResponse(HttpServletResponse response, long waitTime) throws IOException {
        response.setStatus(429);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.setHeader("Retry-After", String.valueOf(waitTime));
        
        ApiResponse<Void> apiResponse = ApiResponse.error("Juda ko'p urinish. Birozdan keyin qayta urinib ko'ring.");
        response.getWriter().write(objectMapper.writeValueAsString(apiResponse));
    }

    private String getClientIp(HttpServletRequest request) {
        /*
         * Render's proxy behavior:
         * Render appends the actual remote IP (the IP of the client connecting to Render) 
         * to the X-Forwarded-For header. If a client attempts to spoof the header by sending 
         * their own X-Forwarded-For, Render will append the real IP to the end of the list.
         * Therefore, the LAST trusted hop (the last IP in the comma-separated list) 
         * is the actual client IP.
         */
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            String[] ips = xForwardedFor.split(",");
            return ips[ips.length - 1].trim();
        }
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }
        return request.getRemoteAddr();
    }
}
