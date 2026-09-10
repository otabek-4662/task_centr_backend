package com.taskcenter.service;

import com.taskcenter.model.User;
import com.taskcenter.model.Workspace;
import com.taskcenter.repository.WorkspaceMemberRepository;
import com.taskcenter.repository.WorkspaceRepository;
import org.springframework.stereotype.Service;

@Service
public class WorkspaceAuthorizationService {

    private final WorkspaceRepository workspaceRepository;
    private final WorkspaceMemberRepository memberRepository;

    public WorkspaceAuthorizationService(WorkspaceRepository workspaceRepository,
                                         WorkspaceMemberRepository memberRepository) {
        this.workspaceRepository = workspaceRepository;
        this.memberRepository = memberRepository;
    }

    public boolean hasAccess(String workspaceId, String userId) {
        Workspace workspace = workspaceRepository.findById(workspaceId).orElse(null);
        if (workspace == null || userId == null) {
            return false;
        }
        return workspace.getOwnerId().equals(userId)
                || memberRepository.existsByWorkspaceIdAndUserId(workspaceId, userId);
    }

    public boolean isOwner(String workspaceId, String userId) {
        Workspace workspace = workspaceRepository.findById(workspaceId).orElse(null);
        return workspace != null && userId != null && workspace.getOwnerId().equals(userId);
    }

    public void checkAccess(String workspaceId, User currentUser) {
        if (currentUser == null) {
            throw new RuntimeException("Ruxsat yo'q");
        }
        Workspace workspace = workspaceRepository.findById(workspaceId).orElse(null);
        if (workspace == null) {
            throw new RuntimeException("Workspace topilmadi");
        }
        if (!workspace.getOwnerId().equals(currentUser.getId())
                && !memberRepository.existsByWorkspaceIdAndUserId(workspaceId, currentUser.getId())) {
            throw new RuntimeException("Ruxsat yo'q");
        }
    }

    public void checkOwner(String workspaceId, User currentUser) {
        if (currentUser == null) {
            throw new RuntimeException("Ruxsat yo'q");
        }
        Workspace workspace = workspaceRepository.findById(workspaceId).orElse(null);
        if (workspace == null) {
            throw new RuntimeException("Workspace topilmadi");
        }
        if (!workspace.getOwnerId().equals(currentUser.getId())) {
            throw new RuntimeException("Ruxsat yo'q");
        }
    }
}
