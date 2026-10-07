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

    @Operation(summary = "Workspace ga taklif yuborish (Admin/Owner)")
    @PostMapping("/workspaces/{workspaceId}/invites")
    public ApiResponse<WorkspaceInvitationDto> inviteUser(
            @PathVariable String workspaceId,
            @Valid @RequestBody InviteRequestDto request,
            @AuthenticationPrincipal User currentUser) {
        WorkspaceInvitationDto dto = invitationService.inviteUser(workspaceId, request, currentUser);
        return ApiResponse.success("Taklif muvaffaqiyatli yuborildi", dto);
    }

    @Operation(summary = "Workspace takliflarini ko'rish (Admin/Owner)")
    @GetMapping("/workspaces/{workspaceId}/invites")
    public ApiResponse<List<WorkspaceInvitationDto>> getWorkspaceInvitations(
            @PathVariable String workspaceId,
            @AuthenticationPrincipal User currentUser) {
        return ApiResponse.success("ok", invitationService.getWorkspaceInvitations(workspaceId, currentUser));
    }

    @Operation(summary = "Taklifni bekor qilish (Admin/Owner)")
    @DeleteMapping("/workspaces/{workspaceId}/invites/{id}")
    public ApiResponse<Void> cancelInvitation(
            @PathVariable String workspaceId,
            @PathVariable String id,
            @AuthenticationPrincipal User currentUser) {
        invitationService.cancelInvitation(workspaceId, id, currentUser);
        return ApiResponse.success("Taklif bekor qilindi", null);
    }

    @Operation(summary = "Menga kelgan faol takliflarni ko'rish")
    @GetMapping("/invitations/me")
    public ApiResponse<List<WorkspaceInvitationDto>> getMyInvitations(
            @AuthenticationPrincipal User currentUser) {
        return ApiResponse.success("ok", invitationService.getMyPendingInvitations(currentUser));
    }

    @Operation(summary = "Taklifni qabul qilish")
    @PatchMapping("/invitations/{id}/accept")
    public ApiResponse<Void> acceptInvitation(
            @PathVariable String id,
            @AuthenticationPrincipal User currentUser) {
        invitationService.acceptInvitation(id, currentUser);
        return ApiResponse.success("Taklif qabul qilindi va jamoaga qo'shildingiz", null);
    }

    @Operation(summary = "Taklifni rad etish")
    @PatchMapping("/invitations/{id}/reject")
    public ApiResponse<Void> rejectInvitation(
            @PathVariable String id,
            @AuthenticationPrincipal User currentUser) {
        invitationService.rejectInvitation(id, currentUser);
        return ApiResponse.success("Taklif rad etildi", null);
    }
}
