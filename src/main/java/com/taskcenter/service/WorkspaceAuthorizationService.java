package com.taskcenter.service;

import com.taskcenter.exception.ForbiddenException;
import com.taskcenter.exception.ResourceNotFoundException;
import com.taskcenter.model.User;
import com.taskcenter.model.Workspace;
import com.taskcenter.model.WorkspaceMember;
import com.taskcenter.model.WorkspaceRole;
import com.taskcenter.repository.WorkspaceMemberRepository;
import com.taskcenter.repository.WorkspaceRepository;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.Optional;

@Service
public class WorkspaceAuthorizationService {

    private final WorkspaceRepository workspaceRepository;
    private final WorkspaceMemberRepository memberRepository;
    private final CacheManager cacheManager;

    public WorkspaceAuthorizationService(WorkspaceRepository workspaceRepository,
                                         WorkspaceMemberRepository memberRepository) {
        this(workspaceRepository, memberRepository, null);
    }

    @org.springframework.beans.factory.annotation.Autowired
    public WorkspaceAuthorizationService(WorkspaceRepository workspaceRepository,
                                         WorkspaceMemberRepository memberRepository,
                                         CacheManager cacheManager) {
        this.workspaceRepository = workspaceRepository;
        this.memberRepository = memberRepository;
        this.cacheManager = cacheManager;
    }

    public Optional<WorkspaceRole> getRole(Workspace workspace, String userId) {
        if (workspace == null || userId == null) {
            return Optional.empty();
        }
        if (workspace.getOwnerId().equals(userId)) {
            return Optional.of(WorkspaceRole.OWNER);
        }
        return getRoleCached(workspace.getId(), userId);
    }

    public Optional<WorkspaceRole> getRole(String workspaceId, String userId) {
        if (workspaceId == null || userId == null) {
            return Optional.empty();
        }
        Workspace workspace = requireWorkspace(workspaceId);
        if (workspace.getOwnerId().equals(userId)) {
            return Optional.of(WorkspaceRole.OWNER);
        }
        return getRoleCached(workspaceId, userId);
    }

    private Optional<WorkspaceRole> getRoleCached(String workspaceId, String userId) {
        String cacheKey = workspaceId + ":" + userId;
        Cache cache = (cacheManager != null) ? cacheManager.getCache("workspaceRoles") : null;
        if (cache != null) {
            Cache.ValueWrapper wrapper = cache.get(cacheKey);
            if (wrapper != null) {
                @SuppressWarnings("unchecked")
                Optional<WorkspaceRole> cached = (Optional<WorkspaceRole>) wrapper.get();
                return cached;
            }
        }

        Optional<WorkspaceRole> role = memberRepository.findByWorkspaceIdAndUserId(workspaceId, userId)
                .map(WorkspaceMember::getRole);

        if (cache != null) {
            cache.put(cacheKey, role);
        }
        return role;
    }

    public void evictRole(String workspaceId, String userId) {
        if (cacheManager != null && workspaceId != null && userId != null) {
            Cache cache = cacheManager.getCache("workspaceRoles");
            if (cache != null) {
                cache.evict(workspaceId + ":" + userId);
            }
        }
    }

    public void evictWorkspace(String workspaceId) {
        if (cacheManager != null && workspaceId != null) {
            Cache wsCache = cacheManager.getCache("workspaces");
            if (wsCache != null) {
                wsCache.evict(workspaceId);
            }
            Cache roleCache = cacheManager.getCache("workspaceRoles");
            if (roleCache != null) {
                roleCache.clear();
            }
        }
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
        if (workspaceId == null) {
            throw new ResourceNotFoundException("Workspace topilmadi: null");
        }
        Cache cache = (cacheManager != null) ? cacheManager.getCache("workspaces") : null;
        if (cache != null) {
            Workspace cached = cache.get(workspaceId, Workspace.class);
            if (cached != null) {
                return cached;
            }
        }
        Workspace ws = workspaceRepository.findById(workspaceId)
                .orElseThrow(() -> new ResourceNotFoundException("Workspace topilmadi: " + workspaceId));
        if (cache != null) {
            cache.put(workspaceId, ws);
        }
        return ws;
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

    public Workspace checkAdmin(String workspaceId, User currentUser) {
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
