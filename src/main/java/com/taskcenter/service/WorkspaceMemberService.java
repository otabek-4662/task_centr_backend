package com.taskcenter.service;

import com.taskcenter.dto.UserDto;
import com.taskcenter.model.User;
import com.taskcenter.model.WorkspaceMember;
import com.taskcenter.repository.UserRepository;
import com.taskcenter.repository.WorkspaceMemberRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class WorkspaceMemberService {

    private final WorkspaceMemberRepository memberRepository;
    private final UserRepository userRepository;
    private final WorkspaceAuthorizationService authorizationService;

    public WorkspaceMemberService(WorkspaceMemberRepository memberRepository,
                                  UserRepository userRepository,
                                  WorkspaceAuthorizationService authorizationService) {
        this.memberRepository = memberRepository;
        this.userRepository = userRepository;
        this.authorizationService = authorizationService;
    }

    @Transactional(readOnly = true)
    public List<UserDto> getMembers(String workspaceId, User currentUser) {
        authorizationService.checkAccess(workspaceId, currentUser);

        List<WorkspaceMember> members = memberRepository.findByWorkspaceId(workspaceId);
        Set<String> memberIds = members.stream()
                .map(WorkspaceMember::getUserId)
                .collect(Collectors.toSet());

        return userRepository.findByIdIn(memberIds)
                .stream()
                .map(UserDto::fromEntity)
                .collect(Collectors.toList());
    }
}
