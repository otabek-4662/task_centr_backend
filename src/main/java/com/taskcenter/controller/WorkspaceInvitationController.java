package com.taskcenter.controller;

import com.taskcenter.dto.ApiResponse;
import com.taskcenter.dto.InviteRequestDto;
import com.taskcenter.dto.WorkspaceInvitationDto;
import com.taskcenter.model.User;
import com.taskcenter.service.WorkspaceInvitationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
@Tag(name = "Workspace Invitations", description = "Workspace ga taklif qilish API lari")
@SecurityRequirement(name = "bearerAuth")
public class WorkspaceInvitationController {

    private final WorkspaceInvitationService invitationService;

    public WorkspaceInvitationController(WorkspaceInvitationService invitationService) {
        this.invitationService = invitationService;
    }

    @Operation(operationId = "createWorkspaceInvitation", summary = "Workspace ga taklif yuborish (Admin/Owner)",
            description = "Foydalanuvchiga workspace ga qo'shilish uchun rasmiy taklifnoma yuboradi. Faqat OWNER yoki ADMIN bajara oladi.")
    @PostMapping("/workspaces/{workspaceId}/invites")
    public ApiResponse<WorkspaceInvitationDto> inviteUser(
            @io.swagger.v3.oas.annotations.Parameter(description = "Ishchi maydon identifikatori (UUID)", example = "ws-123", required = true)
            @PathVariable String workspaceId,
            @Valid @RequestBody InviteRequestDto request,
            @AuthenticationPrincipal User currentUser) {
        WorkspaceInvitationDto dto = invitationService.inviteUser(workspaceId, request, currentUser);
        return ApiResponse.success("Taklif muvaffaqiyatli yuborildi", dto);
    }

    @Operation(operationId = "getWorkspaceInvitations", summary = "Workspace takliflarini ko'rish (Admin/Owner)",
            description = "Berilgan workspace ga yuborilgan barcha kutilayotgan (PENDING) taklifnomalar ro'yxatini ko'rish.")
    @GetMapping("/workspaces/{workspaceId}/invites")
    public ApiResponse<List<WorkspaceInvitationDto>> getWorkspaceInvitations(
            @io.swagger.v3.oas.annotations.Parameter(description = "Ishchi maydon identifikatori (UUID)", example = "ws-123", required = true)
            @PathVariable String workspaceId,
            @AuthenticationPrincipal User currentUser) {
        return ApiResponse.success("ok", invitationService.getWorkspaceInvitations(workspaceId, currentUser));
    }

    @Operation(operationId = "cancelWorkspaceInvitation", summary = "Taklifni bekor qilish (Admin/Owner)",
            description = "Yuborilgan taklifnomani bekor qiladi. Faqat OWNER yoki ADMIN bajara oladi.")
    @DeleteMapping("/workspaces/{workspaceId}/invites/{id}")
    public ApiResponse<Void> cancelInvitation(
            @io.swagger.v3.oas.annotations.Parameter(description = "Ishchi maydon identifikatori (UUID)", example = "ws-123", required = true)
            @PathVariable String workspaceId,
            @io.swagger.v3.oas.annotations.Parameter(description = "Taklifnoma identifikatori (UUID)", example = "inv-123", required = true)
            @PathVariable String id,
            @AuthenticationPrincipal User currentUser) {
        invitationService.cancelInvitation(workspaceId, id, currentUser);
        return ApiResponse.success("Taklif bekor qilindi", null);
    }

    @Operation(operationId = "getMyPendingInvitations", summary = "Menga kelgan faol takliflarni ko'rish",
            description = "Joriy kirgan foydalanuvchining hisobiga kelgan barcha faol taklifnomalarni qaytaradi.")
    @GetMapping("/invitations/me")
    public ApiResponse<List<WorkspaceInvitationDto>> getMyInvitations(
            @AuthenticationPrincipal User currentUser) {
        return ApiResponse.success("ok", invitationService.getMyPendingInvitations(currentUser));
    }

    @Operation(operationId = "acceptWorkspaceInvitation", summary = "Taklifni qabul qilish",
            description = "Kelgan taklifnomani qabul qilib, jamoaga a'zo bo'lib qo'shilish.")
    @PatchMapping("/invitations/{id}/accept")
    public ApiResponse<Void> acceptInvitation(
            @io.swagger.v3.oas.annotations.Parameter(description = "Taklifnoma identifikatori (UUID)", example = "inv-123", required = true)
            @PathVariable String id,
            @AuthenticationPrincipal User currentUser) {
        invitationService.acceptInvitation(id, currentUser);
        return ApiResponse.success("Taklif qabul qilindi va jamoaga qo'shildingiz", null);
    }

    @Operation(operationId = "rejectWorkspaceInvitation", summary = "Taklifni rad etish",
            description = "Kelgan taklifnomani rad etish.")
    @PatchMapping("/invitations/{id}/reject")
    public ApiResponse<Void> rejectInvitation(
            @io.swagger.v3.oas.annotations.Parameter(description = "Taklifnoma identifikatori (UUID)", example = "inv-123", required = true)
            @PathVariable String id,
            @AuthenticationPrincipal User currentUser) {
        invitationService.rejectInvitation(id, currentUser);
        return ApiResponse.success("Taklif rad etildi", null);
    }
}
