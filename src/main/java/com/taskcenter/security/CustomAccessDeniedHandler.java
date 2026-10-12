package com.taskcenter.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.taskcenter.dto.ApiError;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Instant;

@Component
public class CustomAccessDeniedHandler implements AccessDeniedHandler {

    private final ObjectMapper objectMapper;

    public CustomAccessDeniedHandler(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void handle(HttpServletRequest request,
                       HttpServletResponse response,
                       AccessDeniedException accessDeniedException) throws IOException, ServletException {
        if (response.isCommitted()) {
            return;
        }

        String path = request != null ? request.getRequestURI() : "";
        String message = (accessDeniedException != null && accessDeniedException.getMessage() != null && !accessDeniedException.getMessage().isBlank())
                ? accessDeniedException.getMessage()
                : "Amalni bajarish uchun ruxsat yetarli emas";

        ApiError body = ApiError.builder()
                .timestamp(Instant.now())
                .status(HttpServletResponse.SC_FORBIDDEN)
                .error("FORBIDDEN")
                .message(message)
                .path(path)
                .success(false)
                .code("FORBIDDEN")
                .errorCode("FORBIDDEN")
                .build();

        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        objectMapper.writeValue(response.getWriter(), body);
    }
}
