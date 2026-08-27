package com.taskcenter.repository;

import com.taskcenter.model.Workspace;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface WorkspaceRepository extends JpaRepository<Workspace, String> {
    List<Workspace> findAllByOwnerId(String ownerId);
}
