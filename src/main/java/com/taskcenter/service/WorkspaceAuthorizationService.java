package com.taskcenter.service;

import com.taskcenter.model.User;
import com.taskcenter.model.Workspace;
import com.taskcenter.model.WorkspaceMember;
import com.taskcenter.model.WorkspaceRole;
import com.taskcenter.repository.WorkspaceMemberRepository;
import com.taskcenter.repository.WorkspaceRepository;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.Optional;

@Service
public class WorkspaceAuthorizationService {

    private final WorkspaceRepository workspaceRepository;
    private final WorkspaceMemberRepository memberRepository;

    public WorkspaceAuthorizationService(WorkspaceRepository workspaceRepository,
                                         WorkspaceMemberRepository memberRepository) {
        this.workspaceRepository = workspaceRepository;
        this.memberRepository = memberRepository;
    }

    public Optional<WorkspaceRole> getRole(String workspaceId, String userId) {
        Workspace workspace = workspaceRepository.findById(workspaceId).orElse(null);
        if (workspace == null || userId == null) {
            return Optional.empty();
        }
        if (workspace.getOwnerId().equals(userId)) {
            return Optional.of(WorkspaceRole.OWNER);
        }
        return memberRepository.findByWorkspaceIdAndUserId(workspaceId, userId)
                .map(WorkspaceMember::getRole);
    }

    public boolean hasRole(String workspaceId, String userId, WorkspaceRole... allowedRoles) {
        Optional<WorkspaceRole> role = getRole(workspaceId, userId);
        return role.isPresent() && Arrays.asList(allowedRoles).contains(role.get());
    }

    public boolean hasAccess(String workspaceId, String userId) {
        return hasRole(workspaceId, userId, WorkspaceRole.OWNER, WorkspaceRole.ADMIN, WorkspaceRole.MEMBER);
    }

    public boolean isOwner(String workspaceId, String userId) {
        return hasRole(workspaceId, userId, WorkspaceRole.OWNER);
    }

    public boolean isOwnerOrAdmin(String workspaceId, String userId) {
        return hasRole(workspaceId, userId, WorkspaceRole.OWNER, WorkspaceRole.ADMIN);
    }

    private Workspace requireWorkspace(String workspaceId) {
        Workspace workspace = workspaceRepository.findById(workspaceId).orElse(null);
        if (workspace == null) {
            throw new RuntimeException("Workspace topilmadi");
        }
        return workspace;
    }

    public void checkAccess(String workspaceId, User currentUser) {
        requireWorkspace(workspaceId);
        if (currentUser == null || !hasAccess(workspaceId, currentUser.getId())) {
            throw new RuntimeException("Ruxsat yo'q");
        }
    }

    public void checkOwner(String workspaceId, User currentUser) {
        requireWorkspace(workspaceId);
        if (currentUser == null || !isOwner(workspaceId, currentUser.getId())) {
            throw new RuntimeException("Ruxsat yo'q");
        }
    }

    public void checkOwnerOrAdmin(String workspaceId, User currentUser) {
        requireWorkspace(workspaceId);
        if (currentUser == null || !isOwnerOrAdmin(workspaceId, currentUser.getId())) {
            throw new RuntimeException("Ruxsat yo'q");
        }
    }
}
