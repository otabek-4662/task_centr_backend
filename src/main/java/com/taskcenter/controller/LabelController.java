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
@CrossOrigin(origins = "*")
public class LabelController {

    private final LabelService labelService;

    public LabelController(LabelService labelService) {
        this.labelService = labelService;
    }

    @Operation(summary = "Workspace ga tegishli barcha labellarni olish")
    @GetMapping
    public ApiResponse<List<LabelDto>> getLabels(
            @PathVariable String workspaceId,
            @AuthenticationPrincipal User currentUser) {
        List<LabelDto> labels = labelService.getLabels(workspaceId, currentUser);
        return ApiResponse.success("ok", labels);
    }

    @Operation(summary = "Yangi label yaratish")
    @PostMapping
    public ApiResponse<LabelDto> createLabel(
            @PathVariable String workspaceId,
            @RequestBody LabelDto req,
            @AuthenticationPrincipal User currentUser) {
        LabelDto label = labelService.createLabel(workspaceId, req, currentUser);
        return ApiResponse.success("Label yaratildi", label);
    }

    @Operation(summary = "Label ni o'chirish")
    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteLabel(
            @PathVariable String workspaceId,
            @PathVariable String id,
            @AuthenticationPrincipal User currentUser) {
        labelService.deleteLabel(workspaceId, id, currentUser);
        return ApiResponse.success("Label o'chirildi", null);
    }
}
