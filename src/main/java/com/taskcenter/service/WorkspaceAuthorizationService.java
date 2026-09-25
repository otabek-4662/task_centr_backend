package com.taskcenter.service;

import com.taskcenter.exception.ForbiddenException;
import com.taskcenter.exception.ResourceNotFoundException;
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

    public Optional<WorkspaceRole> getRole(Workspace workspace, String userId) {
        if (workspace == null || userId == null) {
            return Optional.empty();
        }
        if (workspace.getOwnerId().equals(userId)) {
            return Optional.of(WorkspaceRole.OWNER);
        }
        return memberRepository.findByWorkspaceIdAndUserId(workspace.getId(), userId)
                .map(WorkspaceMember::getRole);
    }

    public boolean hasRole(Workspace workspace, String userId, WorkspaceRole... allowedRoles) {
        Optional<WorkspaceRole> role = getRole(workspace, userId);
        return role.isPresent() && Arrays.asList(allowedRoles).contains(role.get());
    }

    public boolean hasAccess(Workspace workspace, String userId) {
        return hasRole(workspace, userId, WorkspaceRole.OWNER, WorkspaceRole.ADMIN, WorkspaceRole.MEMBER, WorkspaceRole.VIEWER);
    }

    public boolean canEdit(Workspace workspace, String userId) {
        return hasRole(workspace, userId, WorkspaceRole.OWNER, WorkspaceRole.ADMIN, WorkspaceRole.MEMBER);
    }

    public boolean isViewer(Workspace workspace, String userId) {
        return hasRole(workspace, userId, WorkspaceRole.VIEWER);
    }

    public boolean isOwner(Workspace workspace, String userId) {
        return hasRole(workspace, userId, WorkspaceRole.OWNER);
    }

    public boolean isOwnerOrAdmin(Workspace workspace, String userId) {
        return hasRole(workspace, userId, WorkspaceRole.OWNER, WorkspaceRole.ADMIN);
    }

    public Workspace requireWorkspace(String workspaceId) {
        return workspaceRepository.findById(workspaceId)
                .orElseThrow(() -> new ResourceNotFoundException("Workspace topilmadi: " + workspaceId));
    }

    public Workspace checkAccess(String workspaceId, User currentUser) {
        Workspace workspace = requireWorkspace(workspaceId);
        if (currentUser == null || !hasAccess(workspace, currentUser.getId())) {
            throw new ForbiddenException("Ushbu workspace ga kirish uchun ruxsat yo'q");
        }
        return workspace;
    }

    public Workspace checkCanEdit(String workspaceId, User currentUser) {
        Workspace workspace = requireWorkspace(workspaceId);
        if (currentUser == null || !canEdit(workspace, currentUser.getId())) {
            throw new ForbiddenException("Kuzatuvchi (Viewer) roli ma'lumotlarni o'zgartira olmaydi");
        }
        return workspace;
    }

    public Workspace checkOwner(String workspaceId, User currentUser) {
        Workspace workspace = requireWorkspace(workspaceId);
        if (currentUser == null || !isOwner(workspace, currentUser.getId())) {
            throw new ForbiddenException("Ushbu amalni faqat workspace egasi bajara oladi");
        }
        return workspace;
    }

    public Workspace checkOwnerOrAdmin(String workspaceId, User currentUser) {
        Workspace workspace = requireWorkspace(workspaceId);
        if (currentUser == null || !isOwnerOrAdmin(workspace, currentUser.getId())) {
            throw new ForbiddenException("Ushbu amalni faqat workspace egasi yoki admini bajara oladi");
        }
        return workspace;
    }

    // Overloads for backward compatibility
    public boolean hasAccess(String workspaceId, String userId) {
        return hasAccess(requireWorkspace(workspaceId), userId);
    }

    public boolean canEdit(String workspaceId, String userId) {
        return canEdit(requireWorkspace(workspaceId), userId);
    }

    public boolean isViewer(String workspaceId, String userId) {
        return isViewer(requireWorkspace(workspaceId), userId);
    }

    public boolean isOwner(String workspaceId, String userId) {
        return isOwner(requireWorkspace(workspaceId), userId);
    }

    public boolean isOwnerOrAdmin(String workspaceId, String userId) {
        return isOwnerOrAdmin(requireWorkspace(workspaceId), userId);
    }
}
