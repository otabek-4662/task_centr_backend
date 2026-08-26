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
@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @Operation(summary = "Register - token beradi", description = "Tokensiz ishlaydi. Body: {name, email, password}. Javob: AuthResponse.token (response.data.token) ni Copy -> Swagger Authorize 🔓 ga qo'ying. Misol email: test@test.uz")
    @PostMapping("/register")
    public ResponseEntity<ApiResponse<AuthResponse>> registerUser(@Valid @RequestBody RegisterRequest request) {
        AuthResponse response = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Muvaffaqiyatli ro'yxatdan o'tdingiz", response));
    }

    @Operation(summary = "Login - token beradi", description = "Tokensiz ishlaydi. Body: {email: 'xusanyusupov06@gmail.com', password: 'password123'} -> javobdagi 'token' ni Copy -> Authorize 🔓 -> paste. Keyin qolgan GET/POST/DELETE lar ishlaydi.")
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> authenticateUser(@Valid @RequestBody LoginRequest request) {
        AuthResponse response = authService.login(request);
        return ResponseEntity.ok(ApiResponse.success("Tizimga muvaffaqiyatli kirdingiz", response));
    }
}
