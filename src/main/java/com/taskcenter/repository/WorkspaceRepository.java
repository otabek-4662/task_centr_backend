package com.taskcenter.repository;

import com.taskcenter.model.Workspace;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WorkspaceRepository extends JpaRepository<Workspace, String> {
}
