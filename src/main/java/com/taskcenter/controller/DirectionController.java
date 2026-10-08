package com.taskcenter.controller;

import com.taskcenter.dto.ApiResponse;
import com.taskcenter.dto.DirectionDto;
import com.taskcenter.model.User;
import com.taskcenter.service.DirectionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Directions", description = "Vazifa yo'nalishlari (Frontend, Backend, QA va h.k.) bilan ishlash API lari")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/workspaces/{workspaceId}/directions")
public class DirectionController {

    private final DirectionService directionService;

    public DirectionController(DirectionService directionService) {
        this.directionService = directionService;
    }

    @Operation(
            operationId = "getWorkspaceDirections",
            summary = "Workspace ga tegishli barcha yo'nalishlarni olish",
            description = "Berilgan workspaceId ga tegishli barcha faol (o'chirilmagan) yo'nalishlar (Frontend, Backend va h.k.) ro'yxatini qaytaradi."
    )
    @GetMapping
    public ApiResponse<List<DirectionDto>> getDirections(
            @io.swagger.v3.oas.annotations.Parameter(description = "Ishchi maydon (workspace) ID si", example = "550e8400-e29b-41d4-a716-446655440000")
            @PathVariable String workspaceId,
            @AuthenticationPrincipal User currentUser) {
        List<DirectionDto> directions = directionService.getDirections(workspaceId, currentUser);
        return ApiResponse.success("ok", directions);
    }

    @Operation(
            operationId = "createWorkspaceDirection",
            summary = "Yangi yo'nalish qo'shish",
            description = "Workspace ichida yangi yo'nalish yaratadi. Nomi workspace doirasida unikal bo'lishi shart. Rang hex formatda (#3B82F6) qabul qilinadi."
    )
    @PostMapping
    public ApiResponse<DirectionDto> createDirection(
            @io.swagger.v3.oas.annotations.Parameter(description = "Ishchi maydon (workspace) ID si", example = "550e8400-e29b-41d4-a716-446655440000")
            @PathVariable String workspaceId,
            @Valid @RequestBody DirectionDto req,
            @AuthenticationPrincipal User currentUser) {
        DirectionDto created = directionService.createDirection(workspaceId, req, currentUser);
        return ApiResponse.success("Yo'nalish yaratildi", created);
    }

    @Operation(
            operationId = "updateWorkspaceDirection",
            summary = "Yo'nalishni tahrirlash (nomi, rangi)",
            description = "Yo'nalish nomi va/yoki rangini yangilaydi. Nom yangilanganda, mavjud boshqa yo'nalish nomi bilan to'qnashmasligi tekshiriladi."
    )
    @PutMapping("/{id}")
    public ApiResponse<DirectionDto> updateDirection(
            @io.swagger.v3.oas.annotations.Parameter(description = "Ishchi maydon (workspace) ID si", example = "550e8400-e29b-41d4-a716-446655440000")
            @PathVariable String workspaceId,
            @io.swagger.v3.oas.annotations.Parameter(description = "Yo'nalish ID si", example = "dir-uuid-123")
            @PathVariable String id,
            @Valid @RequestBody DirectionDto req,
            @AuthenticationPrincipal User currentUser) {
        DirectionDto updated = directionService.updateDirection(workspaceId, id, req, currentUser);
        return ApiResponse.success("Yo'nalish yangilandi", updated);
    }

    @Operation(
            operationId = "deleteWorkspaceDirection",
            summary = "Yo'nalishni o'chirish (Faqat OWNER/ADMIN)",
            description = "Yo'nalishni soft-delete qiladi va unga biriktirilgan vazifalardan bog'liqlikni uzadi. Faqat workspace egasi (OWNER) yoki admin (ADMIN) bajara oladi."
    )
    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteDirection(
            @io.swagger.v3.oas.annotations.Parameter(description = "Ishchi maydon (workspace) ID si", example = "550e8400-e29b-41d4-a716-446655440000")
            @PathVariable String workspaceId,
            @io.swagger.v3.oas.annotations.Parameter(description = "Yo'nalish ID si", example = "dir-uuid-123")
            @PathVariable String id,
            @AuthenticationPrincipal User currentUser) {
        directionService.deleteDirection(workspaceId, id, currentUser);
        return ApiResponse.success("Yo'nalish o'chirildi", null);
    }
}
