package com.taskcenter.controller;

import com.taskcenter.dto.ApiResponse;
import com.taskcenter.dto.WorkspaceCreateRequest;
import com.taskcenter.dto.WorkspaceDto;
import com.taskcenter.dto.WorkspaceListDto;
import com.taskcenter.model.User;
import com.taskcenter.service.WorkspaceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Workspace", description = "Workspace lar bilan ishlash API lari")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/workspaces")
@CrossOrigin(origins = "*")
public class WorkspaceController {

    private final WorkspaceService workspaceService;

    public WorkspaceController(WorkspaceService workspaceService) {
        this.workspaceService = workspaceService;
    }

    @Operation(summary = "Foydalanuvchining workspace lari ro'yxatini olish")
    @GetMapping
    public ApiResponse<List<WorkspaceListDto>> getWorkspaces(
            @AuthenticationPrincipal User currentUser,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        List<WorkspaceListDto> workspaces = workspaceService.getWorkspaces(currentUser, page, size);
        return ApiResponse.success("ok", workspaces);
    }

    @Operation(summary = "Bitta workspace ma'lumotlarini ID orqali olish")
    @GetMapping("/{id}")
    public ApiResponse<WorkspaceDto> getWorkspace(
            @PathVariable String id,
            @AuthenticationPrincipal User currentUser) {
        WorkspaceDto workspace = workspaceService.getWorkspaceById(id, currentUser);
        return ApiResponse.success("ok", workspace);
    }

    @Operation(summary = "Yangi workspace yaratish")
    @PostMapping
    public ApiResponse<WorkspaceDto> createWorkspace(
            @Valid @RequestBody WorkspaceCreateRequest request,
            @AuthenticationPrincipal User currentUser) {
        WorkspaceDto workspace = workspaceService.createWorkspace(request, currentUser);
        return ApiResponse.success("Workspace yaratildi", workspace);
    }

    @Operation(summary = "Mavjud workspace ni tahrirlash")
    @PutMapping("/{id}")
    public ApiResponse<WorkspaceDto> updateWorkspace(
            @PathVariable String id,
            @Valid @RequestBody WorkspaceCreateRequest request,
            @AuthenticationPrincipal User currentUser) {
        WorkspaceDto workspace = workspaceService.updateWorkspace(id, request, currentUser);
        return ApiResponse.success("Workspace yangilandi", workspace);
    }

    @Operation(summary = "Workspace ni o'chirish")
    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteWorkspace(
            @PathVariable String id,
            @AuthenticationPrincipal User currentUser) {
        workspaceService.deleteWorkspace(id, currentUser);
        return ApiResponse.success("Workspace o'chirildi", null);
    }
}
