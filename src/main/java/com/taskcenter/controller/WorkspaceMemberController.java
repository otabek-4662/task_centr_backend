package com.taskcenter.controller;

import com.taskcenter.dto.ApiResponse;
import com.taskcenter.dto.UserDto;
import com.taskcenter.model.User;
import com.taskcenter.service.WorkspaceMemberService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
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

    @Operation(summary = "Workspace a'zolari ro'yxatini olish")
    @GetMapping
    public ApiResponse<List<UserDto>> getMembers(
            @PathVariable String workspaceId,
            @AuthenticationPrincipal User currentUser) {
        List<UserDto> members = memberService.getMembers(workspaceId, currentUser);
        return ApiResponse.success("ok", members);
    }
}
