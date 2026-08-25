package com.taskcenter.controller;

import com.taskcenter.dto.ApiResponse;
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
@CrossOrigin(origins = "*")
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

    @Operation(summary = "Foydalanuvchilar ro'yxatini olish (workspaceId bo'yicha filter qilish mumkin)")
    @GetMapping("/users")
    public ApiResponse<Page<UserDto>> getUsers(
            @RequestParam(required = false) String workspaceId,
            @AuthenticationPrincipal User currentUser,
            @PageableDefault(size = 20, sort = "name") Pageable pageable) {
        Page<UserDto> users = userService.getUsers(workspaceId, currentUser, pageable);
        return ApiResponse.success("ok", users);
    }
}
