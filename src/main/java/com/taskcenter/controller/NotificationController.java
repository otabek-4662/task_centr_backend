package com.taskcenter.controller;

import com.taskcenter.dto.ApiResponse;
import com.taskcenter.dto.NotificationDto;
import com.taskcenter.model.User;
import com.taskcenter.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Notifications", description = "Foydalanuvchi bildirishnomalari")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @Operation(
            operationId = "getMyNotifications",
            summary = "Mening bildirishnomalarim ro'yxatini olish (Pageable)",
            description = "Joriy foydalanuvchiga tegishli barcha bildirishnomalarni (vazifa biriktirilishi, izohlar, eslatmalar) sahifalab qaytaradi."
    )
    @GetMapping
    public ApiResponse<Page<NotificationDto>> getMyNotifications(
            @org.springdoc.core.annotations.ParameterObject Pageable pageable,
            @AuthenticationPrincipal User currentUser) {
        return ApiResponse.success("ok", notificationService.getUserNotifications(currentUser, pageable));
    }

    @Operation(
            operationId = "markNotificationAsRead",
            summary = "Bildirishnomani o'qilgan deb belgilash",
            description = "Bitta bildirishnomaning o'qilganlik holatini (isRead = true) saqlaydi."
    )
    @PutMapping("/{id}/read")
    public ApiResponse<Void> markAsRead(
            @io.swagger.v3.oas.annotations.Parameter(description = "Bildirishnoma ID si", example = "notif-uuid-123")
            @PathVariable String id,
            @AuthenticationPrincipal User currentUser) {
        notificationService.markAsRead(id, currentUser);
        return ApiResponse.success("ok", null);
    }

    @Operation(
            operationId = "markAllNotificationsAsRead",
            summary = "Barcha o'qilmaganlarni o'qilgan deb belgilash",
            description = "Foydalanuvchining barcha o'qilmagan bildirishnomalarini bir vaqtning o'zida o'qilgan deb belgilaydi."
    )
    @PutMapping("/read-all")
    public ApiResponse<Void> markAllAsRead(
            @AuthenticationPrincipal User currentUser) {
        notificationService.markAllAsRead(currentUser);
        return ApiResponse.success("ok", null);
    }
}
