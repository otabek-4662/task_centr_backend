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

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;
import com.taskcenter.service.TelegramMessageEvent;
import com.taskcenter.util.TelegramUtil;
import java.util.UUID;

import java.time.LocalDateTime;
import java.util.Map;

@Tag(name = "Telegram Auth", description = "Telegram Mini App autentifikatsiyasi API lari. Tavsiya etilgan usul: POST /api/auth/telegram orqali JWT token olish.")
@RestController
@RequestMapping({"/api/auth", "/api/v1/auth"})
public class TelegramAuthController {

    private final TelegramInitDataValidator validator;
    private final ObjectMapper mapper;
    private final UserRepository userRepository;
    private final JwtTokenProvider tokenProvider;
    private final AuthService authService;
    private final com.taskcenter.service.WorkspaceInvitationService workspaceInvitationService;
    private final PasswordEncoder passwordEncoder;
    private final ApplicationEventPublisher eventPublisher;

    @org.springframework.beans.factory.annotation.Autowired
    public TelegramAuthController(TelegramInitDataValidator validator,
                                  ObjectMapper mapper,
                                  UserRepository userRepository,
                                  JwtTokenProvider tokenProvider,
                                  AuthService authService,
                                  com.taskcenter.service.WorkspaceInvitationService workspaceInvitationService,
                                  PasswordEncoder passwordEncoder,
                                  ApplicationEventPublisher eventPublisher) {
        this.validator = validator;
        this.mapper = mapper;
        this.userRepository = userRepository;
        this.tokenProvider = tokenProvider;
        this.authService = authService;
        this.workspaceInvitationService = workspaceInvitationService;
        this.passwordEncoder = passwordEncoder;
        this.eventPublisher = eventPublisher;
    }

    public TelegramAuthController(TelegramInitDataValidator validator,
                                  ObjectMapper mapper,
                                  UserRepository userRepository,
                                  JwtTokenProvider tokenProvider,
                                  AuthService authService,
                                  com.taskcenter.service.WorkspaceInvitationService workspaceInvitationService) {
        this(validator, mapper, userRepository, tokenProvider, authService, workspaceInvitationService, null, null);
    }

    public record TelegramAuthRequest(
            @io.swagger.v3.oas.annotations.media.Schema(description = "Telegram WebApp initData satri (window.Telegram.WebApp.initData)", example = "query_id=AAHd...&user=%7B%22id%22%3A123%7D&auth_date=1620000000&hash=...")
            @NotBlank String initData) {}

    public record TelegramPendingInviteRequest(
            @io.swagger.v3.oas.annotations.media.Schema(description = "Telegram WebApp initData satri", example = "query_id=AAHd...&user=%7B%22id%22%3A123%7D&auth_date=1620000000&hash=...")
            @NotBlank String initData,
            @io.swagger.v3.oas.annotations.media.Schema(description = "Taklif tokeni (masalan: raw token yoki inv_<token>)", example = "pP3aK_91xL-w7Q3zD5eFg8hIjKlMnOpQrStUvWxYz01")
            @NotBlank String inviteToken) {}

    public record TelegramPendingInviteResponse(
            @io.swagger.v3.oas.annotations.media.Schema(description = "Telegram botga yo'naltiruvchi havola", example = "https://t.me/task_center_bot?start=inv_pP3aK_91xL-w7Q3zD5eFg8hIjKlMnOpQrStUvWxYz01")
            String botLink) {}

    @Operation(
            operationId = "loginWithTelegram",
            summary = "Telegram Mini App orqali login (Tavsiya etilgan usul)",
            description = "Frontend Telegram Mini App ochilganda `window.Telegram.WebApp.initData` ni ushbu endpointga yuborib JWT token oladi va keyingi barcha so'rovlarda `Authorization: Bearer <token>` headerini ishlatadi. "
                    + "Foydalanuvchi tizimda topilsa JWT token qaytariladi. Agar foydalanuvchi akkauntini botga ulamagan bo'lsa, xavfsizlik maqsadida avtomatik hisob ochilmaydi va 401 'Avval botda akkauntingizni ulang' xatosi qaytariladi."
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Muvaffaqiyatli kirish — standart login bilan bir xil JWT token va foydalanuvchi ma'lumotlari qaytariladi",
                    content = @io.swagger.v3.oas.annotations.media.Content(
                            mediaType = "application/json",
                            schema = @io.swagger.v3.oas.annotations.media.Schema(implementation = ApiResponse.class),
                            examples = @io.swagger.v3.oas.annotations.media.ExampleObject(
                                    name = "SuccessResponse",
                                    value = "{\"success\":true,\"message\":\"Tizimga muvaffaqiyatli kirdingiz\",\"data\":{\"token\":\"eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...\",\"refreshToken\":\"d8e3b8a1-7c2e-4b6a-9f1a-8c3d5e7f9a0b\",\"user\":{\"id\":\"u-123456\",\"name\":\"bekmurod\",\"fullName\":\"Bekmurod\",\"telegramLinked\":true}},\"timestamp\":\"2026-10-12T14:40:00Z\"}"
                            )
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "Validatsiya xatosi (VALIDATION_ERROR) — initData bo'sh bo'lganda",
                    content = @io.swagger.v3.oas.annotations.media.Content(
                            mediaType = "application/json",
                            schema = @io.swagger.v3.oas.annotations.media.Schema(implementation = com.taskcenter.dto.ApiError.class)
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "Yaroqsiz/muddati o'tgan initData yoki akkaunt botga ulanmagan",
                    content = @io.swagger.v3.oas.annotations.media.Content(
                            mediaType = "application/json",
                            schema = @io.swagger.v3.oas.annotations.media.Schema(implementation = com.taskcenter.dto.ApiError.class),
                            examples = {
                                    @io.swagger.v3.oas.annotations.media.ExampleObject(
                                            name = "InvalidInitData",
                                            summary = "HMAC buzilgan yoki auth_date 24 soatdan oshgan",
                                            value = "{\"timestamp\":\"2026-10-12T14:40:00Z\",\"status\":401,\"error\":\"UNAUTHORIZED\",\"message\":\"Invalid or expired initData\",\"path\":\"/api/auth/telegram\"}"
                                    ),
                                    @io.swagger.v3.oas.annotations.media.ExampleObject(
                                            name = "UserNotLinked",
                                            summary = "Telegram foydalanuvchi tizimda topilmadi",
                                            value = "{\"timestamp\":\"2026-10-12T14:40:00Z\",\"status\":401,\"error\":\"UNAUTHORIZED\",\"message\":\"Avval botda akkauntingizni ulang\",\"path\":\"/api/auth/telegram\"}"
                                    )
                            }
                    )
            )
    })
    @PostMapping("/telegram")
    public ResponseEntity<ApiResponse<AuthResponse>> telegramLogin(@Valid @RequestBody TelegramAuthRequest req) throws Exception {
        Map<String, String> params = validator.validate(req.initData())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid or expired initData"));

        JsonNode user = mapper.readTree(params.get("user"));
        long telegramId = user.get("id").asLong();

        User u = userRepository.findByTelegramChatId(telegramId).orElse(null);
        if (u == null) {
            String username = user.has("username") && !user.get("username").isNull()
                    ? user.get("username").asText().trim() : null;
            String firstName = user.has("first_name") && !user.get("first_name").isNull()
                    ? user.get("first_name").asText().trim() : "";
            String lastName = user.has("last_name") && !user.get("last_name").isNull()
                    ? user.get("last_name").asText().trim() : "";

            String candidateName = (username != null && !username.isBlank())
                    ? username.toLowerCase()
                    : "tg_" + telegramId;

            if (userRepository.existsByName(candidateName)) {
                candidateName = candidateName + "_" + (telegramId % 10000);
            }

            String fullName = (firstName + " " + lastName).trim();
            if (fullName.isBlank()) {
                fullName = (username != null ? username : candidateName);
            }

            String rawPass = UUID.randomUUID().toString();
            String encodedPass = passwordEncoder != null ? passwordEncoder.encode(rawPass) : rawPass;

            User newUser = User.builder()
                    .name(candidateName)
                    .fullName(fullName)
                    .email("tg_" + telegramId + "@taskcenter.local")
                    .password(encodedPass)
                    .telegramChatId(telegramId)
                    .role(User.Role.USER)
                    .lastSeenAt(LocalDateTime.now())
                    .build();
            User savedUser = userRepository.save(newUser);
            u = (savedUser != null) ? savedUser : newUser;

            if (workspaceInvitationService != null) {
                String pendingToken = workspaceInvitationService.getPendingChatInvite(telegramId);
                if (pendingToken != null) {
                    try {
                        workspaceInvitationService.acceptInvitation(pendingToken, u);
                        workspaceInvitationService.removePendingChatInvite(telegramId);
                    } catch (Exception ignored) {}
                }
            }

            if (eventPublisher != null) {
                String welcomeMsg = "🎉 Davraga xush kelibsiz, <b>" + TelegramUtil.escapeHtml(fullName) + "</b>!\n\n"
                        + "Telegram Mini App orqali hisobingiz muvaffaqiyatli yaratildi va botga ulandi ☕️\n"
                        + "Endi barcha vazifalaringizni shu yerdan va Mini App orqali qulay boshqara olasiz!";
                eventPublisher.publishEvent(new TelegramMessageEvent(telegramId, welcomeMsg));
            }
        }

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
