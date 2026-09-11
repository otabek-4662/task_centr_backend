package com.taskcenter.controller;

import com.taskcenter.dto.*;
import com.taskcenter.model.User;
import com.taskcenter.service.WorkspaceService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/workspaces")
@CrossOrigin(origins = "*")
public class WorkspaceController {

    private final WorkspaceService workspaceService;

    public WorkspaceController(WorkspaceService workspaceService) {
        this.workspaceService = workspaceService;
    }

    @GetMapping
    public ApiResponse<List<WorkspaceListDto>> getWorkspaces(
            @AuthenticationPrincipal User currentUser,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.success("ok", workspaceService.getWorkspaceList(currentUser, page, size));
    }

    @GetMapping("/{id}")
    public ApiResponse<WorkspaceDto> getWorkspace(
            @PathVariable String id,
            @AuthenticationPrincipal User currentUser) {
        return ApiResponse.success("ok", workspaceService.getWorkspaceById(id, currentUser));
    }

    @PostMapping
    public ApiResponse<WorkspaceDto> createWorkspace(
            @Valid @RequestBody WorkspaceCreateRequest req,
            @AuthenticationPrincipal User currentUser) {
        return ApiResponse.success("Workspace yaratildi", workspaceService.createWorkspace(req, currentUser));
    }

    @PutMapping("/{id}")
    public ApiResponse<WorkspaceDto> updateWorkspace(
            @PathVariable String id,
            @RequestBody WorkspaceCreateRequest req,
            @AuthenticationPrincipal User currentUser) {
        return ApiResponse.success("Workspace yangilandi", workspaceService.updateWorkspace(id, req, currentUser));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteWorkspace(
            @PathVariable String id,
            @AuthenticationPrincipal User currentUser) {
        workspaceService.deleteWorkspace(id, currentUser);
        return ApiResponse.success("Workspace o'chirildi", null);
    }
}
