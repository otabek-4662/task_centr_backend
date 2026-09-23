package com.taskcenter.service;

import com.taskcenter.dto.UserDto;
import com.taskcenter.dto.WorkspaceMemberInviteRequest;
import com.taskcenter.dto.WorkspaceMemberResponseDto;
import com.taskcenter.dto.WorkspaceMemberRoleUpdateRequest;
import com.taskcenter.exception.BadRequestException;
import com.taskcenter.exception.ConflictException;
import com.taskcenter.exception.ForbiddenException;
import com.taskcenter.exception.ResourceNotFoundException;
import com.taskcenter.model.User;
import com.taskcenter.model.Workspace;
import com.taskcenter.model.WorkspaceMember;
import com.taskcenter.model.WorkspaceRole;
import com.taskcenter.repository.UserRepository;
import com.taskcenter.repository.WorkspaceMemberRepository;
import com.taskcenter.repository.WorkspaceRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class WorkspaceMemberService {

    private final WorkspaceMemberRepository memberRepository;
    private final UserRepository userRepository;
    private final WorkspaceRepository workspaceRepository;
    private final WorkspaceAuthorizationService authorizationService;

    public WorkspaceMemberService(WorkspaceMemberRepository memberRepository,
                                  UserRepository userRepository,
                                  WorkspaceRepository workspaceRepository,
                                  WorkspaceAuthorizationService authorizationService) {
        this.memberRepository = memberRepository;
        this.userRepository = userRepository;
        this.workspaceRepository = workspaceRepository;
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

    @Transactional(readOnly = true)
    public List<WorkspaceMemberResponseDto> getWorkspaceMembers(String workspaceId, User currentUser) {
        authorizationService.checkAccess(workspaceId, currentUser);
        Workspace workspace = getWorkspace(workspaceId);

        List<WorkspaceMember> members = memberRepository.findByWorkspaceId(workspaceId);
        Map<String, WorkspaceRole> roleMap = members.stream()
                .collect(Collectors.toMap(WorkspaceMember::getUserId, WorkspaceMember::getRole));

        Set<String> allUserIds = new HashSet<>(roleMap.keySet());
        allUserIds.add(workspace.getOwnerId());

        List<User> users = userRepository.findByIdIn(allUserIds);
        List<WorkspaceMemberResponseDto> result = new ArrayList<>();

        for (User u : users) {
            WorkspaceRole role = u.getId().equals(workspace.getOwnerId())
                    ? WorkspaceRole.OWNER
                    : roleMap.get(u.getId());
            if (role != null) {
                result.add(WorkspaceMemberResponseDto.fromEntity(u, role));
            }
        }

        return result;
    }

    @Transactional
    public WorkspaceMemberResponseDto addMember(String workspaceId, WorkspaceMemberInviteRequest request, User currentUser) {
        authorizationService.checkOwnerOrAdmin(workspaceId, currentUser);
        Workspace workspace = getWorkspace(workspaceId);

        String query = request.getUsernameOrEmail().trim();
        User targetUser = userRepository.findByNameOrEmail(query)
                .orElseThrow(() -> new ResourceNotFoundException("Foydalanuvchi topilmadi: " + query));

        if (workspace.getOwnerId().equals(targetUser.getId())) {
            throw new BadRequestException("Workspace egasi allaqachon loyiha egasi hisoblanadi");
        }

        if (memberRepository.existsByWorkspaceIdAndUserId(workspaceId, targetUser.getId())) {
            throw new ConflictException("Ushbu foydalanuvchi allaqachon workspace a'zosi");
        }

        WorkspaceRole roleToAssign = request.getRole() != null ? request.getRole() : WorkspaceRole.MEMBER;
        if (roleToAssign == WorkspaceRole.OWNER) {
            throw new BadRequestException("A'zoga OWNER rolini berib bo'lmaydi");
        }

        WorkspaceMember member = WorkspaceMember.builder()
                .workspaceId(workspaceId)
                .userId(targetUser.getId())
                .role(roleToAssign)
                .build();
        memberRepository.save(member);

        return WorkspaceMemberResponseDto.fromEntity(targetUser, member.getRole());
    }

    @Transactional
    public WorkspaceMemberResponseDto updateMemberRole(String workspaceId, String targetUserId, WorkspaceMemberRoleUpdateRequest request, User currentUser) {
        authorizationService.checkOwnerOrAdmin(workspaceId, currentUser);
        Workspace workspace = getWorkspace(workspaceId);

        if (workspace.getOwnerId().equals(targetUserId)) {
            throw new BadRequestException("Workspace egasining rolini o'zgartirib bo'lmaydi");
        }

        if (request.getRole() == WorkspaceRole.OWNER) {
            throw new BadRequestException("A'zo rolini OWNER ga o'zgartirib bo'lmaydi");
        }

        WorkspaceMember member = memberRepository.findByWorkspaceIdAndUserId(workspaceId, targetUserId)
                .orElseThrow(() -> new ResourceNotFoundException("A'zo topilmadi"));

        boolean isCurrentUserOwner = authorizationService.isOwner(workspaceId, currentUser.getId());
        if (!isCurrentUserOwner && member.getRole() == WorkspaceRole.ADMIN) {
            throw new ForbiddenException("Faqat workspace egasi boshqa adminning rolini o'zgartirishi mumkin");
        }

        member.setRole(request.getRole());
        memberRepository.save(member);

        User targetUser = userRepository.findById(targetUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Foydalanuvchi topilmadi"));

        return WorkspaceMemberResponseDto.fromEntity(targetUser, member.getRole());
    }

    @Transactional
    public void removeMember(String workspaceId, String targetUserId, User currentUser) {
        Workspace workspace = getWorkspace(workspaceId);

        if (workspace.getOwnerId().equals(targetUserId)) {
            throw new BadRequestException("Workspace egasini jamoadan chiqarib bo'lmaydi");
        }

        boolean isSelf = targetUserId.equals(currentUser.getId());
        if (!isSelf) {
            authorizationService.checkOwnerOrAdmin(workspaceId, currentUser);
            boolean isCurrentUserOwner = authorizationService.isOwner(workspaceId, currentUser.getId());
            WorkspaceMember member = memberRepository.findByWorkspaceIdAndUserId(workspaceId, targetUserId)
                    .orElseThrow(() -> new ResourceNotFoundException("A'zo topilmadi"));

            if (!isCurrentUserOwner && member.getRole() == WorkspaceRole.ADMIN) {
                throw new ForbiddenException("Faqat workspace egasi boshqa adminni jamoadan chiqarishi mumkin");
            }
            memberRepository.delete(member);
        } else {
            authorizationService.checkAccess(workspaceId, currentUser);
            WorkspaceMember member = memberRepository.findByWorkspaceIdAndUserId(workspaceId, targetUserId)
                    .orElseThrow(() -> new ResourceNotFoundException("Siz ushbu workspace a'zosi emassiz"));
            memberRepository.delete(member);
        }
    }

    private Workspace getWorkspace(String workspaceId) {
        return workspaceRepository.findById(workspaceId)
                .orElseThrow(() -> new ResourceNotFoundException("Workspace topilmadi: " + workspaceId));
    }
}
