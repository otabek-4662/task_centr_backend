package com.taskcenter.service;

import com.taskcenter.dto.AuthResponse;
import com.taskcenter.dto.LoginRequest;
import com.taskcenter.dto.RegisterRequest;
import com.taskcenter.model.User;
import com.taskcenter.repository.UserRepository;
import com.taskcenter.security.JwtTokenProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private AuthenticationManager authenticationManager;
    @Mock
    private UserRepository userRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private JwtTokenProvider tokenProvider;
    @Mock
    private com.taskcenter.repository.RefreshTokenRepository refreshTokenRepository;
    @Mock
    private com.taskcenter.repository.WorkspaceInvitationRepository invitationRepository;
    @Mock
    private com.taskcenter.repository.WorkspaceMemberRepository memberRepository;
    @Mock
    private com.taskcenter.repository.PasswordResetTokenRepository passwordResetTokenRepository;
    @Mock
    private EmailService emailService;
    @Mock
    private com.taskcenter.security.RateLimitingService rateLimitingService;

    @InjectMocks
    private AuthService authService;

    private RegisterRequest registerRequest(String name) {
        RegisterRequest req = new RegisterRequest();
        req.setName(name);
        req.setPassword("password123");
        return req;
    }

    @Test
    void register_encodesPasswordAndReturnsToken() {
        Authentication auth = new UsernamePasswordAuthenticationToken("tester", null);
        when(userRepository.existsByName("tester")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("ENC");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            u.setId("u-123");
            return u;
        });
        when(authenticationManager.authenticate(any())).thenReturn(auth);
        when(tokenProvider.generateToken(auth)).thenReturn("jwt-token");

        AuthResponse response = authService.register(registerRequest("tester"));

        assertThat(response.getToken()).isEqualTo("jwt-token");
        assertThat(response.getUser().getName()).isEqualTo("tester");
        verify(passwordEncoder).encode("password123");
    }

    @Test
    void register_duplicateNameThrows() {
        when(userRepository.existsByName("elshod")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(registerRequest("elshod")))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("allaqachon");
    }

    @Test
    void login_returnsTokenForKnownUser() {
        User user = User.builder().id("id1").name("elshod").fullName("Elshod T").role(User.Role.USER).build();
        Authentication auth = new UsernamePasswordAuthenticationToken(user, null);
        when(authenticationManager.authenticate(any())).thenReturn(auth);
        when(userRepository.findByNameOrEmail("elshod")).thenReturn(Optional.of(user));
        when(tokenProvider.generateToken(auth)).thenReturn("jwt-token");

        LoginRequest req = new LoginRequest();
        req.setName("elshod");
        req.setPassword("password123");

        when(rateLimitingService.checkUsernameFailedLimit("elshod")).thenReturn(0L);

        AuthResponse response = authService.login(req);

        assertThat(response.getToken()).isEqualTo("jwt-token");
        assertThat(response.getUser().getName()).isEqualTo("elshod");
    }

    @Test
    void login_badCredentialsPropagate() {
        when(authenticationManager.authenticate(any()))
                .thenThrow(new BadCredentialsException("Bad credentials"));

        LoginRequest req = new LoginRequest();
        req.setName("elshod");
        req.setPassword("wrong");

        when(rateLimitingService.checkUsernameFailedLimit("elshod")).thenReturn(0L);

        assertThatThrownBy(() -> authService.login(req))
                .isInstanceOf(BadCredentialsException.class);
    }

    @Test
    void forgotPassword_whenUserExists_savesTokenAndSendsEmail() {
        User user = User.builder().id("u1").name("tester").email("test@example.com").build();
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));

        com.taskcenter.dto.ForgotPasswordRequest req = new com.taskcenter.dto.ForgotPasswordRequest();
        req.setEmail("test@example.com");

        authService.forgotPassword(req);

        verify(passwordResetTokenRepository).save(any(com.taskcenter.model.PasswordResetToken.class));
        verify(emailService).sendPasswordResetEmail(org.mockito.ArgumentMatchers.eq("test@example.com"), any(String.class));
    }

    @Test
    void forgotPassword_whenUserNotFound_silentSuccess() {
        when(userRepository.findByEmail("unknown@example.com")).thenReturn(Optional.empty());

        com.taskcenter.dto.ForgotPasswordRequest req = new com.taskcenter.dto.ForgotPasswordRequest();
        req.setEmail("unknown@example.com");

        authService.forgotPassword(req);

        org.mockito.Mockito.verifyNoInteractions(passwordResetTokenRepository);
        org.mockito.Mockito.verifyNoInteractions(emailService);
    }

    @Test
    void resetPassword_withValidToken_updatesPasswordAndMarksUsed() {
        User user = User.builder().id("u1").name("tester").password("oldEnc").build();
        com.taskcenter.model.PasswordResetToken token = com.taskcenter.model.PasswordResetToken.builder()
                .token("valid-token")
                .user(user)
                .used(false)
                .expiryDate(java.time.LocalDateTime.now().plusMinutes(20))
                .build();

        when(passwordResetTokenRepository.findByToken("valid-token")).thenReturn(Optional.of(token));
        when(passwordEncoder.encode("newPassword123")).thenReturn("newEnc");

        com.taskcenter.dto.ResetPasswordRequest req = new com.taskcenter.dto.ResetPasswordRequest();
        req.setToken("valid-token");
        req.setNewPassword("newPassword123");

        authService.resetPassword(req);

        assertThat(user.getPassword()).isEqualTo("newEnc");
        assertThat(token.isUsed()).isTrue();
        verify(userRepository).save(user);
        verify(passwordResetTokenRepository).save(token);
    }

    @Test
    void resetPassword_withExpiredToken_throwsBadRequest() {
        User user = User.builder().id("u1").name("tester").build();
        com.taskcenter.model.PasswordResetToken token = com.taskcenter.model.PasswordResetToken.builder()
                .token("expired-token")
                .user(user)
                .used(false)
                .expiryDate(java.time.LocalDateTime.now().minusMinutes(5))
                .build();

        when(passwordResetTokenRepository.findByToken("expired-token")).thenReturn(Optional.of(token));

        com.taskcenter.dto.ResetPasswordRequest req = new com.taskcenter.dto.ResetPasswordRequest();
        req.setToken("expired-token");
        req.setNewPassword("newPassword123");

        assertThatThrownBy(() -> authService.resetPassword(req))
                .isInstanceOf(com.taskcenter.exception.BadRequestException.class)
                .hasMessageContaining("muddati tugagan");
    }

    @Test
    void resetPassword_withInvalidToken_throwsBadRequest() {
        when(passwordResetTokenRepository.findByToken("invalid-token")).thenReturn(Optional.empty());

        com.taskcenter.dto.ResetPasswordRequest req = new com.taskcenter.dto.ResetPasswordRequest();
        req.setToken("invalid-token");
        req.setNewPassword("newPassword123");

        assertThatThrownBy(() -> authService.resetPassword(req))
                .isInstanceOf(com.taskcenter.exception.BadRequestException.class)
                .hasMessageContaining("yaroqsiz yoki topilmadi");
    }

    @Test
    void resetPassword_withUsedToken_throwsBadRequest() {
        User user = User.builder().id("u1").name("tester").build();
        com.taskcenter.model.PasswordResetToken token = com.taskcenter.model.PasswordResetToken.builder()
                .token("used-token")
                .user(user)
                .used(true)
                .expiryDate(java.time.LocalDateTime.now().plusMinutes(20))
                .build();

        when(passwordResetTokenRepository.findByToken("used-token")).thenReturn(Optional.of(token));

        com.taskcenter.dto.ResetPasswordRequest req = new com.taskcenter.dto.ResetPasswordRequest();
        req.setToken("used-token");
        req.setNewPassword("newPassword123");

        assertThatThrownBy(() -> authService.resetPassword(req))
                .isInstanceOf(com.taskcenter.exception.BadRequestException.class)
                .hasMessageContaining("ishlatilgan");
    }

    // === FIX #4 — register() @Transactional: invitation auto-link ===

    @Test
    void register_withPendingInvitation_autoLinksToWorkspace() {
        // ARRANGE
        com.taskcenter.model.WorkspaceInvitation inv = com.taskcenter.model.WorkspaceInvitation.builder()
                .id("inv-1")
                .workspaceId("ws-1")
                .receiverEmail("newuser")
                .role(com.taskcenter.model.WorkspaceRole.MEMBER)
                .status(com.taskcenter.model.InvitationStatus.PENDING)
                .expiresAt(java.time.LocalDateTime.now().plusDays(7))
                .build();

        Authentication auth = new UsernamePasswordAuthenticationToken("newuser", null);
        when(userRepository.existsByName("newuser")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("ENC");
        when(userRepository.save(any(User.class))).thenAnswer(i -> {
            User u = i.getArgument(0);
            u.setId("u-new");
            return u;
        });
        when(invitationRepository.findByReceiverEmailAndStatus("newuser",
                com.taskcenter.model.InvitationStatus.PENDING))
                .thenReturn(java.util.List.of(inv));
        when(memberRepository.existsByWorkspaceIdAndUserId("ws-1", "u-new")).thenReturn(false);
        when(authenticationManager.authenticate(any())).thenReturn(auth);
        when(tokenProvider.generateToken(auth)).thenReturn("jwt");

        // ACT
        AuthResponse response = authService.register(registerRequest("newuser"));

        // ASSERT: workspace member yaratilishi kerak
        verify(memberRepository).save(any(com.taskcenter.model.WorkspaceMember.class));
        // invitation ACCEPTED holatga o'tishi kerak
        verify(invitationRepository).save(any(com.taskcenter.model.WorkspaceInvitation.class));
        assertThat(response.getToken()).isEqualTo("jwt");
    }

    @Test
    void register_invitationSaveFails_exceptionPropagates() {
        // @Transactional bo'lmaganda user DB da qolardi, lekin member qo'shilmasdi
        // Endi @Transactional bilan exception propagate bo'ladi (rollback Spring ta'minlaydi)
        com.taskcenter.model.WorkspaceInvitation inv = com.taskcenter.model.WorkspaceInvitation.builder()
                .id("inv-2")
                .workspaceId("ws-2")
                .receiverEmail("failuser")
                .role(com.taskcenter.model.WorkspaceRole.MEMBER)
                .status(com.taskcenter.model.InvitationStatus.PENDING)
                .expiresAt(java.time.LocalDateTime.now().plusDays(7))
                .build();

        when(userRepository.existsByName("failuser")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("ENC");
        when(userRepository.save(any(User.class))).thenAnswer(i -> {
            User u = i.getArgument(0);
            u.setId("u-fail");
            return u;
        });
        when(invitationRepository.findByReceiverEmailAndStatus("failuser",
                com.taskcenter.model.InvitationStatus.PENDING))
                .thenReturn(java.util.List.of(inv));
        when(memberRepository.existsByWorkspaceIdAndUserId("ws-2", "u-fail")).thenReturn(false);
        // invitation save chaqiruvida DB xatosi simulatsiya
        when(invitationRepository.save(any())).thenThrow(new RuntimeException("DB constraint violation"));

        // @Transactional bo'lgani uchun exception tashqariga chiqadi (rollback bo'ladi)
        assertThatThrownBy(() -> authService.register(registerRequest("failuser")))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("DB constraint violation");
    }
}
