package com.taskcenter.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RateLimitFilterTest {

    @Mock
    private RateLimitingService rateLimitingService;

    @Mock
    private FilterChain filterChain;

    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    @Test
    @DisplayName("CLIENT_IP_HEADER sozlanganda ko'rsatilgan sarlavhadan IP ni oladi va X-Forwarded-For ga aldanmaydi")
    void usesConfiguredClientIpHeaderAndIgnoresOtherHeaders() throws ServletException, IOException {
        RateLimitFilter filter = new RateLimitFilter(rateLimitingService, objectMapper, true, true, "CF-Connecting-IP");
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/auth/login");
        request.addHeader("CF-Connecting-IP", "198.51.100.42");
        request.addHeader("X-Forwarded-For", "203.0.113.195, 10.0.0.1"); // Mijoz yuborgan soxta IP
        MockHttpServletResponse response = new MockHttpServletResponse();

        when(rateLimitingService.tryConsumeIpLimit(eq("login"), eq("198.51.100.42"), anyInt(), anyInt()))
                .thenReturn(0L);

        filter.doFilterInternal(request, response, filterChain);

        verify(rateLimitingService).tryConsumeIpLimit(eq("login"), eq("198.51.100.42"), anyInt(), anyInt());
        verify(rateLimitingService, never()).tryConsumeIpLimit(eq("login"), eq("203.0.113.195"), anyInt(), anyInt());
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("CLIENT_IP_HEADER bo'sh bo'lganda request.getRemoteAddr() ishlatiladi")
    void usesRemoteAddrWhenClientIpHeaderEmpty() throws ServletException, IOException {
        RateLimitFilter filter = new RateLimitFilter(rateLimitingService, objectMapper, true, true, "");
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/auth/login");
        request.setRemoteAddr("192.168.1.55");
        request.addHeader("CF-Connecting-IP", "198.51.100.99");
        request.addHeader("X-Forwarded-For", "203.0.113.50");
        MockHttpServletResponse response = new MockHttpServletResponse();

        when(rateLimitingService.tryConsumeIpLimit(eq("login"), eq("192.168.1.55"), anyInt(), anyInt()))
                .thenReturn(0L);

        filter.doFilterInternal(request, response, filterChain);

        verify(rateLimitingService).tryConsumeIpLimit(eq("login"), eq("192.168.1.55"), anyInt(), anyInt());
        verify(rateLimitingService, never()).tryConsumeIpLimit(eq("login"), eq("198.51.100.99"), anyInt(), anyInt());
    }

    @Test
    @DisplayName("Soxta yoki noto'g'ri formatli sarlavha kelganda getRemoteAddr() ga qaytadi")
    void fallsBackToRemoteAddrWhenHeaderValueIsNotValidIp() throws ServletException, IOException {
        RateLimitFilter filter = new RateLimitFilter(rateLimitingService, objectMapper, true, true, "CF-Connecting-IP");
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/auth/login");
        request.setRemoteAddr("172.16.0.2");
        // Soxta / noto'g'ri IP formatlari: harflar, buzilgan sonlar, domain
        request.addHeader("CF-Connecting-IP", "malicious-header-value-999.999.999.999");
        MockHttpServletResponse response = new MockHttpServletResponse();

        when(rateLimitingService.tryConsumeIpLimit(eq("login"), eq("172.16.0.2"), anyInt(), anyInt()))
                .thenReturn(0L);

        filter.doFilterInternal(request, response, filterChain);

        verify(rateLimitingService).tryConsumeIpLimit(eq("login"), eq("172.16.0.2"), anyInt(), anyInt());
        verify(rateLimitingService, never()).tryConsumeIpLimit(eq("login"), eq("malicious-header-value-999.999.999.999"), anyInt(), anyInt());
    }

    @Test
    @DisplayName("POST /api/invitations/by-id/{id}/accept rate limit nazoratidan o'tadi")
    void invitationByIdAccept_rateLimited() throws ServletException, IOException {
        RateLimitFilter filter = new RateLimitFilter(rateLimitingService, objectMapper, true, true, "X-Custom-Client-IP");
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/invitations/by-id/inv-123/accept");
        request.addHeader("X-Custom-Client-IP", "198.51.100.10");
        MockHttpServletResponse response = new MockHttpServletResponse();

        when(rateLimitingService.tryConsumeIpLimit(eq("invite-action"), eq("198.51.100.10"), eq(20), eq(1)))
                .thenReturn(45L); // Limit oshgan

        filter.doFilterInternal(request, response, filterChain);

        assertEquals(429, response.getStatus());
        assertEquals("45", response.getHeader("Retry-After"));
        verify(filterChain, never()).doFilter(any(), any());
    }
}
