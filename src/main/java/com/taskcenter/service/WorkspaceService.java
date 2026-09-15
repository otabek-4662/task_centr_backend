package com.taskcenter.service;

import com.taskcenter.dto.WorkspaceCreateRequest;
import com.taskcenter.dto.WorkspaceDto;
import com.taskcenter.dto.WorkspaceListDto;
import com.taskcenter.dto.UserDto;
import com.taskcenter.exception.EntityNotFoundException;
import com.taskcenter.model.User;
import com.taskcenter.model.Workspace;
import com.taskcenter.model.WorkspaceMember;
import com.taskcenter.model.WorkspaceRole;
import com.taskcenter.repository.WorkspaceRepository;
import com.taskcenter.repository.WorkspaceMemberRepository;
import com.taskcenter.repository.UserRepository;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class WorkspaceService {

    private final WorkspaceRepository workspaceRepository;
    private final WorkspaceMemberRepository memberRepository;
    private final UserRepository userRepository;
    private final WorkspaceAuthorizationService authorizationService;

public WorkspaceService(WorkspaceRepository workspaceRepository,
                             WorkspaceMemberRepository memberRepository,
                             UserRepository userRepository,
                             WorkspaceAuthorizationService authorizationService) {
        this.workspaceRepository = workspaceRepository;
        this.memberRepository = memberRepository;
        this.userRepository = userRepository;
        this.authorizationService = authorizationService;
    }

    @Cacheable(value = "workspaces-by-user", key = "#currentUser.id + '-' + #page + '-' + #size")
    public List<WorkspaceListDto> getWorkspaceList(User currentUser, int page, int size) {
        if (page < 0 || size <= 0) {
            return workspaceRepository.findByOwnerIdOrMemberUserId(currentUser.getId())
                    .stream()
                    .map(WorkspaceListDto::fromEntity)
                    .collect(Collectors.toList());
        }
        if (size > 100) size = 100;
        Pageable pageable = PageRequest.of(page, size);
        Page<Workspace> workspacePage = workspaceRepository
                .findByOwnerIdOrMemberUserIdPaginated(currentUser.getId(), pageable);
        return workspacePage.getContent()
                .stream()
                .map(WorkspaceListDto::fromEntity)
                .collect(Collectors.toList());
    }

    @Cacheable(value = "workspaces-by-id", key = "#id")
    public WorkspaceDto getWorkspaceById(String id, User currentUser) {
        authorizationService.checkAccess(id, currentUser);
        Workspace ws = workspaceRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Workspace", id));
        return WorkspaceDto.fromEntity(ws);
    }

@CacheEvict(value = "workspaces-by-user", allEntries = true)
    public WorkspaceDto createWorkspace(WorkspaceCreateRequest req, User currentUser) {
        Workspace ws = Workspace.builder()
                .title(req.getTitle())
                .bgColor(req.getBgColor())
                .description(req.getDescription())
                .ownerId(currentUser.getId())
                .keyPrefix(resolveUniqueKeyPrefix(req.getTitle()))
                .build();
        ws = workspaceRepository.save(ws);

        WorkspaceMember ownerMember = WorkspaceMember.builder()
                .workspaceId(ws.getId())
                .userId(currentUser.getId())
                .role(WorkspaceRole.OWNER)
                .build();
        memberRepository.save(ownerMember);

        return WorkspaceDto.fromEntity(ws);
    }

    public UserDto addMemberToWorkspace(String workspaceId, String userId, User currentUser) {
        authorizationService.checkOwnerOrAdmin(workspaceId, currentUser);
        WorkspaceMember member = WorkspaceMember.builder()
                .workspaceId(workspaceId)
                .userId(userId)
                .role(WorkspaceRole.MEMBER)
                .build();
        memberRepository.save(member);
        return UserDto.fromEntity(userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User", userId)));
    }

    public void removeMemberFromWorkspace(String workspaceId, String userId, User currentUser) {
        authorizationService.checkOwnerOrAdmin(workspaceId, currentUser);
        WorkspaceMember member = memberRepository.findByWorkspaceIdAndUserId(workspaceId, userId)
                .orElseThrow(() -> new EntityNotFoundException("WorkspaceMember", workspaceId + "_" + userId));
        memberRepository.delete(member);
    }

    @Caching(evict = {
        @CacheEvict(value = "workspaces-by-id", key = "#id"),
        @CacheEvict(value = "workspaces-by-user", allEntries = true)
    })
    public WorkspaceDto updateWorkspace(String id, WorkspaceCreateRequest req, User currentUser) {
        authorizationService.checkOwner(id, currentUser);
        Workspace ws = workspaceRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Workspace", id));
        if (req.getTitle() != null) ws.setTitle(req.getTitle());
        if (req.getBgColor() != null) ws.setBgColor(req.getBgColor());
        if (req.getDescription() != null) ws.setDescription(req.getDescription());
        workspaceRepository.save(ws);
        return WorkspaceDto.fromEntity(ws);
    }

    @Caching(evict = {
        @CacheEvict(value = "workspaces-by-id", key = "#id"),
        @CacheEvict(value = "workspaces-by-user", allEntries = true)
    })
    public void deleteWorkspace(String id, User currentUser) {
        authorizationService.checkOwner(id, currentUser);
        workspaceRepository.deleteById(id);
    }

    /**
     * Bir xil nomli workspace'lar bir xil prefiks olmasligi uchun
     * band prefiksga raqamli qo'shimcha qo'yadi: WR -> WR2 -> WR3 ...
     * (key_prefix ustuni max 10 belgi).
     */
    private String resolveUniqueKeyPrefix(String title) {
        String base = generateKeyPrefix(title);
        String candidate = base;
        int suffix = 2;
        while (workspaceRepository.existsByKeyPrefix(candidate)) {
            String tail = String.valueOf(suffix++);
            int keep = Math.max(0, Math.min(base.length(), 10 - tail.length()));
            candidate = base.substring(0, keep) + tail;
        }
        return candidate;
    }

    private String generateKeyPrefix(String title) {
        if (title == null || title.isBlank()) return "WS";
        String[] words = title.trim().toUpperCase().split("\\s+");
        if (words.length == 1) {
            return words[0].substring(0, Math.min(4, words[0].length()));
        }
        StringBuilder prefix = new StringBuilder();
        for (String word : words) {
            if (!word.isEmpty()) prefix.append(word.charAt(0));
            if (prefix.length() >= 4) break;
        }
        return prefix.toString();
    }
}
