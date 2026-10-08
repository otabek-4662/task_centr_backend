package com.taskcenter.controller;

import com.taskcenter.dto.*;
import com.taskcenter.model.User;
import com.taskcenter.service.TaskChecklistService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Checklists", description = "Vazifalar ichidagi kichik ro'yxatlar API lari")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/tasks/{taskId}/checklists")
public class TaskChecklistDirectController {

    private final TaskChecklistService checklistService;

    public TaskChecklistDirectController(TaskChecklistService checklistService) {
        this.checklistService = checklistService;
    }

    @Operation(
            operationId = "getTaskChecklists",
            summary = "Taskning barcha checklistlarini olish",
            description = "Berilgan vazifaga tegishli barcha quyi topshiriqlar (kichik ro'yxat bandlari) ro'yxatini qaytaradi."
    )
    @GetMapping
    public ApiResponse<List<ChecklistItemDto>> getChecklists(
            @Parameter(description = "Vazifa (task) ID si", example = "task-uuid-123")
            @PathVariable String taskId,
            @AuthenticationPrincipal User currentUser) {
        String workspaceId = checklistService.resolveWorkspaceId(taskId);
        return ApiResponse.success("ok", checklistService.getItems(workspaceId, taskId, currentUser));
    }

    @Operation(
            operationId = "createTaskChecklistItem",
            summary = "Yangi checklist bandi qo'shish",
            description = "Vazifa ichiga yangi quyi topshiriq (checklist bandi) qo'shadi."
    )
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<ChecklistItemDto> addChecklist(
            @Parameter(description = "Vazifa (task) ID si", example = "task-uuid-123")
            @PathVariable String taskId,
            @Valid @RequestBody ChecklistItemCreateRequest request,
            @AuthenticationPrincipal User currentUser) {
        String workspaceId = checklistService.resolveWorkspaceId(taskId);
        return ApiResponse.success("ok", checklistService.addItem(workspaceId, taskId, request, currentUser));
    }

    @Operation(
            operationId = "updateTaskChecklistItem",
            summary = "Checklist bandini yangilash (sarlavha yoki bajarilganlik)",
            description = "Checklist bandining sarlavhasi, bajarilganlik holati (isCompleted) yoki tartib indeksini yangilaydi."
    )
    @PutMapping("/{itemId}")
    public ApiResponse<ChecklistItemDto> updateChecklist(
            @Parameter(description = "Vazifa (task) ID si", example = "task-uuid-123")
            @PathVariable String taskId,
            @Parameter(description = "Checklist bandi ID si", example = "item-uuid-123")
            @PathVariable String itemId,
            @Valid @RequestBody ChecklistItemUpdateRequest request,
            @AuthenticationPrincipal User currentUser) {
        String workspaceId = checklistService.resolveWorkspaceId(taskId);
        return ApiResponse.success("ok", checklistService.updateItem(workspaceId, taskId, itemId, request, currentUser));
    }

    @Operation(
            operationId = "deleteTaskChecklistItem",
            summary = "Checklist bandini o'chirish",
            description = "Vazifa ichidagi berilgan checklist bandini o'chiradi."
    )
    @DeleteMapping("/{itemId}")
    public ApiResponse<Void> deleteChecklist(
            @Parameter(description = "Vazifa (task) ID si", example = "task-uuid-123")
            @PathVariable String taskId,
            @Parameter(description = "Checklist bandi ID si", example = "item-uuid-123")
            @PathVariable String itemId,
            @AuthenticationPrincipal User currentUser) {
        String workspaceId = checklistService.resolveWorkspaceId(taskId);
        checklistService.deleteItem(workspaceId, taskId, itemId, currentUser);
        return ApiResponse.success("o'chirildi", null);
    }
}
