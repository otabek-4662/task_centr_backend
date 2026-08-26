package com.taskcenter.controller;

import com.taskcenter.dto.ApiResponse;
import com.taskcenter.dto.UserDto;
import com.taskcenter.model.User;
import com.taskcenter.model.WorkspaceMember;
import com.taskcenter.repository.UserRepository;
import com.taskcenter.repository.WorkspaceMemberRepository;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
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

    public UserController(UserRepository userRepository, WorkspaceMemberRepository memberRepository) {
        this.userRepository = userRepository;
        this.memberRepository = memberRepository;
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
    public ApiResponse<List<UserDto>> getUsers(@RequestParam(required = false) String workspaceId,
                                               @AuthenticationPrincipal User currentUser) {
        List<User> allUsers = userRepository.findAll();
        if (workspaceId == null || workspaceId.isBlank()) {
            List<UserDto> dtos = allUsers.stream().map(UserDto::fromEntity).collect(Collectors.toList());
            return ApiResponse.success("ok", dtos);
        }
        Set<String> memberIds = memberRepository.findByWorkspaceId(workspaceId)
                .stream().map(WorkspaceMember::getUserId).collect(Collectors.toSet());
        List<UserDto> dtos = allUsers.stream()
                .map(u -> UserDto.fromEntity(u, memberIds.contains(u.getId())))
                .collect(Collectors.toList());
        return ApiResponse.success("ok", dtos);
    }
}
