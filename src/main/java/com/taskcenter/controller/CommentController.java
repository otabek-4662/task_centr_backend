package com.taskcenter.controller;

import com.taskcenter.dto.ApiResponse;
import com.taskcenter.dto.CommentCreateRequest;
import com.taskcenter.dto.CommentDto;
import com.taskcenter.dto.CommentUpdateRequest;
import com.taskcenter.model.User;
import com.taskcenter.service.CommentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Comments", description = "Task izohlari bilan ishlash API lari")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/tasks/{taskId}/comments")
public class CommentController {

    private final CommentService commentService;

    public CommentController(CommentService commentService) {
        this.commentService = commentService;
    }

    @Operation(
            operationId = "getTaskComments",
            summary = "Task izohlari ro'yxatini sahifalab olish",
            description = "Vazifaga qoldirilgan barcha izohlarni sahifalab (pagination) va sanasi bo'yicha saralangan holda qaytaradi."
    )
    @GetMapping
    public ApiResponse<Page<CommentDto>> getComments(
            @io.swagger.v3.oas.annotations.Parameter(description = "Vazifa (task) ID si", example = "task-uuid-123")
            @PathVariable String taskId,
            @org.springdoc.core.annotations.ParameterObject @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.ASC) Pageable pageable,
            @AuthenticationPrincipal User currentUser) {
        Page<CommentDto> comments = commentService.getComments(taskId, pageable, currentUser);
        return ApiResponse.success("ok", comments);
    }

    @Operation(
            operationId = "createTaskComment",
            summary = "Taskka yangi izoh qo'shish",
            description = "Vazifaga yangi sharh/izoh yozadi. Matn 5000 belgidan oshmasligi kerak."
    )
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<CommentDto> addComment(
            @io.swagger.v3.oas.annotations.Parameter(description = "Vazifa (task) ID si", example = "task-uuid-123")
            @PathVariable String taskId,
            @Valid @RequestBody CommentCreateRequest request,
            @AuthenticationPrincipal User currentUser) {
        CommentDto comment = commentService.addComment(taskId, request, currentUser);
        return ApiResponse.success("Izoh muvaffaqiyatli qo'shildi", comment);
    }

    @Operation(
            operationId = "updateTaskComment",
            summary = "Izohni tahrirlash",
            description = "Faqat izoh muallifi tomonidan o'zgartirilishi mumkin."
    )
    @PutMapping("/{commentId}")
    public ApiResponse<CommentDto> updateComment(
            @io.swagger.v3.oas.annotations.Parameter(description = "Vazifa (task) ID si", example = "task-uuid-123")
            @PathVariable String taskId,
            @io.swagger.v3.oas.annotations.Parameter(description = "Izoh (comment) ID si", example = "comment-uuid-123")
            @PathVariable String commentId,
            @Valid @RequestBody CommentUpdateRequest request,
            @AuthenticationPrincipal User currentUser) {
        CommentDto updated = commentService.updateComment(taskId, commentId, request, currentUser);
        return ApiResponse.success("Izoh muvaffaqiyatli yangilandi", updated);
    }

    @Operation(
            operationId = "deleteTaskComment",
            summary = "Izohni o'chirish",
            description = "Faqat izoh muallifi yoki loyiha admini tomonidan o'chirilishi mumkin."
    )
    @DeleteMapping("/{commentId}")
    public ApiResponse<Void> deleteComment(
            @io.swagger.v3.oas.annotations.Parameter(description = "Vazifa (task) ID si", example = "task-uuid-123")
            @PathVariable String taskId,
            @io.swagger.v3.oas.annotations.Parameter(description = "Izoh (comment) ID si", example = "comment-uuid-123")
            @PathVariable String commentId,
            @AuthenticationPrincipal User currentUser) {
        commentService.deleteComment(taskId, commentId, currentUser);
        return ApiResponse.success("Izoh muvaffaqiyatli o'chirildi", null);
    }
}
