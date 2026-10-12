package com.taskcenter.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.taskcenter.model.User;
import com.taskcenter.repository.UserRepository;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@DisplayName("TelegramWebAppAuthFilter — Telegram Mini App initData Auth Filter Tests")
class TelegramWebAppAuthFilterTest {

    private TelegramInitDataValidator validator;
    private UserRepository userRepository;
    private ObjectMapper objectMapper;
    private TelegramWebAppAuthFilter filter;
    private FilterChain filterChain;

    @BeforeEach
    void setUp() {
        validator = mock(TelegramInitDataValidator.class);
        userRepository = mock(UserRepository.class);
        objectMapper = new ObjectMapper().findAndRegisterModules();
        filter = new TelegramWebAppAuthFilter(validator, userRepository, objectMapper);
        filterChain = mock(FilterChain.class);
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("Sarlavhalarda Telegram initData bo'lmasa: filtr zanjiri hech qanday to'siqsiz davom etadi")
    void doFilter_noTelegramHeader_continuesChain() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/workspaces");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain, times(1)).doFilter(request, response);
        assertNull(SecurityContextHolder.getContext().getAuthentication());
        assertEquals(200, response.getStatus());
    }

    @Test
    @DisplayName("Yaroqsiz yoki muddati o'tgan initData kelganda: 401 Unauthorized qaytaradi")
    void doFilter_invalidInitData_returns401() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/workspaces");
        request.addHeader(TelegramWebAppAuthFilter.HEADER_INIT_DATA, "invalid_init_data_string");
        MockHttpServletResponse response = new MockHttpServletResponse();

        when(validator.validate("invalid_init_data_string")).thenReturn(Optional.empty());

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain, never()).doFilter(request, response);
        assertEquals(401, response.getStatus());
        assertTrue(response.getContentAsString().contains("UNAUTHORIZED"));
    }

    @Test
    @DisplayName("To'g'ri initData va foydalanuvchi bazada mavjud bo'lganda: SecurityContext da autentifikatsiya qilinadi")
    void doFilter_validInitData_authenticatesUser() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/workspaces");
        String initData = "query_id=123&user=%7B%22id%22%3A987654321%7D&auth_date=1620000000&hash=validhash";
        request.addHeader("Authorization", "tma " + initData);
        MockHttpServletResponse response = new MockHttpServletResponse();

        Map<String, String> parsedParams = Map.of(
                "query_id", "123",
                "user", "{\"id\":987654321,\"username\":\"bekmurod\"}",
                "auth_date", "1620000000"
        );
        when(validator.validate(initData)).thenReturn(Optional.of(parsedParams));

        User user = User.builder()
                .id("u-123")
                .name("bekmurod")
                .telegramChatId(987654321L)
                .role(User.Role.USER)
                .build();
        when(userRepository.findByTelegramChatId(987654321L)).thenReturn(Optional.of(user));

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain, times(1)).doFilter(request, response);
        assertNotNull(SecurityContextHolder.getContext().getAuthentication());
        assertEquals(user, SecurityContextHolder.getContext().getAuthentication().getPrincipal());
        assertEquals(987654321L, request.getAttribute(TelegramWebAppAuthFilter.ATTR_TELEGRAM_USER_ID));
    }

    @Test
    @DisplayName("To'g'ri initData, lekin Telegram ID bazadagi hech bir foydalanuvchiga ulanmagan bo'lsa: 401 qaytaradi")
    void doFilter_validInitData_userNotLinked_returns401() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/workspaces");
        String initData = "user=%7B%22id%22%3A111222%7D&auth_date=1620000000&hash=valid";
        request.addHeader(TelegramWebAppAuthFilter.HEADER_INIT_DATA, initData);
        MockHttpServletResponse response = new MockHttpServletResponse();

        Map<String, String> parsedParams = Map.of(
                "user", "{\"id\":111222,\"username\":\"unlinked_user\"}"
        );
        when(validator.validate(initData)).thenReturn(Optional.of(parsedParams));
        when(userRepository.findByTelegramChatId(111222L)).thenReturn(Optional.empty());

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain, never()).doFilter(request, response);
        assertEquals(401, response.getStatus());
        assertTrue(response.getContentAsString().contains("Telegram hisob"));
    }
}
