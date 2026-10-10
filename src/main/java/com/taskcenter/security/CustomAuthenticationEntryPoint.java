package com.taskcenter.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.taskcenter.dto.ApiResponse;
import io.jsonwebtoken.ExpiredJwtException;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class CustomAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;

    public CustomAuthenticationEntryPoint(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void commence(HttpServletRequest request,
                         HttpServletResponse response,
                         AuthenticationException authException) throws IOException, ServletException {
        boolean isExpired = Boolean.TRUE.equals(request.getAttribute("token_expired"))
                || request.getAttribute("jwt_exception") instanceof ExpiredJwtException;

        String code = isExpired ? "TOKEN_EXPIRED" : "UNAUTHORIZED";
        String errorCode = isExpired ? "TOKEN_EXPIRED" : "UNAUTHORIZED";
        String message = isExpired ? "JWT token muddati tugagan" : "Autentifikatsiyadan o'tilmagan yoki token yaroqsiz";

        ApiResponse<Void> body = ApiResponse.<Void>builder()
                .success(false)
                .code(code)
                .errorCode(errorCode)
                .message(message)
                .status(HttpServletResponse.SC_UNAUTHORIZED)
                .build();

        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        objectMapper.writeValue(response.getWriter(), body);
    }
}
