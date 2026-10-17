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

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
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
    @Mock
    private com.taskcenter.service.WorkspaceInvitationService workspaceInvitationService;

    private TelegramAuthController controller;

    @BeforeEach
    void setUp() {
        controller = new TelegramAuthController(validator, mapper, userRepository, tokenProvider, authService, workspaceInvitationService);
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
    @DisplayName("Foydalanuvchi bazada topilmasa avtomatik ro'yxatdan o'tadi va token qaytadi")
    void telegramLogin_userNotFound_autoRegistersAndReturnsToken() throws Exception {
        String initData = "valid_init_data";
        when(validator.validate(initData)).thenReturn(Optional.of(Map.of(
                "user", "{\"id\":99999,\"first_name\":\"Stranger\"}"
        )));
        when(userRepository.findByTelegramChatId(99999L)).thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
        when(tokenProvider.generateTokenFromUser(any(User.class))).thenReturn("jwt-token-new");
        when(authService.createRefreshToken(any())).thenReturn("refresh-token-new");

        ResponseEntity<ApiResponse<AuthResponse>> response = controller.telegramLogin(new TelegramAuthController.TelegramAuthRequest(initData));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().isSuccess()).isTrue();
        assertThat(response.getBody().getData().getToken()).isEqualTo("jwt-token-new");
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

    @Test
    @DisplayName("createPendingInvite initData va inviteToken to'g'ri bo'lsa botLink muvaffaqiyatli qaytadi")
    void createPendingInvite_valid_returnsBotLink() throws Exception {
        String initData = "valid_init_data";
        when(validator.validate(initData)).thenReturn(Optional.of(Map.of(
                "user", "{\"id\":55555,\"first_name\":\"TelegramGuest\"}"
        )));
        when(workspaceInvitationService.registerPendingTelegramChatInvite(55555L, "tok_123"))
                .thenReturn("https://t.me/test_bot?start=inv_tok_123");

        ResponseEntity<ApiResponse<TelegramAuthController.TelegramPendingInviteResponse>> response =
                controller.createPendingInvite(new TelegramAuthController.TelegramPendingInviteRequest(initData, "tok_123"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().isSuccess()).isTrue();
        assertThat(response.getBody().getData().botLink()).isEqualTo("https://t.me/test_bot?start=inv_tok_123");
    }

    @Test
    @DisplayName("createPendingInvite initData yaroqsiz bo'lsa 401 qaytadi")
    void createPendingInvite_invalidInitData_throws401() {
        String initData = "invalid_init_data";
        when(validator.validate(initData)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> controller.createPendingInvite(
                new TelegramAuthController.TelegramPendingInviteRequest(initData, "tok_123")))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(e -> {
                    ResponseStatusException rse = (ResponseStatusException) e;
                    assertThat(rse.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
                });
    }

    private static final String TEST_BOT_TOKEN = "123456789:ABCdefGhIJKlmNoPQRsTUVwxyZ";

    private String generateInitData(Map<String, String> params, String botToken) {
        Map<String, String> sortedParams = new TreeMap<>(params);
        String dataCheck = sortedParams.entrySet().stream()
                .map(e -> e.getKey() + "=" + e.getValue())
                .collect(Collectors.joining("\n"));

        byte[] secret = hmac("WebAppData".getBytes(StandardCharsets.UTF_8), botToken.getBytes(StandardCharsets.UTF_8));
        String hash = HexFormat.of().formatHex(hmac(secret, dataCheck.getBytes(StandardCharsets.UTF_8)));

        return sortedParams.entrySet().stream()
                .map(e -> e.getKey() + "=" + URLEncoder.encode(e.getValue(), StandardCharsets.UTF_8))
                .collect(Collectors.joining("&")) + "&hash=" + hash;
    }

    private static byte[] hmac(byte[] key, byte[] data) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(key, "HmacSHA256"));
            return mac.doFinal(data);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    @DisplayName("Qabul mezoni: haqiqiy initData bilan token olinadi")
    void telegramLogin_realValidator_validInitData_returnsToken() throws Exception {
        TelegramInitDataValidator realValidator = new TelegramInitDataValidator(TEST_BOT_TOKEN, 86400L);
        TelegramAuthController realController = new TelegramAuthController(
                realValidator, mapper, userRepository, tokenProvider, authService, workspaceInvitationService
        );

        Map<String, String> params = new LinkedHashMap<>();
        params.put("auth_date", String.valueOf(Instant.now().getEpochSecond() - 30));
        params.put("query_id", "AAHdF6IQAAAAAN0XohDhrOrc");
        params.put("user", "{\"id\":12345,\"first_name\":\"Bekmurod\",\"username\":\"bekmurod\"}");
        String validInitData = generateInitData(params, TEST_BOT_TOKEN);

        User user = User.builder().id("u-1").name("bek").fullName("Bekmurod").telegramChatId(12345L).build();
        when(userRepository.findByTelegramChatId(12345L)).thenReturn(Optional.of(user));
        when(tokenProvider.generateTokenFromUser(user)).thenReturn("jwt-token-real");
        when(authService.createRefreshToken("u-1")).thenReturn("refresh-token-real");

        ResponseEntity<ApiResponse<AuthResponse>> response = realController.telegramLogin(
                new TelegramAuthController.TelegramAuthRequest(validInitData)
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getData().getToken()).isEqualTo("jwt-token-real");
        assertThat(response.getBody().getData().getRefreshToken()).isEqualTo("refresh-token-real");
        assertThat(response.getBody().getData().getUser().getTelegramLinked()).isTrue();
    }

    @Test
    @DisplayName("Qabul mezoni: o'zgartirilgan initData 401 beradi")
    void telegramLogin_realValidator_tamperedInitData_throws401() {
        TelegramInitDataValidator realValidator = new TelegramInitDataValidator(TEST_BOT_TOKEN, 86400L);
        TelegramAuthController realController = new TelegramAuthController(
                realValidator, mapper, userRepository, tokenProvider, authService, workspaceInvitationService
        );

        Map<String, String> params = new LinkedHashMap<>();
        params.put("auth_date", String.valueOf(Instant.now().getEpochSecond() - 30));
        params.put("query_id", "AAHdF6IQAAAAAN0XohDhrOrc");
        params.put("user", "{\"id\":12345,\"first_name\":\"Bekmurod\"}");
        String validInitData = generateInitData(params, TEST_BOT_TOKEN);
        String tamperedInitData = validInitData.substring(0, validInitData.length() - 4) + "0000";

        assertThatThrownBy(() -> realController.telegramLogin(new TelegramAuthController.TelegramAuthRequest(tamperedInitData)))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(e -> {
                    ResponseStatusException rse = (ResponseStatusException) e;
                    assertThat(rse.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
                    assertThat(rse.getReason()).isEqualTo("Invalid or expired initData");
                });
    }

    @Test
    @DisplayName("Qabul mezoni: eskirgan auth_date 401 beradi")
    void telegramLogin_realValidator_expiredAuthDate_throws401() {
        TelegramInitDataValidator realValidator = new TelegramInitDataValidator(TEST_BOT_TOKEN, 86400L);
        TelegramAuthController realController = new TelegramAuthController(
                realValidator, mapper, userRepository, tokenProvider, authService, workspaceInvitationService
        );

        Map<String, String> params = new LinkedHashMap<>();
        // 25 soat oldin (maxAgeSec 24 soat / 86400s)
        params.put("auth_date", String.valueOf(Instant.now().getEpochSecond() - (25 * 3600)));
        params.put("query_id", "AAHdF6IQAAAAAN0XohDhrOrc");
        params.put("user", "{\"id\":12345,\"first_name\":\"Bekmurod\"}");
        String expiredInitData = generateInitData(params, TEST_BOT_TOKEN);

        assertThatThrownBy(() -> realController.telegramLogin(new TelegramAuthController.TelegramAuthRequest(expiredInitData)))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(e -> {
                    ResponseStatusException rse = (ResponseStatusException) e;
                    assertThat(rse.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
                    assertThat(rse.getReason()).isEqualTo("Invalid or expired initData");
                });
    }

    @Test
    @DisplayName("Qabul mezoni: foydalanuvchi bazada yo'q bo'lsa avtomatik ro'yxatdan o'tib token beradi")
    void telegramLogin_realValidator_userNotLinked_autoRegisters() throws Exception {
        TelegramInitDataValidator realValidator = new TelegramInitDataValidator(TEST_BOT_TOKEN, 86400L);
        TelegramAuthController realController = new TelegramAuthController(
                realValidator, mapper, userRepository, tokenProvider, authService, workspaceInvitationService
        );

        Map<String, String> params = new LinkedHashMap<>();
        params.put("auth_date", String.valueOf(Instant.now().getEpochSecond() - 30));
        params.put("query_id", "AAHdF6IQAAAAAN0XohDhrOrc");
        params.put("user", "{\"id\":88888,\"first_name\":\"UnlinkedUser\"}");
        String validInitData = generateInitData(params, TEST_BOT_TOKEN);

        when(userRepository.findByTelegramChatId(88888L)).thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
        when(tokenProvider.generateTokenFromUser(any(User.class))).thenReturn("jwt-token-real");
        when(authService.createRefreshToken(any())).thenReturn("refresh-token-real");

        ResponseEntity<ApiResponse<AuthResponse>> response = realController.telegramLogin(new TelegramAuthController.TelegramAuthRequest(validInitData));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().isSuccess()).isTrue();
        assertThat(response.getBody().getData().getToken()).isEqualTo("jwt-token-real");
    }
}
