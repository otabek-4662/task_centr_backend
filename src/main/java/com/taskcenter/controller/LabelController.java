package com.taskcenter.controller;

import com.taskcenter.dto.ApiResponse;
import com.taskcenter.dto.LabelDto;
import com.taskcenter.model.User;
import com.taskcenter.service.LabelService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Labels", description = "Vazifalar yorliqlari (Labels) bilan ishlash API lari")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/workspaces/{workspaceId}/labels")
public class LabelController {

    private final LabelService labelService;

    public LabelController(LabelService labelService) {
        this.labelService = labelService;
    }

    @Operation(
            operationId = "getWorkspaceLabels",
            summary = "Workspace ga tegishli barcha labellarni olish",
            description = "Ishchi maydonga tegishli barcha faol teglar (yorliqlar) ro'yxatini qaytaradi."
    )
    @GetMapping
    public ApiResponse<List<LabelDto>> getLabels(
            @io.swagger.v3.oas.annotations.Parameter(description = "Ishchi maydon (workspace) ID si", example = "550e8400-e29b-41d4-a716-446655440000")
            @PathVariable String workspaceId,
            @AuthenticationPrincipal User currentUser) {
        List<LabelDto> labels = labelService.getLabels(workspaceId, currentUser);
        return ApiResponse.success("ok", labels);
    }

    @Operation(
            operationId = "createWorkspaceLabel",
            summary = "Yangi label yaratish",
            description = "Yangi teg (yorliq) yaratadi. Nomi va ixtiyoriy hex rangi beriladi."
    )
    @PostMapping
    public ApiResponse<LabelDto> createLabel(
            @io.swagger.v3.oas.annotations.Parameter(description = "Ishchi maydon (workspace) ID si", example = "550e8400-e29b-41d4-a716-446655440000")
            @PathVariable String workspaceId,
            @jakarta.validation.Valid @RequestBody LabelDto req,
            @AuthenticationPrincipal User currentUser) {
        LabelDto label = labelService.createLabel(workspaceId, req, currentUser);
        return ApiResponse.success("Label yaratildi", label);
    }

    @Operation(
            operationId = "updateWorkspaceLabel",
            summary = "Label ni yangilash",
            description = "Tegning nomi va rangini o'zgartiradi."
    )
    @PutMapping("/{id}")
    public ApiResponse<LabelDto> updateLabel(
            @io.swagger.v3.oas.annotations.Parameter(description = "Ishchi maydon (workspace) ID si", example = "550e8400-e29b-41d4-a716-446655440000")
            @PathVariable String workspaceId,
            @io.swagger.v3.oas.annotations.Parameter(description = "Teg (label) ID si", example = "lbl-uuid-123")
            @PathVariable String id,
            @jakarta.validation.Valid @RequestBody LabelDto req,
            @AuthenticationPrincipal User currentUser) {
        LabelDto label = labelService.updateLabel(workspaceId, id, req, currentUser);
        return ApiResponse.success("Label yangilandi", label);
    }

    @Operation(
            operationId = "deleteWorkspaceLabel",
            summary = "Label ni o'chirish",
            description = "Tegni soft-delete qiladi va vazifalardan olib tashlaydi."
    )
    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteLabel(
            @io.swagger.v3.oas.annotations.Parameter(description = "Ishchi maydon (workspace) ID si", example = "550e8400-e29b-41d4-a716-446655440000")
            @PathVariable String workspaceId,
            @io.swagger.v3.oas.annotations.Parameter(description = "Teg (label) ID si", example = "lbl-uuid-123")
            @PathVariable String id,
            @AuthenticationPrincipal User currentUser) {
        labelService.deleteLabel(workspaceId, id, currentUser);
        return ApiResponse.success("Label o'chirildi", null);
    }
}

