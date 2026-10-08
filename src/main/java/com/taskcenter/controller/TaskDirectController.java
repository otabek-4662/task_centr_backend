package com.taskcenter.controller;

import com.taskcenter.dto.ApiResponse;
import com.taskcenter.dto.TaskDto;
import com.taskcenter.model.User;
import com.taskcenter.service.TaskService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Deprecated
@Tag(name = "Tasks Direct", description = "Vazifalar bilan bevosita ishlash (eski/dublikat API lar)")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/tasks")
public class TaskDirectController {

    private final TaskService taskService;

    public TaskDirectController(TaskService taskService) {
        this.taskService = taskService;
    }

    @Deprecated
    @Operation(
            operationId = "getTaskDirectById",
            summary = "Vazifani bevosita ID orqali olish (eski/dublikat endpoint)",
            description = "Eski dublikat endpoint. Yangi integratsiyalar va tavsiya etilgan asosiy yo'l: GET /api/workspaces/{workspaceId}/tasks/{id}. Ushbu endpoint workspaceId bilmagan holatlar uchun vaqtinchalik saqlanmoqda.",
            deprecated = true
    )
    @GetMapping("/{id}")
    public ApiResponse<TaskDto> getTaskDirectById(
            @io.swagger.v3.oas.annotations.Parameter(description = "Vazifa (task) ID si", example = "task-uuid-123")
            @PathVariable String id,
            @AuthenticationPrincipal User currentUser) {
        TaskDto task = taskService.getTaskDirectById(id, currentUser);
        return ApiResponse.success("ok", task);
    }

    @Operation(
            operationId = "getTaskDetails",
            summary = "Vazifa to'liq ma'lumotlarini olish",
            description = "Vazifa, uning izohlari va checklist elementlarini qaytaradi."
    )
    @GetMapping("/{id}/details")
    public ApiResponse<com.taskcenter.dto.TaskDetailsDto> getTaskDetails(
            @io.swagger.v3.oas.annotations.Parameter(description = "Vazifa (task) ID si", example = "task-uuid-123")
            @PathVariable String id,
            @AuthenticationPrincipal User currentUser) {
        com.taskcenter.dto.TaskDetailsDto details = taskService.getTaskDetails(id, currentUser);
        return ApiResponse.success("ok", details);
    }
}
