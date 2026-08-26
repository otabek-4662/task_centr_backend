package com.taskcenter.repository;

import com.taskcenter.model.Task;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface TaskRepository extends JpaRepository<Task, String> {
    List<Task> findByColumnIdOrderByOrderAsc(String columnId);
    List<Task> findByWorkspaceIdOrderByOrderAsc(String workspaceId);
    List<Task> findByWorkspaceId(String workspaceId);
}
