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

    public TelegramAuthController(TelegramInitDataValidator validator,
                                  ObjectMapper mapper,
                                  UserRepository userRepository,
                                  JwtTokenProvider tokenProvider,
                                  AuthService authService) {
        this.validator = validator;
        this.mapper = mapper;
        this.userRepository = userRepository;
        this.tokenProvider = tokenProvider;
        this.authService = authService;
    }

    public record TelegramAuthRequest(@NotBlank String initData) {}

    @Operation(summary = "Telegram Mini App orqali login")
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
}
