package com.taskcenter.controller;

import com.taskcenter.dto.*;
import com.taskcenter.model.User;
import com.taskcenter.service.BoardService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class BoardController {

    private final BoardService boardService;

    public BoardController(BoardService boardService) {
        this.boardService = boardService;
    }

    // ========== COLUMNS ==========

    @GetMapping("/workspaces/{workspaceId}/columns")
    public ApiResponse<List<ColumnDto>> getColumns(
            @PathVariable String workspaceId,
            @AuthenticationPrincipal User currentUser) {
        return ApiResponse.success("ok", boardService.getColumns(workspaceId, currentUser));
    }

    @PostMapping("/workspaces/{workspaceId}/columns")
    public ApiResponse<ColumnDto> createColumn(
            @PathVariable String workspaceId,
            @Valid @RequestBody ColumnCreateRequest req,
            @AuthenticationPrincipal User currentUser) {
        return ApiResponse.success("Column yaratildi", boardService.createColumn(workspaceId, req, currentUser));
    }

    @PutMapping("/workspaces/{workspaceId}/columns/{id}")
    public ApiResponse<ColumnDto> updateColumn(
            @PathVariable String workspaceId,
            @PathVariable String id,
            @RequestBody ColumnCreateRequest req,
            @AuthenticationPrincipal User currentUser) {
        return ApiResponse.success("Column yangilandi", boardService.updateColumn(workspaceId, id, req, currentUser));
    }

    @PatchMapping("/workspaces/{workspaceId}/columns/{id}")
    public ApiResponse<ColumnDto> patchColumn(
            @PathVariable String workspaceId,
            @PathVariable String id,
            @RequestBody ColumnPatchRequest req,
            @AuthenticationPrincipal User currentUser) {
        return ApiResponse.success("Column yangilandi", boardService.patchColumn(workspaceId, id, req, currentUser));
    }

    @PatchMapping("/workspaces/{workspaceId}/columns")
    public ApiResponse<List<ColumnDto>> reorderColumns(
            @PathVariable String workspaceId,
            @RequestBody List<ColumnReorderItem> items,
            @AuthenticationPrincipal User currentUser) {
        return ApiResponse.success("Columnlar tartibi yangilandi", boardService.reorderColumns(workspaceId, items, currentUser));
    }

    @DeleteMapping("/workspaces/{workspaceId}/columns/{id}")
    public ApiResponse<Void> deleteColumn(
            @PathVariable String workspaceId,
            @PathVariable String id,
            @AuthenticationPrincipal User currentUser) {
        boardService.deleteColumn(workspaceId, id, currentUser);
        return ApiResponse.success("Column o'chirildi", null);
    }

    // ========== BOARD ==========

    @GetMapping("/workspaces/{workspaceId}/board")
    public ApiResponse<List<ColumnWithCardsDto>> getBoard(
            @PathVariable String workspaceId,
            @AuthenticationPrincipal User currentUser) {
        return ApiResponse.success("ok", boardService.getBoard(workspaceId, currentUser));
    }

    // ========== TASKS ==========

    @GetMapping("/workspaces/{workspaceId}/tasks")
    public ApiResponse<List<TaskDto>> getTasksByWorkspace(
            @PathVariable String workspaceId,
            @AuthenticationPrincipal User currentUser,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        return ApiResponse.success("ok", boardService.getTasksByWorkspace(workspaceId, currentUser, page, size));
    }

    @GetMapping("/workspaces/{workspaceId}/tasks/{id}")
    public ApiResponse<TaskDto> getTaskById(
            @PathVariable String workspaceId,
            @PathVariable String id,
            @AuthenticationPrincipal User currentUser) {
        return ApiResponse.success("ok", boardService.getTaskById(workspaceId, id, currentUser));
    }

    @PostMapping("/workspaces/{workspaceId}/tasks")
    public ApiResponse<TaskDto> createTask(
            @PathVariable String workspaceId,
            @Valid @RequestBody TaskCreateRequest req,
            @AuthenticationPrincipal User currentUser) {
        return ApiResponse.success("Task yaratildi", boardService.createTask(workspaceId, req, currentUser));
    }

    @PutMapping("/workspaces/{workspaceId}/tasks/{id}")
    public ApiResponse<TaskDto> updateTask(
            @PathVariable String workspaceId,
            @PathVariable String id,
            @RequestBody TaskUpdateRequest req,
            @AuthenticationPrincipal User currentUser) {
        return ApiResponse.success("Task yangilandi", boardService.updateTask(workspaceId, id, req, currentUser));
    }

    @PatchMapping("/workspaces/{workspaceId}/tasks/{id}")
    public ApiResponse<TaskDto> patchTask(
            @PathVariable String workspaceId,
            @PathVariable String id,
            @RequestBody TaskUpdateRequest req,
            @AuthenticationPrincipal User currentUser) {
        return ApiResponse.success("Task yangilandi", boardService.updateTask(workspaceId, id, req, currentUser));
    }

    @DeleteMapping("/workspaces/{workspaceId}/tasks/{id}")
    public ApiResponse<Void> deleteTask(
            @PathVariable String workspaceId,
            @PathVariable String id,
            @AuthenticationPrincipal User currentUser) {
        boardService.deleteTask(workspaceId, id, currentUser);
        return ApiResponse.success("Task o'chirildi", null);
    }

    // ========== LABELS ==========

    @GetMapping("/workspaces/{workspaceId}/labels")
    public ApiResponse<List<LabelDto>> getLabels(
            @PathVariable String workspaceId,
            @AuthenticationPrincipal User currentUser) {
        return ApiResponse.success("ok", boardService.getLabels(workspaceId, currentUser));
    }

    @PostMapping("/workspaces/{workspaceId}/labels")
    public ApiResponse<LabelDto> createLabel(
            @PathVariable String workspaceId,
            @RequestBody LabelDto req,
            @AuthenticationPrincipal User currentUser) {
        return ApiResponse.success("Label yaratildi", boardService.createLabel(workspaceId, req, currentUser));
    }

    @DeleteMapping("/workspaces/{workspaceId}/labels/{id}")
    public ApiResponse<Void> deleteLabel(
            @PathVariable String workspaceId,
            @PathVariable String id,
            @AuthenticationPrincipal User currentUser) {
        boardService.deleteLabel(workspaceId, id, currentUser);
        return ApiResponse.success("Label o'chirildi", null);
    }

    // ========== MEMBERS ==========

    @GetMapping("/workspaces/{workspaceId}/members")
    public ApiResponse<List<UserDto>> getMembers(
            @PathVariable String workspaceId,
            @AuthenticationPrincipal User currentUser) {
        return ApiResponse.success("ok", boardService.getMembers(workspaceId, currentUser));
    }
}
