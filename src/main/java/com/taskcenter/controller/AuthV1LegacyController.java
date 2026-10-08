package com.taskcenter.controller;

import com.taskcenter.dto.*;
import com.taskcenter.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Deprecated
@Tag(name = "Auth", description = "Token olish — authsiz, tokensiz faqat shu 2 ta ishlaydi")
@RestController
@RequestMapping("/api/v1/auth")
public class AuthV1LegacyController {

    private final AuthService authService;

    public AuthV1LegacyController(AuthService authService) {
        this.authService = authService;
    }

    @Deprecated
    @Operation(operationId = "v1RegisterUser", summary = "Register (eski v1 endpoint)",
            description = "Eski dublikat endpoint. Yangi loyihalar va frontend uchun POST /api/auth/register dan foydalaning.",
            deprecated = true)
    @PostMapping("/register")
    public ResponseEntity<ApiResponse<AuthResponse>> registerUser(@Valid @RequestBody RegisterRequest request) {
        AuthResponse response = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Muvaffaqiyatli ro'yxatdan o'tdingiz", response));
    }

    @Deprecated
    @Operation(operationId = "v1LoginUser", summary = "Login (eski v1 endpoint)",
            description = "Eski dublikat endpoint. Yangi loyihalar va frontend uchun POST /api/auth/login dan foydalaning.",
            deprecated = true)
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> authenticateUser(@Valid @RequestBody LoginRequest request) {
        AuthResponse response = authService.login(request);
        return ResponseEntity.ok(ApiResponse.success("Tizimga muvaffaqiyatli kirdingiz", response));
    }

    @Deprecated
    @Operation(operationId = "v1RefreshToken", summary = "Refresh token (eski v1 endpoint)",
            description = "Eski dublikat endpoint. Yangi loyihalar va frontend uchun POST /api/auth/refresh dan foydalaning.",
            deprecated = true)
    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<TokenRefreshResponse>> refreshUser(@Valid @RequestBody TokenRefreshRequest request) {
        TokenRefreshResponse response = authService.refresh(request);
        return ResponseEntity.ok(ApiResponse.success("Token muvaffaqiyatli yangilandi", response));
    }

    @Deprecated
    @Operation(operationId = "v1LogoutUser", summary = "Logout (eski v1 endpoint)",
            description = "Eski dublikat endpoint. Yangi loyihalar va frontend uchun POST /api/auth/logout dan foydalaning.",
            deprecated = true)
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(@Valid @RequestBody TokenRefreshRequest request) {
        authService.logout(request.getRefreshToken());
        return ResponseEntity.ok(ApiResponse.success("Muvaffaqiyatli tizimdan chiqildi", null));
    }

    @Deprecated
    @Operation(operationId = "v1ForgotPassword", summary = "Parolni tiklash so'rovi (eski v1 endpoint)",
            description = "Eski dublikat endpoint. Yangi loyihalar va frontend uchun POST /api/auth/forgot-password dan foydalaning.",
            deprecated = true)
    @PostMapping("/forgot-password")
    public ResponseEntity<ApiResponse<Void>> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        authService.forgotPassword(request);
        return ResponseEntity.ok(ApiResponse.success("Agar ushbu email ro'yxatdan o'tgan bo'lsa, quloqqa aytiladigan so'zni tiklash havolasi yuborildi", null));
    }

    @Deprecated
    @Operation(operationId = "v1ResetPassword", summary = "Yangi parol o'rnatish (eski v1 endpoint)",
            description = "Eski dublikat endpoint. Yangi loyihalar va frontend uchun POST /api/auth/reset-password dan foydalaning.",
            deprecated = true)
    @PostMapping("/reset-password")
    public ResponseEntity<ApiResponse<Void>> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        authService.resetPassword(request);
        return ResponseEntity.ok(ApiResponse.success("Quloqqa aytiladigan so'z muvaffaqiyatli yangilandi. Endi yangi so'z bilan tizimga kirishingiz mumkin", null));
    }
}
