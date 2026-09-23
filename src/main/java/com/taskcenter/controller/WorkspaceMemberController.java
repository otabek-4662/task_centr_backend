package com.taskcenter.controller;

import com.taskcenter.dto.ApiResponse;
import com.taskcenter.dto.WorkspaceMemberInviteRequest;
import com.taskcenter.dto.WorkspaceMemberResponseDto;
import com.taskcenter.dto.WorkspaceMemberRoleUpdateRequest;
import com.taskcenter.model.User;
import com.taskcenter.service.WorkspaceMemberService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Workspace Members", description = "Workspace a'zolari bilan ishlash API lari")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/workspaces/{workspaceId}/members")
@CrossOrigin(origins = "*")
public class WorkspaceMemberController {

    private final WorkspaceMemberService memberService;

    public WorkspaceMemberController(WorkspaceMemberService memberService) {
        this.memberService = memberService;
    }

    @Operation(summary = "Workspace a'zolari ro'yxatini rollari bilan olish")
    @GetMapping
    public ApiResponse<List<WorkspaceMemberResponseDto>> getMembers(
            @PathVariable String workspaceId,
            @AuthenticationPrincipal User currentUser) {
        List<WorkspaceMemberResponseDto> members = memberService.getWorkspaceMembers(workspaceId, currentUser);
        return ApiResponse.success("ok", members);
    }

    @Operation(summary = "Workspace ga yangi a'zo taklif qilish / qo'shish")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<WorkspaceMemberResponseDto> addMember(
            @PathVariable String workspaceId,
            @Valid @RequestBody WorkspaceMemberInviteRequest request,
            @AuthenticationPrincipal User currentUser) {
        WorkspaceMemberResponseDto response = memberService.addMember(workspaceId, request, currentUser);
        return ApiResponse.success("A'zo muvaffaqiyatli qo'shildi", response);
    }

    @Operation(summary = "Workspace a'zosining rolini o'zgartirish")
    @PatchMapping("/{userId}")
    public ApiResponse<WorkspaceMemberResponseDto> updateMemberRole(
            @PathVariable String workspaceId,
            @PathVariable String userId,
            @Valid @RequestBody WorkspaceMemberRoleUpdateRequest request,
            @AuthenticationPrincipal User currentUser) {
        WorkspaceMemberResponseDto response = memberService.updateMemberRole(workspaceId, userId, request, currentUser);
        return ApiResponse.success("A'zo roli muvaffaqiyatli yangilandi", response);
    }

    @Operation(summary = "Workspace a'zosini chiqarib yuborish yoki jamoani tark etish")
    @DeleteMapping("/{userId}")
    public ApiResponse<Void> removeMember(
            @PathVariable String workspaceId,
            @PathVariable String userId,
            @AuthenticationPrincipal User currentUser) {
        memberService.removeMember(workspaceId, userId, currentUser);
        return ApiResponse.success("A'zo jamoadan chiqarildi", null);
    }
}
