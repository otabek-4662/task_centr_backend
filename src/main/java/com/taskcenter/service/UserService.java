package com.taskcenter.service;

import com.taskcenter.dto.UserDto;
import com.taskcenter.exception.ForbiddenException;
import com.taskcenter.model.User;
import com.taskcenter.model.WorkspaceMember;
import com.taskcenter.repository.UserRepository;
import com.taskcenter.repository.WorkspaceMemberRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final WorkspaceMemberRepository memberRepository;
    private final WorkspaceAuthorizationService authorizationService;

    public UserService(UserRepository userRepository,
                       WorkspaceMemberRepository memberRepository,
                       WorkspaceAuthorizationService authorizationService) {
        this.userRepository = userRepository;
        this.memberRepository = memberRepository;
        this.authorizationService = authorizationService;
    }

    public UserDto getCurrentUser(User currentUser) {
        if (currentUser == null) {
            throw new ForbiddenException("Foydalanuvchi tizimga kirmagan");
        }
        return UserDto.fromEntity(currentUser);
    }

    @Transactional(readOnly = true)
    public Page<UserDto> getUsers(String workspaceId, User currentUser, Pageable pageable) {
        Page<User> userPage;
        if (workspaceId == null || workspaceId.isBlank()) {
            userPage = userRepository.findAll(pageable);
        } else {
            authorizationService.checkAccess(workspaceId, currentUser);
            Set<String> memberIds = memberRepository.findByWorkspaceId(workspaceId)
                    .stream()
                    .map(WorkspaceMember::getUserId)
                    .collect(Collectors.toSet());

            List<User> members = userRepository.findAllById(memberIds);
            int start = (int) pageable.getOffset();
            int end = Math.min(start + pageable.getPageSize(), members.size());

            List<User> pagedMembers = (start <= end && start < members.size())
                    ? members.subList(start, end)
                    : List.of();

            userPage = new PageImpl<>(pagedMembers, pageable, members.size());
        }
        return userPage.map(UserDto::fromEntity);
    }
}
