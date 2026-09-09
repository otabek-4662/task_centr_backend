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
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
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
        when(userRepository.findByName("elshod")).thenReturn(Optional.of(user));
        when(tokenProvider.generateToken(auth)).thenReturn("jwt-token");

        LoginRequest req = new LoginRequest();
        req.setName("elshod");
        req.setPassword("password123");

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

        assertThatThrownBy(() -> authService.login(req))
                .isInstanceOf(BadCredentialsException.class);
    }
}
