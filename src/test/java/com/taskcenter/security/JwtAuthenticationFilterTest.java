package com.taskcenter.security;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.MalformedJwtException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import jakarta.servlet.FilterChain;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {

    @Mock
    private JwtTokenProvider tokenProvider;
    @Mock
    private UserDetailsService userDetailsService;
    @Mock
    private FilterChain filterChain;
    @Mock
    private UserDetails userDetails;

    private JwtAuthenticationFilter filter;

    @BeforeEach
    void setUp() {
        filter = new JwtAuthenticationFilter(tokenProvider, userDetailsService);
        SecurityContextHolder.clearContext();
    }

    // ===== HAPPY PATH =====

    @Test
    void validToken_setsAuthentication() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer valid.jwt.token");
        MockHttpServletResponse response = new MockHttpServletResponse();

        when(tokenProvider.validateToken("valid.jwt.token")).thenReturn(true);
        when(tokenProvider.getUserNameFromJWT("valid.jwt.token")).thenReturn("elshod");
        when(userDetailsService.loadUserByUsername("elshod")).thenReturn(userDetails);
        when(userDetails.getAuthorities()).thenReturn(List.of());

        filter.doFilterInternal(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
        assertThat(SecurityContextHolder.getContext().getAuthentication().getPrincipal()).isEqualTo(userDetails);
        verify(filterChain).doFilter(request, response);
    }

    // ===== NO TOKEN =====

    @Test
    void noAuthorizationHeader_doesNotAuthenticate() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilterInternal(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(filterChain).doFilter(request, response);
        verifyNoInteractions(tokenProvider);
    }

    @Test
    void nonBearerHeader_doesNotAuthenticate() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Basic dXNlcjpwYXNz");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilterInternal(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(filterChain).doFilter(request, response);
    }

    // ===== INVALID TOKEN =====

    @Test
    void invalidToken_doesNotAuthenticate_chainContinues() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer invalid.token");
        MockHttpServletResponse response = new MockHttpServletResponse();

        when(tokenProvider.validateToken("invalid.token")).thenReturn(false);

        filter.doFilterInternal(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(filterChain).doFilter(request, response);
    }

    // ===== EXCEPTION CASES =====

    @Test
    void expiredJwtException_doesNotAuthenticate_chainContinues() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer expired.token");
        MockHttpServletResponse response = new MockHttpServletResponse();

        when(tokenProvider.validateToken("expired.token"))
                .thenThrow(new ExpiredJwtException(null, null, "Token expired"));

        filter.doFilterInternal(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void malformedJwtException_doesNotAuthenticate_chainContinues() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer malformed");
        MockHttpServletResponse response = new MockHttpServletResponse();

        when(tokenProvider.validateToken("malformed"))
                .thenThrow(new MalformedJwtException("Bad format"));

        filter.doFilterInternal(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void usernameNotFound_doesNotAuthenticate_chainContinues() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer valid.but.user.deleted");
        MockHttpServletResponse response = new MockHttpServletResponse();

        when(tokenProvider.validateToken("valid.but.user.deleted")).thenReturn(true);
        when(tokenProvider.getUserNameFromJWT("valid.but.user.deleted")).thenReturn("deletedUser");
        when(userDetailsService.loadUserByUsername("deletedUser"))
                .thenThrow(new UsernameNotFoundException("User not found"));

        filter.doFilterInternal(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void unexpectedException_doesNotAuthenticate_chainContinues() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer some.token");
        MockHttpServletResponse response = new MockHttpServletResponse();

        when(tokenProvider.validateToken("some.token"))
                .thenThrow(new RuntimeException("Unexpected DB error"));

        filter.doFilterInternal(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void emptyBearerValue_doesNotAuthenticate() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer ");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilterInternal(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(filterChain).doFilter(request, response);
    }
}
