package com.taskcenter.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.taskcenter.dto.ApiResponse;
import com.taskcenter.dto.AuthResponse;
import com.taskcenter.model.User;
import com.taskcenter.repository.UserRepository;
import com.taskcenter.security.JwtTokenProvider;
import com.taskcenter.security.TelegramInitDataValidator;
import com.taskcenter.service.AuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TelegramAuthControllerTest {

    @Mock
    private TelegramInitDataValidator validator;
    private final ObjectMapper mapper = new ObjectMapper();
    @Mock
    private UserRepository userRepository;
    @Mock
    private JwtTokenProvider tokenProvider;
    @Mock
    private AuthService authService;

    private TelegramAuthController controller;

    @BeforeEach
    void setUp() {
        controller = new TelegramAuthController(validator, mapper, userRepository, tokenProvider, authService);
    }

    @Test
    @DisplayName("Foydalanuvchi topilsa muvaffaqiyatli JWT va Refresh token qaytadi")
    void telegramLogin_validUser_returnsToken() throws Exception {
        String initData = "valid_init_data";
        when(validator.validate(initData)).thenReturn(Optional.of(Map.of(
                "user", "{\"id\":12345,\"first_name\":\"Bek\"}"
        )));

        User user = User.builder().id("u-1").name("bek").fullName("Bekmurod").telegramChatId(12345L).build();
        when(userRepository.findByTelegramChatId(12345L)).thenReturn(Optional.of(user));
        when(tokenProvider.generateTokenFromUser(user)).thenReturn("jwt-token-xyz");
        when(authService.createRefreshToken("u-1")).thenReturn("refresh-token-xyz");

        ResponseEntity<ApiResponse<AuthResponse>> response = controller.telegramLogin(
                new TelegramAuthController.TelegramAuthRequest(initData)
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getData().getToken()).isEqualTo("jwt-token-xyz");
        assertThat(response.getBody().getData().getRefreshToken()).isEqualTo("refresh-token-xyz");
        assertThat(response.getBody().getData().getUser().getName()).isEqualTo("bek");
    }

    @Test
    @DisplayName("Foydalanuvchi bazada topilmasa 401 'Avval botda akkauntingizni ulang' qaytadi")
    void telegramLogin_userNotFound_throws401WithBotMessage() {
        String initData = "valid_init_data";
        when(validator.validate(initData)).thenReturn(Optional.of(Map.of(
                "user", "{\"id\":99999,\"first_name\":\"Stranger\"}"
        )));
        when(userRepository.findByTelegramChatId(99999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> controller.telegramLogin(new TelegramAuthController.TelegramAuthRequest(initData)))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(e -> {
                    ResponseStatusException rse = (ResponseStatusException) e;
                    assertThat(rse.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
                    assertThat(rse.getReason()).isEqualTo("Avval botda akkauntingizni ulang");
                });
    }

    @Test
    @DisplayName("initData yaroqsiz yoki muddati o'tgan bo'lsa 401 qaytadi")
    void telegramLogin_invalidInitData_throws401() {
        String initData = "invalid_init_data";
        when(validator.validate(initData)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> controller.telegramLogin(new TelegramAuthController.TelegramAuthRequest(initData)))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(e -> {
                    ResponseStatusException rse = (ResponseStatusException) e;
                    assertThat(rse.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
                });
    }
}
