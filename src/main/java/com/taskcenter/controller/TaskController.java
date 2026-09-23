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

import java.util.List;

@Tag(name = "Tasks", description = "Vazifalar (Tasks) bilan ishlash API lari")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/workspaces/{workspaceId}/tasks")
@CrossOrigin(origins = "*")
public class TaskController {

    private final TaskService taskService;

    public TaskController(TaskService taskService) {
        this.taskService = taskService;
    }

    @Operation(summary = "Workspace ga tegishli vazifalar ro'yxatini olish (pagination bilan)")
    @GetMapping
    public ApiResponse<List<TaskDto>> getTasks(
            @PathVariable String workspaceId,
            @AuthenticationPrincipal User currentUser,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        List<TaskDto> tasks = taskService.getTasksByWorkspace(workspaceId, currentUser, page, size);
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
        return ApiResponse.success("Task yaratildi", task);
    }

    @Operation(summary = "Vazifani to'liq yangilash")
    @PutMapping("/{id}")
    public ApiResponse<TaskDto> updateTask(
            @PathVariable String workspaceId,
            @PathVariable String id,
            @RequestBody TaskUpdateRequest request,
            @AuthenticationPrincipal User currentUser) {
        TaskDto task = taskService.updateTask(workspaceId, id, request, currentUser);
        return ApiResponse.success("Task yangilandi", task);
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

    @Operation(summary = "Ustun ichidagi vazifalar tartibini yangilash (oddiy task ID lar massivi: ['task1', 'task2'])")
    @PatchMapping("/columns/{columnId}/reorder")
    public ApiResponse<List<TaskDto>> reorderTasks(
            @PathVariable String workspaceId,
            @PathVariable String columnId,
            @RequestBody List<String> taskIds,
            @AuthenticationPrincipal User currentUser) {
        List<TaskDto> tasks = taskService.reorderTasks(workspaceId, columnId, taskIds, currentUser);
        return ApiResponse.success("Vazifalar tartibi yangilandi", tasks);
    }

    @Operation(summary = "Vazifani o'chirish")
    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteTask(
            @PathVariable String workspaceId,
            @PathVariable String id,
            @AuthenticationPrincipal User currentUser) {
        taskService.deleteTask(workspaceId, id, currentUser);
        return ApiResponse.success("Task o'chirildi", null);
    }
}
