package com.taskcenter.controller;

import com.taskcenter.dto.ApiResponse;
import com.taskcenter.dto.TelegramLinkDto;
import com.taskcenter.dto.UserDto;
import com.taskcenter.model.User;
import com.taskcenter.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Users", description = "Foydalanuvchilar ma'lumotlari bilan ishlash API lari")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @Operation(summary = "Joriy foydalanuvchi profilini olish")
    @GetMapping("/me")
    public ApiResponse<UserDto> getMe(@AuthenticationPrincipal User currentUser) {
        return ApiResponse.success("ok", userService.getCurrentUser(currentUser));
    }

    @Operation(summary = "Joriy foydalanuvchi profilini olish (alias)")
    @GetMapping("/auth/me")
    public ApiResponse<UserDto> getAuthMe(@AuthenticationPrincipal User currentUser) {
        return getMe(currentUser);
    }

    @Operation(summary = "Workspace dagi foydalanuvchilar ro'yxatini olish")
    @GetMapping("/users")
    public ApiResponse<Page<UserDto>> getUsers(
            @RequestParam String workspaceId,
            @AuthenticationPrincipal User currentUser,
            @PageableDefault(size = 20, sort = "name") Pageable pageable) {
        Page<UserDto> users = userService.getUsers(workspaceId, currentUser, pageable);
        return ApiResponse.success("ok", users);
    }

    @Operation(summary = "Telegram bilan ulash uchun vaqtinchalik token, tayyor havola va amal qilish muddatini olish")
    @GetMapping("/users/me/telegram-link-token")
    public ApiResponse<TelegramLinkDto> getTelegramLinkToken(@AuthenticationPrincipal User currentUser) {
        return ApiResponse.success("ok", userService.createTelegramLink(currentUser));
    }

    @Operation(summary = "Telegram akkauntni uzish (faqat joriy foydalanuvchi uchun)")
    @DeleteMapping("/users/me/telegram")
    public ApiResponse<Void> unlinkTelegram(@AuthenticationPrincipal User currentUser) {
        userService.unlinkTelegram(currentUser);
        return ApiResponse.success("Telegram akkaunt uzildi", null);
    }

    @Operation(summary = "Profil ma'lumotlarini tahrirlash", description = "Foydalanuvchi to'liq ismi yoki login nomini yangilash.")
    @PatchMapping("/users/me")
    public ApiResponse<UserDto> updateProfile(
            @AuthenticationPrincipal User currentUser,
            @jakarta.validation.Valid @RequestBody com.taskcenter.dto.UpdateProfileRequest request) {
        UserDto updated = userService.updateProfile(currentUser, request);
        return ApiResponse.success("Profil muvaffaqiyatli yangilandi", updated);
    }

    @Operation(summary = "Parolni o'zgartirish", description = "Eski parolni tekshirib yangi parol o'rnatish.")
    @PostMapping("/users/me/change-password")
    public ApiResponse<Void> changePassword(
            @AuthenticationPrincipal User currentUser,
            @jakarta.validation.Valid @RequestBody com.taskcenter.dto.ChangePasswordRequest request) {
        userService.changePassword(currentUser, request);
        return ApiResponse.success("Quloqqa aytiladigan so'z muvaffaqiyatli o'zgartirildi", null);
    }
}
