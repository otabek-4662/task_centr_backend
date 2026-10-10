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

    private final String clientIpHeader;

    public RateLimitFilter(
            RateLimitingService rateLimitingService,
            ObjectMapper objectMapper,
            @Value("${ratelimit.auth.enabled:true}") boolean enabled,
            @Value("${ratelimit.api.enabled:true}") boolean apiEnabled,
            @Value("${CLIENT_IP_HEADER:}") String clientIpHeader) {
        this.rateLimitingService = rateLimitingService;
        this.objectMapper = objectMapper;
        this.enabled = enabled;
        this.apiEnabled = apiEnabled;
        this.clientIpHeader = clientIpHeader != null ? clientIpHeader.trim() : "";
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
            } else if (path.equals("/api/v1/auth/telegram") || path.equals("/api/v1/auth/telegram/pending-invite")) {
                waitTime = rateLimitingService.tryConsumeIpLimit("telegram-auth", clientIp, telegramIpMax, telegramIpWindow);
            } else if (path.equals("/api/auth/register") || path.equals("/api/auth/forgot-password") || path.equals("/api/auth/reset-password")) {
                waitTime = rateLimitingService.tryConsumeIpLimit("register", clientIp, registerIpMax, registerIpWindow);
            } else if (path.matches("^/api/invitations/(by-id/)?[^/]+/(accept|reject)$")) {
                waitTime = rateLimitingService.tryConsumeIpLimit("invite-action", clientIp, 20, 1);
            }

            if (waitTime > 0) {
                sendRateLimitResponse(response, waitTime);
                return;
            }
        }

        if (enabled && "GET".equalsIgnoreCase(method) && path.startsWith("/api/invitations/") && !path.equals("/api/invitations/me")) {
            String clientIp = getClientIp(request);
            long waitTime = rateLimitingService.tryConsumeIpLimit("invite-preview", clientIp, 30, 1);
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
        
        ApiResponse<Void> apiResponse = ApiResponse.<Void>builder()
                .success(false)
                .code("TOO_MANY_REQUESTS")
                .errorCode("RATE_LIMITED")
                .message("Juda ko'p urinish. Birozdan keyin qayta urinib ko'ring.")
                .status(429)
                .retryAfterSeconds(waitTime)
                .build();
        response.getWriter().write(objectMapper.writeValueAsString(apiResponse));
    }

    private static final java.util.regex.Pattern IPV4_PATTERN =
            java.util.regex.Pattern.compile("^((25[0-5]|(2[0-4]|1\\d|[1-9]|)\\d)\\.){3}(25[0-5]|(2[0-4]|1\\d|[1-9]|)\\d)$");

    private static boolean isValidIp(String ip) {
        if (ip == null || ip.isBlank()) {
            return false;
        }
        String candidate = ip.trim();
        if (IPV4_PATTERN.matcher(candidate).matches()) {
            return true;
        }
        if (candidate.contains(":") && candidate.matches("^[0-9a-fA-F:]+$")) {
            try {
                java.net.InetAddress addr = java.net.InetAddress.getByName(candidate);
                return addr instanceof java.net.Inet6Address;
            } catch (Exception ignored) {
                return false;
            }
        }
        return false;
    }

    private String getClientIp(HttpServletRequest request) {
        if (!clientIpHeader.isEmpty()) {
            String headerVal = request.getHeader(clientIpHeader);
            if (headerVal != null && !headerVal.isBlank()) {
                String candidate = headerVal.split(",")[0].trim();
                if (isValidIp(candidate)) {
                    return candidate;
                }
            }
        }
        return request.getRemoteAddr();
    }
}
