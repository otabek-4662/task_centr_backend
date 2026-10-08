package com.taskcenter.controller;

import com.taskcenter.dto.*;
import com.taskcenter.model.User;
import com.taskcenter.service.TaskChecklistService;
import io.swagger.v3.oas.annotations.Operation;
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
@RequestMapping("/api/workspaces/{workspaceId}/tasks/{taskId}/checklists")
public class TaskChecklistController {

    private final TaskChecklistService checklistService;

    public TaskChecklistController(TaskChecklistService checklistService) {
        this.checklistService = checklistService;
    }

    @Deprecated
    @Operation(
            operationId = "getWorkspaceTaskChecklistsLegacy",
            summary = "Taskning barcha checklistlarini olish (eski uslub)",
            description = "Eski uslubdagi endpoint. Yangi toza REST varianti: GET /api/tasks/{taskId}/checklists.",
            deprecated = true
    )
    @GetMapping
    public ApiResponse<List<ChecklistItemDto>> getChecklists(
            @io.swagger.v3.oas.annotations.Parameter(description = "Ishchi maydon (workspace) ID si", example = "550e8400-e29b-41d4-a716-446655440000")
            @PathVariable String workspaceId,
            @io.swagger.v3.oas.annotations.Parameter(description = "Vazifa (task) ID si", example = "task-uuid-123")
            @PathVariable String taskId,
            @AuthenticationPrincipal User currentUser) {
        return ApiResponse.success("ok", checklistService.getItems(workspaceId, taskId, currentUser));
    }

    @Deprecated
    @Operation(
            operationId = "createWorkspaceTaskChecklistItemLegacy",
            summary = "Yangi checklist qo'shish (eski uslub)",
            description = "Eski uslubdagi endpoint. Yangi toza REST varianti: POST /api/tasks/{taskId}/checklists.",
            deprecated = true
    )
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<ChecklistItemDto> addChecklist(
            @io.swagger.v3.oas.annotations.Parameter(description = "Ishchi maydon (workspace) ID si", example = "550e8400-e29b-41d4-a716-446655440000")
            @PathVariable String workspaceId,
            @io.swagger.v3.oas.annotations.Parameter(description = "Vazifa (task) ID si", example = "task-uuid-123")
            @PathVariable String taskId,
            @Valid @RequestBody ChecklistItemCreateRequest request,
            @AuthenticationPrincipal User currentUser) {
        return ApiResponse.success("ok", checklistService.addItem(workspaceId, taskId, request, currentUser));
    }

    @Deprecated
    @Operation(
            operationId = "updateWorkspaceTaskChecklistItemLegacy",
            summary = "Checklistni yangilash (eski uslub)",
            description = "Eski uslubdagi endpoint. Yangi toza REST varianti: PUT /api/tasks/{taskId}/checklists/{itemId}.",
            deprecated = true
    )
    @PutMapping("/{itemId}")
    public ApiResponse<ChecklistItemDto> updateChecklist(
            @io.swagger.v3.oas.annotations.Parameter(description = "Ishchi maydon (workspace) ID si", example = "550e8400-e29b-41d4-a716-446655440000")
            @PathVariable String workspaceId,
            @io.swagger.v3.oas.annotations.Parameter(description = "Vazifa (task) ID si", example = "task-uuid-123")
            @PathVariable String taskId,
            @io.swagger.v3.oas.annotations.Parameter(description = "Checklist bandi ID si", example = "item-uuid-123")
            @PathVariable String itemId,
            @Valid @RequestBody ChecklistItemUpdateRequest request,
            @AuthenticationPrincipal User currentUser) {
        return ApiResponse.success("ok", checklistService.updateItem(workspaceId, taskId, itemId, request, currentUser));
    }

    @Deprecated
    @Operation(
            operationId = "deleteWorkspaceTaskChecklistItemLegacy",
            summary = "Checklistni o'chirish (eski uslub)",
            description = "Eski uslubdagi endpoint. Yangi toza REST varianti: DELETE /api/tasks/{taskId}/checklists/{itemId}.",
            deprecated = true
    )
    @DeleteMapping("/{itemId}")
    public ApiResponse<Void> deleteChecklist(
            @io.swagger.v3.oas.annotations.Parameter(description = "Ishchi maydon (workspace) ID si", example = "550e8400-e29b-41d4-a716-446655440000")
            @PathVariable String workspaceId,
            @io.swagger.v3.oas.annotations.Parameter(description = "Vazifa (task) ID si", example = "task-uuid-123")
            @PathVariable String taskId,
            @io.swagger.v3.oas.annotations.Parameter(description = "Checklist bandi ID si", example = "item-uuid-123")
            @PathVariable String itemId,
            @AuthenticationPrincipal User currentUser) {
        checklistService.deleteItem(workspaceId, taskId, itemId, currentUser);
        return ApiResponse.success("o'chirildi", null);
    }
}
