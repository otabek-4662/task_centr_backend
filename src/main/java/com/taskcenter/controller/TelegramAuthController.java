package com.taskcenter.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.taskcenter.dto.ApiResponse;
import com.taskcenter.dto.AuthResponse;
import com.taskcenter.model.User;
import com.taskcenter.repository.UserRepository;
import com.taskcenter.security.JwtTokenProvider;
import com.taskcenter.security.TelegramInitDataValidator;
import com.taskcenter.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.Map;

@Tag(name = "Telegram Auth", description = "Telegram Mini App autentifikatsiyasi")
@RestController
@RequestMapping("/api/v1/auth")
public class TelegramAuthController {

    private final TelegramInitDataValidator validator;
    private final ObjectMapper mapper;
    private final UserRepository userRepository;
    private final JwtTokenProvider tokenProvider;
    private final AuthService authService;
    private final com.taskcenter.service.WorkspaceInvitationService workspaceInvitationService;

    public TelegramAuthController(TelegramInitDataValidator validator,
                                  ObjectMapper mapper,
                                  UserRepository userRepository,
                                  JwtTokenProvider tokenProvider,
                                  AuthService authService,
                                  com.taskcenter.service.WorkspaceInvitationService workspaceInvitationService) {
        this.validator = validator;
        this.mapper = mapper;
        this.userRepository = userRepository;
        this.tokenProvider = tokenProvider;
        this.authService = authService;
        this.workspaceInvitationService = workspaceInvitationService;
    }

    public record TelegramAuthRequest(
            @io.swagger.v3.oas.annotations.media.Schema(description = "Telegram WebApp initData satri", example = "query_id=AAHd...&user=%7B%22id%22%3A123%7D&auth_date=1620000000&hash=...")
            @NotBlank String initData) {}

    public record TelegramPendingInviteRequest(
            @io.swagger.v3.oas.annotations.media.Schema(description = "Telegram WebApp initData satri", example = "query_id=AAHd...&user=%7B%22id%22%3A123%7D&auth_date=1620000000&hash=...")
            @NotBlank String initData,
            @io.swagger.v3.oas.annotations.media.Schema(description = "Taklif tokeni (masalan: raw token yoki inv_<token>)", example = "pP3aK_91xL-w7Q3zD5eFg8hIjKlMnOpQrStUvWxYz01")
            @NotBlank String inviteToken) {}

    public record TelegramPendingInviteResponse(
            @io.swagger.v3.oas.annotations.media.Schema(description = "Telegram botga yo'naltiruvchi havola", example = "https://t.me/task_center_bot?start=inv_pP3aK_91xL-w7Q3zD5eFg8hIjKlMnOpQrStUvWxYz01")
            String botLink) {}

    @Operation(operationId = "loginWithTelegram", summary = "Telegram Mini App orqali login",
            description = "Telegram WebApp tomonidan berilgan initData HMAC-SHA256 imzosini va auth_date muddatini tekshirib, tizimga ulangan foydalanuvchiga JWT token va refresh token beradi.")
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Muvaffaqiyatli kirish"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Validatsiya xatosi (VALIDATION_ERROR)"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Yaroqsiz initData yoki bot ulanmagan (UNAUTHORIZED)")
    })
    @PostMapping("/telegram")
    public ResponseEntity<ApiResponse<AuthResponse>> telegramLogin(@Valid @RequestBody TelegramAuthRequest req) throws Exception {
        Map<String, String> params = validator.validate(req.initData())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid or expired initData"));

        JsonNode user = mapper.readTree(params.get("user"));
        long telegramId = user.get("id").asLong();

        User u = userRepository.findByTelegramChatId(telegramId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                        "Avval botda akkauntingizni ulang"));

        u.setLastSeenAt(LocalDateTime.now());
        userRepository.save(u);

        String token = tokenProvider.generateTokenFromUser(u);
        String refreshToken = authService.createRefreshToken(u.getId());
        AuthResponse response = new AuthResponse(token, refreshToken, AuthResponse.UserDto.fromEntity(u));

        return ResponseEntity.ok(ApiResponse.success("Tizimga muvaffaqiyatli kirdingiz", response));
    }

    @Operation(operationId = "createPendingTelegramInvite", summary = "Mini App ulanmagan foydalanuvchi uchun kutilayotgan taklif yaratish",
            description = "initData HMAC va auth_date ni tekshiradi, inviteToken faolligini tekshiradi va telegram_pending_chat_invites ga 24 soatlik yozuv qo'shib botLink qaytaradi.")
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Muvaffaqiyatli saqlandi va botLink qaytarildi"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Taklif muddati o'tgan, bekor qilingan yoki limit to'lgan"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Yaroqsiz initData (UNAUTHORIZED)"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Taklif topilmadi (INVITE_NOT_FOUND)"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "429", description = "So'rovlar soni me'yordan oshdi (RATE_LIMITED)")
    })
    @PostMapping("/telegram/pending-invite")
    public ResponseEntity<ApiResponse<TelegramPendingInviteResponse>> createPendingInvite(
            @Valid @RequestBody TelegramPendingInviteRequest req) throws Exception {
        Map<String, String> params = validator.validate(req.initData())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid or expired initData"));

        JsonNode userNode = mapper.readTree(params.get("user"));
        if (userNode == null || !userNode.has("id")) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid initData user");
        }
        long telegramUserId = userNode.get("id").asLong();

        String botLink = workspaceInvitationService.registerPendingTelegramChatInvite(telegramUserId, req.inviteToken());
        return ResponseEntity.ok(ApiResponse.success("ok", new TelegramPendingInviteResponse(botLink)));
    }
}
