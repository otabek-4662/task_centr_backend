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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;

import java.util.List;

@Tag(name = "Workspace", description = "Workspace lar bilan ishlash API lari")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/workspaces")
public class WorkspaceController {

    private final WorkspaceService workspaceService;

    public WorkspaceController(WorkspaceService workspaceService) {
        this.workspaceService = workspaceService;
    }

    @Operation(operationId = "getMyWorkspaces", summary = "Foydalanuvchining workspace lari ro'yxatini olish",
            description = "Joriy foydalanuvchi a'zo bo'lgan barcha ishchi maydonlar ro'yxatini sahifalab (pagination) qaytaradi.")
    @GetMapping
    public ApiResponse<Page<WorkspaceListDto>> getWorkspaces(
            @AuthenticationPrincipal User currentUser,
            @org.springdoc.core.annotations.ParameterObject @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        Page<WorkspaceListDto> workspaces = workspaceService.getWorkspaces(currentUser, pageable);
        return ApiResponse.success("ok", workspaces);
    }

    @Operation(operationId = "getWorkspaceById", summary = "Bitta workspace ma'lumotlarini ID orqali olish",
            description = "Berilgan ID li ishchi maydonning batafsil ma'lumotlari, egaligi va sozlamalarini qaytaradi.")
    @GetMapping("/{id}")
    public ApiResponse<WorkspaceDto> getWorkspace(
            @io.swagger.v3.oas.annotations.Parameter(description = "Ishchi maydon identifikatori (UUID)", example = "ws-123", required = true)
            @PathVariable String id,
            @AuthenticationPrincipal User currentUser) {
        WorkspaceDto workspace = workspaceService.getWorkspaceById(id, currentUser);
        return ApiResponse.success("ok", workspace);
    }

    @Operation(operationId = "createWorkspace", summary = "Yangi workspace yaratish",
            description = "Yangi ishchi maydon yaratadi. Foydalanuvchi avtomatik tarzda OWNER roliga ega bo'ladi. Agar initDefaultColumns=true bo'lsa, To Do, In Progress, Done ustunlari avtomatik ochiladi.")
    @PostMapping
    public ApiResponse<WorkspaceDto> createWorkspace(
            @Valid @RequestBody WorkspaceCreateRequest request,
            @AuthenticationPrincipal User currentUser) {
        WorkspaceDto workspace = workspaceService.createWorkspace(request, currentUser);
        return ApiResponse.success("G'alva yaratildi", workspace);
    }

    @Operation(operationId = "updateWorkspace", summary = "Mavjud workspace ni tahrirlash",
            description = "Ishchi maydonning sarlavhasi, foni va tavsifini tahrirlaydi. Faqat workspace egasi (OWNER) yoki admin (ADMIN) bajara oladi.")
    @PutMapping("/{id}")
    public ApiResponse<WorkspaceDto> updateWorkspace(
            @io.swagger.v3.oas.annotations.Parameter(description = "Ishchi maydon identifikatori (UUID)", example = "ws-123", required = true)
            @PathVariable String id,
            @Valid @RequestBody WorkspaceCreateRequest request,
            @AuthenticationPrincipal User currentUser) {
        WorkspaceDto workspace = workspaceService.updateWorkspace(id, request, currentUser);
        return ApiResponse.success("G'alva yangilandi", workspace);
    }

    @Operation(operationId = "deleteWorkspace", summary = "Workspace ni o'chirish",
            description = "Ishchi maydonni soft-delete qiladi. Faqat workspace egasi (OWNER) bajara oladi.")
    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteWorkspace(
            @io.swagger.v3.oas.annotations.Parameter(description = "Ishchi maydon identifikatori (UUID)", example = "ws-123", required = true)
            @PathVariable String id,
            @AuthenticationPrincipal User currentUser) {
        workspaceService.deleteWorkspace(id, currentUser);
        return ApiResponse.success("G'alva o'chirildi", null);
    }
}
