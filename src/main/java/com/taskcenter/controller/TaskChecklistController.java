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

    @Operation(summary = "Taskning barcha checklistlarini olish")
    @GetMapping
    public ApiResponse<List<ChecklistItemDto>> getChecklists(
            @PathVariable String workspaceId,
            @PathVariable String taskId,
            @AuthenticationPrincipal User currentUser) {
        return ApiResponse.success("ok", checklistService.getItems(workspaceId, taskId, currentUser));
    }

    @Operation(summary = "Yangi checklist qo'shish")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<ChecklistItemDto> addChecklist(
            @PathVariable String workspaceId,
            @PathVariable String taskId,
            @Valid @RequestBody ChecklistItemCreateRequest request,
            @AuthenticationPrincipal User currentUser) {
        return ApiResponse.success("ok", checklistService.addItem(workspaceId, taskId, request, currentUser));
    }

    @Operation(summary = "Checklistni yangilash (titleni yoki bajarganlikni)")
    @PutMapping("/{itemId}")
    public ApiResponse<ChecklistItemDto> updateChecklist(
            @PathVariable String workspaceId,
            @PathVariable String taskId,
            @PathVariable String itemId,
            @RequestBody ChecklistItemUpdateRequest request,
            @AuthenticationPrincipal User currentUser) {
        return ApiResponse.success("ok", checklistService.updateItem(workspaceId, taskId, itemId, request, currentUser));
    }

    @Operation(summary = "Checklistni o'chirish")
    @DeleteMapping("/{itemId}")
    public ApiResponse<Void> deleteChecklist(
            @PathVariable String workspaceId,
            @PathVariable String taskId,
            @PathVariable String itemId,
            @AuthenticationPrincipal User currentUser) {
        checklistService.deleteItem(workspaceId, taskId, itemId, currentUser);
        return ApiResponse.success("o'chirildi", null);
    }
}
