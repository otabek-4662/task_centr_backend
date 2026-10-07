package com.taskcenter.controller;

import com.taskcenter.dto.ApiResponse;
import com.taskcenter.dto.TelegramLinkDto;
import com.taskcenter.dto.UserDto;
import com.taskcenter.model.User;
import com.taskcenter.service.UserService;
import com.taskcenter.security.RateLimitingService;
import com.taskcenter.exception.RateLimitException;
import io.swagger.v3.oas.annotations.Hidden;
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
    private final RateLimitingService rateLimitingService;
    private final com.taskcenter.service.TaskService taskService;

    @org.springframework.beans.factory.annotation.Value("${ratelimit.telegram-link.user.max:10}")
    private int telegramLinkUserMax;
    @org.springframework.beans.factory.annotation.Value("${ratelimit.telegram-link.user.window-minutes:1}")
    private int telegramLinkUserWindow;

    public UserController(UserService userService, RateLimitingService rateLimitingService, com.taskcenter.service.TaskService taskService) {
        this.userService = userService;
        this.rateLimitingService = rateLimitingService;
        this.taskService = taskService;
    }

    @Operation(summary = "Joriy foydalanuvchi profilini olish")
    @GetMapping("/me")
    public ApiResponse<UserDto> getMe(@AuthenticationPrincipal User currentUser) {
        return ApiResponse.success("ok", userService.getCurrentUser(currentUser));
    }

    @Hidden
    @GetMapping("/auth/me")
    public ApiResponse<UserDto> getAuthMe(@AuthenticationPrincipal User currentUser) {
        return getMe(currentUser);
    }

    @Operation(summary = "Joriy foydalanuvchiga biriktirilgan barcha vazifalar ro'yxatini olish (barcha workspacelar bo'yicha)")
    @GetMapping("/users/me/tasks")
    public ApiResponse<Page<com.taskcenter.dto.TaskDto>> getMyTasks(
            @AuthenticationPrincipal User currentUser,
            @org.springdoc.core.annotations.ParameterObject @ModelAttribute com.taskcenter.dto.TaskFilterRequest filter,
            @org.springdoc.core.annotations.ParameterObject @PageableDefault(size = 20, sort = "createdAt", direction = org.springframework.data.domain.Sort.Direction.DESC) Pageable pageable) {
        Page<com.taskcenter.dto.TaskDto> tasks = taskService.getMyTasks(currentUser, filter, pageable);
        return ApiResponse.success("ok", tasks);
    }

    @Hidden
    @GetMapping("/me/tasks")
    public ApiResponse<Page<com.taskcenter.dto.TaskDto>> getMyTasksAlias(
            @AuthenticationPrincipal User currentUser,
            @org.springdoc.core.annotations.ParameterObject @ModelAttribute com.taskcenter.dto.TaskFilterRequest filter,
            @org.springdoc.core.annotations.ParameterObject @PageableDefault(size = 20, sort = "createdAt", direction = org.springframework.data.domain.Sort.Direction.DESC) Pageable pageable) {
        return getMyTasks(currentUser, filter, pageable);
    }

    @Operation(summary = "Workspace dagi foydalanuvchilar ro'yxatini olish")
    @GetMapping("/users")
    public ApiResponse<Page<UserDto>> getUsers(
            @RequestParam String workspaceId,
            @AuthenticationPrincipal User currentUser,
            @org.springdoc.core.annotations.ParameterObject @PageableDefault(size = 20, sort = "name") Pageable pageable) {
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

    public record TelegramLinkRequest(@jakarta.validation.constraints.NotBlank String initData) {}

    @Operation(summary = "Mini App orqali Telegram akkauntni ulash")
    @PostMapping("/users/me/telegram")
    public ApiResponse<UserDto> linkTelegramViaMiniApp(
            @AuthenticationPrincipal User currentUser,
            @jakarta.validation.Valid @RequestBody TelegramLinkRequest request) {
        long waitTime = rateLimitingService.tryConsumeUserLimit("telegram-link", currentUser.getId(), telegramLinkUserMax, telegramLinkUserWindow);
        if (waitTime > 0) {
            throw new RateLimitException(waitTime);
        }
        return ApiResponse.success("Telegram muvaffaqiyatli ulandi", userService.linkTelegramViaInitData(currentUser, request.initData()));
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
