package com.taskcenter.service;

import com.taskcenter.model.Workspace;
import com.taskcenter.repository.WorkspaceRepository;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class WorkspaceService {

    private final WorkspaceRepository workspaceRepository;

    public WorkspaceService(WorkspaceRepository workspaceRepository) {
        this.workspaceRepository = workspaceRepository;
    }

    @Cacheable(value = "workspaces", key = "#userId")
    public List<Workspace> getWorkspacesByUser(String userId) {
        return workspaceRepository.findByOwnerIdOrMemberUserId(userId);
    }

    @Cacheable(value = "workspaces", key = "#id")
    public Optional<Workspace> getWorkspaceById(String id) {
        return workspaceRepository.findById(id);
    }

    @Caching(evict = {
        @CacheEvict(value = "workspaces", key = "#workspace.id"),
        @CacheEvict(value = "workspaces", key = "#workspace.ownerId")
    })
    public Workspace saveWorkspace(Workspace workspace) {
        return workspaceRepository.save(workspace);
    }

    @Caching(evict = {
        @CacheEvict(value = "workspaces", key = "#id"),
        @CacheEvict(value = "workspaces", allEntries = true)
    })
    public void deleteWorkspace(String id) {
        workspaceRepository.deleteById(id);
    }
}
