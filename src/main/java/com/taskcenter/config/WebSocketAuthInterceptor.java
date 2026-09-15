package com.taskcenter.config;

import com.taskcenter.security.JwtTokenProvider;
import com.taskcenter.service.WorkspaceAuthorizationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.util.Map;

@Component
@RequiredArgsConstructor
public class WebSocketAuthInterceptor implements HandshakeInterceptor {

    private final JwtTokenProvider jwtTokenProvider;
    private final WorkspaceAuthorizationService authorizationService;

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response, WebSocketHandler wsHandler, Map<String, Object> attributes) {
        // Extract token from query parameters or headers
        String token = extractToken(request);
        if (token == null || !jwtTokenProvider.validateToken(token)) {
            return false; // Reject connection
        }
        // Store user info in attributes for later use in channel interceptor
        String userId = jwtTokenProvider.getUserIdFromToken(token);
        attributes.put("userId", userId);
        return true;
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response, WebSocketHandler wsHandler, Exception exception) {
        // No-op
    }

    private String extractToken(ServerHttpRequest request) {
        // Try to get token from query parameter "token" (?token=...)
        String query = request.getURI().getQuery();
        if (query != null) {
            for (String param : query.split("&")) {
                int idx = param.indexOf('=');
                String name = idx >= 0 ? param.substring(0, idx) : param;
                if (name.equals("token")) {
                    String value = idx >= 0 ? param.substring(idx + 1) : "";
                    return java.net.URLDecoder.decode(value, java.nio.charset.StandardCharsets.UTF_8);
                }
            }
        }
        // Try to get token from header "Authorization: Bearer <token>"
        String authHeader = request.getHeaders().getFirst("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }
        return null;
    }
}