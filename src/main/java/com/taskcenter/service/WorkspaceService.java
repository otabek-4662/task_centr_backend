package com.taskcenter.service;

import com.taskcenter.dto.WorkspaceCreateRequest;
import com.taskcenter.dto.WorkspaceDto;
import com.taskcenter.dto.WorkspaceListDto;
import com.taskcenter.exception.EntityNotFoundException;
import com.taskcenter.model.User;
import com.taskcenter.model.Workspace;
import com.taskcenter.repository.WorkspaceRepository;
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
    private final WorkspaceAuthorizationService authorizationService;

    public WorkspaceService(WorkspaceRepository workspaceRepository,
                            WorkspaceAuthorizationService authorizationService) {
        this.workspaceRepository = workspaceRepository;
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
                .build();
        workspaceRepository.save(ws);
        return WorkspaceDto.fromEntity(ws);
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
}
