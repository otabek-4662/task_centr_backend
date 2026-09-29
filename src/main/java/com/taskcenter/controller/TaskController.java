package com.taskcenter.controller;

import com.taskcenter.dto.ApiResponse;
import com.taskcenter.dto.TaskCreateRequest;
import com.taskcenter.dto.TaskDto;
import com.taskcenter.dto.TaskUpdateRequest;
import com.taskcenter.model.User;
import com.taskcenter.service.TaskService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;

import java.util.List;

@Tag(name = "Tasks", description = "Vazifalar (Tasks) bilan ishlash API lari")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/workspaces/{workspaceId}/tasks")
public class TaskController {

    private final TaskService taskService;

    public TaskController(TaskService taskService) {
        this.taskService = taskService;
    }

    @Operation(summary = "Workspace ga tegishli vazifalar ro'yxatini olish (qidiruv, filter va pagination bilan)")
    @GetMapping
    public ApiResponse<Page<TaskDto>> getTasks(
            @PathVariable String workspaceId,
            @AuthenticationPrincipal User currentUser,
            @org.springdoc.core.annotations.ParameterObject @ModelAttribute com.taskcenter.dto.TaskFilterRequest filter,
            @org.springdoc.core.annotations.ParameterObject @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        Page<TaskDto> tasks = taskService.getTasksByWorkspace(workspaceId, currentUser, filter, pageable);
        return ApiResponse.success("ok", tasks);
    }

    @Operation(summary = "Bitta vazifani ID orqali olish (batafsil ma'lumotlari bilan)")
    @GetMapping("/{id}")
    public ApiResponse<TaskDto> getTaskById(
            @PathVariable String workspaceId,
            @PathVariable String id,
            @AuthenticationPrincipal User currentUser) {
        TaskDto task = taskService.getTaskById(workspaceId, id, currentUser);
        return ApiResponse.success("ok", task);
    }

    @Operation(summary = "Yangi vazifa yaratish")
    @PostMapping
    public ApiResponse<TaskDto> createTask(
            @PathVariable String workspaceId,
            @Valid @RequestBody TaskCreateRequest request,
            @AuthenticationPrincipal User currentUser) {
        TaskDto task = taskService.createTask(workspaceId, request, currentUser);
        return ApiResponse.success("Bosh og'riq yaratildi", task);
    }

    @Operation(summary = "Vazifani to'liq yangilash")
    @PutMapping("/{id}")
    public ApiResponse<TaskDto> updateTask(
            @PathVariable String workspaceId,
            @PathVariable String id,
            @RequestBody TaskUpdateRequest request,
            @AuthenticationPrincipal User currentUser) {
        TaskDto task = taskService.updateTask(workspaceId, id, request, currentUser);
        return ApiResponse.success("Bosh og'riq yangilandi", task);
    }

    @Operation(summary = "Vazifani qisman yangilash")
    @PatchMapping("/{id}")
    public ApiResponse<TaskDto> patchTask(
            @PathVariable String workspaceId,
            @PathVariable String id,
            @RequestBody TaskUpdateRequest request,
            @AuthenticationPrincipal User currentUser) {
        TaskDto task = taskService.updateTask(workspaceId, id, request, currentUser);
        return ApiResponse.success("Task yangilandi", task);
    }

    @Operation(summary = "Vazifaning o'rnini (rank) va ustunini yangilash (Lexorank)")
    @PatchMapping("/{id}/reorder")
    public ApiResponse<TaskDto> reorderTask(
            @PathVariable String workspaceId,
            @PathVariable String id,
            @RequestBody com.taskcenter.dto.TaskReorderRequest request,
            @AuthenticationPrincipal User currentUser) {
        TaskDto task = taskService.reorderTask(workspaceId, id, request, currentUser);
        return ApiResponse.success("Vazifa tartibi yangilandi", task);
    }

    @Operation(summary = "Vazifani nusxalash (Clone)")
    @PostMapping("/{id}/clone")
    public ApiResponse<TaskDto> cloneTask(
            @PathVariable String workspaceId,
            @PathVariable String id,
            @AuthenticationPrincipal User currentUser) {
        TaskDto clonedTask = taskService.cloneTask(workspaceId, id, currentUser);
        return ApiResponse.success("Vazifa nusxalandi", clonedTask);
    }

    @Operation(summary = "Vazifani kuzatish / kuzatishni to'xtatish (Watch)")
    @PostMapping("/{id}/watch")
    public ApiResponse<Void> watchTask(
            @PathVariable String workspaceId,
            @PathVariable String id,
            @AuthenticationPrincipal User currentUser) {
        taskService.toggleWatch(workspaceId, id, currentUser);
        return ApiResponse.success("Kuzatuv holati o'zgartirildi", null);
    }

    @Operation(summary = "Vazifani o'chirish")
    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteTask(
            @PathVariable String workspaceId,
            @PathVariable String id,
            @AuthenticationPrincipal User currentUser) {
        taskService.deleteTask(workspaceId, id, currentUser);
        return ApiResponse.success("Bosh og'riq o'chirildi", null);
    }

    @Operation(summary = "Vazifani arxivga olish")
    @PostMapping("/{id}/archive")
    public ApiResponse<TaskDto> archiveTask(
            @PathVariable String workspaceId,
            @PathVariable String id,
            @AuthenticationPrincipal User currentUser) {
        TaskDto task = taskService.toggleArchive(workspaceId, id, true, currentUser);
        return ApiResponse.success("Vazifa arxivlandi", task);
    }

    @Operation(summary = "Vazifani arxivdan chiqarish")
    @PostMapping("/{id}/unarchive")
    public ApiResponse<TaskDto> unarchiveTask(
            @PathVariable String workspaceId,
            @PathVariable String id,
            @AuthenticationPrincipal User currentUser) {
        TaskDto task = taskService.toggleArchive(workspaceId, id, false, currentUser);
        return ApiResponse.success("Vazifa arxivdan chiqarildi", task);
    }

    @Operation(summary = "Vazifaga foydalanuvchini biriktirish / olib tashlash")
    @PostMapping("/{id}/assign")
    public ApiResponse<TaskDto> assignTask(
            @PathVariable String workspaceId,
            @PathVariable String id,
            @RequestParam String userId,
            @AuthenticationPrincipal User currentUser) {
        TaskDto task = taskService.toggleAssignee(workspaceId, id, userId, currentUser);
        return ApiResponse.success("Biriktirilgan foydalanuvchilar yangilandi", task);
    }
}
