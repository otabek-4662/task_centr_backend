package com.taskcenter.service;

import com.taskcenter.dto.TokenRefreshRequest;
import com.taskcenter.dto.TokenRefreshResponse;
import com.taskcenter.exception.ForbiddenException;
import com.taskcenter.exception.ResourceNotFoundException;
import com.taskcenter.model.RefreshToken;
import com.taskcenter.model.User;
import com.taskcenter.repository.RefreshTokenRepository;
import com.taskcenter.repository.UserRepository;
import com.taskcenter.security.JwtTokenProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceRefreshTokenTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private JwtTokenProvider tokenProvider;
    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @InjectMocks
    private AuthService authService;

    @Test
    void refresh_withValidToken_returnsNewTokens() {
        String tokenStr = UUID.randomUUID().toString();
        User user = User.builder().id("u1").name("tester").build();
        RefreshToken oldToken = RefreshToken.builder()
                .id("t1")
                .token(tokenStr)
                .userId(user.getId())
                .expiryDate(LocalDateTime.now().plusDays(1))
                .revoked(false)
                .build();

        when(refreshTokenRepository.findByToken(tokenStr)).thenReturn(Optional.of(oldToken));
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        when(tokenProvider.generateTokenFromUser(user)).thenReturn("new-jwt-token");
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenAnswer(i -> {
            RefreshToken t = i.getArgument(0);
            if (t.getId() == null) {
                t.setId("t2");
            }
            return t;
        });

        TokenRefreshRequest request = new TokenRefreshRequest();
        request.setRefreshToken(tokenStr);

        TokenRefreshResponse response = authService.refresh(request);

        assertThat(response.getAccessToken()).isEqualTo("new-jwt-token");
        assertThat(response.getRefreshToken()).isNotNull().isNotEqualTo(tokenStr);
        assertThat(oldToken.getRevoked()).isTrue();
        
        verify(refreshTokenRepository, times(2)).save(any(RefreshToken.class));
    }

    @Test
    void refresh_withExpiredToken_throwsExceptionAndRevokes() {
        String tokenStr = UUID.randomUUID().toString();
        User user = User.builder().id("u1").name("tester").build();
        RefreshToken oldToken = RefreshToken.builder()
                .id("t1")
                .token(tokenStr)
                .userId(user.getId())
                .expiryDate(LocalDateTime.now().minusDays(1))
                .revoked(false)
                .build();

        when(refreshTokenRepository.findByToken(tokenStr)).thenReturn(Optional.of(oldToken));

        TokenRefreshRequest request = new TokenRefreshRequest();
        request.setRefreshToken(tokenStr);

        assertThatThrownBy(() -> authService.refresh(request))
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("muddati tugagan");

        verify(refreshTokenRepository).delete(oldToken);
    }

    @Test
    void refresh_withRevokedToken_throwsExceptionAndRevokesAll() {
        String tokenStr = UUID.randomUUID().toString();
        User user = User.builder().id("u1").name("tester").build();
        RefreshToken revokedToken = RefreshToken.builder()
                .id("t1")
                .token(tokenStr)
                .userId(user.getId())
                .expiryDate(LocalDateTime.now().plusDays(1))
                .revoked(true)
                .build();

        when(refreshTokenRepository.findByToken(tokenStr)).thenReturn(Optional.of(revokedToken));

        TokenRefreshRequest request = new TokenRefreshRequest();
        request.setRefreshToken(tokenStr);

        assertThatThrownBy(() -> authService.refresh(request))
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("allaqachon ishlatilgan");

        verify(refreshTokenRepository).deleteByUserId("u1");
    }

    @Test
    void logout_validToken_marksAsRevoked() {
        String tokenStr = UUID.randomUUID().toString();
        RefreshToken token = RefreshToken.builder()
                .id("t1")
                .token(tokenStr)
                .revoked(false)
                .build();

        when(refreshTokenRepository.findByToken(tokenStr)).thenReturn(Optional.of(token));

        authService.logout(tokenStr);

        assertThat(token.getRevoked()).isTrue();
        verify(refreshTokenRepository).save(token);
    }
}
