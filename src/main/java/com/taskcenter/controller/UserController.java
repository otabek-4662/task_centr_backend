package com.taskcenter.controller;

import com.taskcenter.dto.ApiResponse;
import com.taskcenter.dto.UserDto;
import com.taskcenter.model.User;
import com.taskcenter.model.WorkspaceMember;
import com.taskcenter.repository.UserRepository;
import com.taskcenter.repository.WorkspaceMemberRepository;
import com.taskcenter.service.WorkspaceAuthorizationService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class UserController {

    private final UserRepository userRepository;
    private final WorkspaceMemberRepository memberRepository;
    private final WorkspaceAuthorizationService authorizationService;

    public UserController(UserRepository userRepository,
                          WorkspaceMemberRepository memberRepository,
                          WorkspaceAuthorizationService authorizationService) {
        this.userRepository = userRepository;
        this.memberRepository = memberRepository;
        this.authorizationService = authorizationService;
    }

    @GetMapping("/me")
    public ApiResponse<UserDto> getMe(@AuthenticationPrincipal User currentUser) {
        if (currentUser == null) {
            throw new RuntimeException("Unauthorized");
        }
        return ApiResponse.success("ok", UserDto.fromEntity(currentUser));
    }

    @GetMapping("/auth/me")
    public ApiResponse<UserDto> getAuthMe(@AuthenticationPrincipal User currentUser) {
        return getMe(currentUser);
    }

    @GetMapping("/users")
    public ApiResponse<Page<UserDto>> getUsers(@RequestParam(required = false) String workspaceId,
                                               @AuthenticationPrincipal User currentUser,
                                               @PageableDefault(size = 20, sort = "name") Pageable pageable) {
        Page<User> userPage;
        if (workspaceId == null || workspaceId.isBlank()) {
            userPage = userRepository.findAll(pageable);
        } else {
            authorizationService.checkAccess(workspaceId, currentUser);
            Set<String> memberIds = memberRepository.findByWorkspaceId(workspaceId)
                    .stream().map(WorkspaceMember::getUserId).collect(Collectors.toSet());
            List<User> members = userRepository.findAllById(memberIds);
            int start = (int) pageable.getOffset();
            int end = Math.min(start + pageable.getPageSize(), members.size());
            List<User> pagedMembers = members.subList(start, end);
            userPage = new org.springframework.data.domain.PageImpl<>(pagedMembers, pageable, members.size());
        }
        Page<UserDto> dtoPage = userPage.map(UserDto::fromEntity);
        return ApiResponse.success("ok", dtoPage);
    }
}
