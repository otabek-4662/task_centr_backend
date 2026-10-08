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
public class WorkspaceMemberController {

    private final WorkspaceMemberService memberService;

    public WorkspaceMemberController(WorkspaceMemberService memberService) {
        this.memberService = memberService;
    }

    @Operation(operationId = "getWorkspaceMembers", summary = "Workspace a'zolari ro'yxatini rollari bilan olish",
            description = "Ishchi maydonga a'zo barcha foydalanuvchilar va ularning rollarini (OWNER, ADMIN, MEMBER, VIEWER) qaytaradi.")
    @GetMapping
    public ApiResponse<List<WorkspaceMemberResponseDto>> getMembers(
            @io.swagger.v3.oas.annotations.Parameter(description = "Ishchi maydon identifikatori (UUID)", example = "ws-123", required = true)
            @PathVariable String workspaceId,
            @AuthenticationPrincipal User currentUser) {
        List<WorkspaceMemberResponseDto> members = memberService.getWorkspaceMembers(workspaceId, currentUser);
        return ApiResponse.success("ok", members);
    }

    @Operation(operationId = "inviteWorkspaceMember", summary = "Workspace ga yangi a'zo taklif qilish / qo'shish",
            description = "Foydalanuvchi logini yoki email orqali uni berilgan rol bilan workspace a'zoligiga qo'shadi.")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<WorkspaceMemberResponseDto> addMember(
            @io.swagger.v3.oas.annotations.Parameter(description = "Ishchi maydon identifikatori (UUID)", example = "ws-123", required = true)
            @PathVariable String workspaceId,
            @Valid @RequestBody WorkspaceMemberInviteRequest request,
            @AuthenticationPrincipal User currentUser) {
        WorkspaceMemberResponseDto response = memberService.addMember(workspaceId, request, currentUser);
        return ApiResponse.success("A'zo muvaffaqiyatli qo'shildi", response);
    }

    @Operation(operationId = "updateWorkspaceMemberRole", summary = "Workspace a'zosining rolini o'zgartirish",
            description = "Mavjud a'zoning rolini yangilaydi (masalan MEMBER -> ADMIN). Faqat OWNER yoki ADMIN bajara oladi.")
    @PatchMapping("/{userId}")
    public ApiResponse<WorkspaceMemberResponseDto> updateMemberRole(
            @io.swagger.v3.oas.annotations.Parameter(description = "Ishchi maydon identifikatori (UUID)", example = "ws-123", required = true)
            @PathVariable String workspaceId,
            @io.swagger.v3.oas.annotations.Parameter(description = "Foydalanuvchi identifikatori (UUID)", example = "user-123", required = true)
            @PathVariable String userId,
            @Valid @RequestBody WorkspaceMemberRoleUpdateRequest request,
            @AuthenticationPrincipal User currentUser) {
        WorkspaceMemberResponseDto response = memberService.updateMemberRole(workspaceId, userId, request, currentUser);
        return ApiResponse.success("A'zo roli muvaffaqiyatli yangilandi", response);
    }

    @Operation(operationId = "removeWorkspaceMember", summary = "Workspace a'zosini chiqarib yuborish yoki jamoani tark etish",
            description = "A'zoni workspace tarkibidan chiqaradi. Admin boshqa a'zoni chiqarishi yoki a'zo o'zi chiqib ketishi mumkin.")
    @DeleteMapping("/{userId}")
    public ApiResponse<Void> removeMember(
            @io.swagger.v3.oas.annotations.Parameter(description = "Ishchi maydon identifikatori (UUID)", example = "ws-123", required = true)
            @PathVariable String workspaceId,
            @io.swagger.v3.oas.annotations.Parameter(description = "Foydalanuvchi identifikatori (UUID)", example = "user-123", required = true)
            @PathVariable String userId,
            @AuthenticationPrincipal User currentUser) {
        memberService.removeMember(workspaceId, userId, currentUser);
        return ApiResponse.success("A'zo jamoadan chiqarildi", null);
    }
}
