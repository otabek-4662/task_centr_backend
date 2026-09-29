package com.taskcenter.controller;

import com.taskcenter.dto.ApiResponse;
import com.taskcenter.dto.AuthResponse;
import com.taskcenter.dto.LoginRequest;
import com.taskcenter.dto.RegisterRequest;
import com.taskcenter.service.AuthService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Auth", description = "Token olish — authsiz, tokensiz faqat shu 2 ta ishlaydi")
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @Operation(summary = "Register - token beradi", description = "Body: {name, password}. Token 7 kun amal qiladi.")
    @PostMapping("/register")
    public ResponseEntity<ApiResponse<AuthResponse>> registerUser(@Valid @RequestBody RegisterRequest request) {
        AuthResponse response = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Muvaffaqiyatli ro'yxatdan o'tdingiz", response));
    }

    @Operation(summary = "Login - token beradi", description = "Body: {name, password}. Token ni Swagger Authorize 🔓 ga qo'ying.")
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> authenticateUser(@Valid @RequestBody LoginRequest request) {
        AuthResponse response = authService.login(request);
        return ResponseEntity.ok(ApiResponse.success("Tizimga muvaffaqiyatli kirdingiz", response));
    }

    @Operation(summary = "Refresh - yangi token beradi", description = "Muddati tugagan token o'rniga yangisini olish. Body: {refreshToken}")
    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<com.taskcenter.dto.TokenRefreshResponse>> refreshUser(@Valid @RequestBody com.taskcenter.dto.TokenRefreshRequest request) {
        com.taskcenter.dto.TokenRefreshResponse response = authService.refresh(request);
        return ResponseEntity.ok(ApiResponse.success("Token muvaffaqiyatli yangilandi", response));
    }

    @Operation(summary = "Parolni tiklash so'rovi (Email orqali havola yuborish)", description = "Body: {email}. Brevo orqali emailingizga tiklash havolasi yuboriladi.")
    @PostMapping("/forgot-password")
    public ResponseEntity<ApiResponse<Void>> forgotPassword(@Valid @RequestBody com.taskcenter.dto.ForgotPasswordRequest request) {
        authService.forgotPassword(request);
        return ResponseEntity.ok(ApiResponse.success("Agar ushbu email ro'yxatdan o'tgan bo'lsa, quloqqa aytiladigan so'zni tiklash havolasi yuborildi", null));
    }

    @Operation(summary = "Yangi parol o'rnatish", description = "Body: {token, newPassword}. Emailga kelgan token orqali yangi parol o'rnatiladi.")
    @PostMapping("/reset-password")
    public ResponseEntity<ApiResponse<Void>> resetPassword(@Valid @RequestBody com.taskcenter.dto.ResetPasswordRequest request) {
        authService.resetPassword(request);
        return ResponseEntity.ok(ApiResponse.success("Quloqqa aytiladigan so'z muvaffaqiyatli yangilandi. Endi yangi so'z bilan tizimga kirishingiz mumkin", null));
    }
}
