package com.taskcenter.repository;

import com.taskcenter.model.Task;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

public interface TaskRepository extends JpaRepository<Task, String> {
    List<Task> findByColumnIdOrderByOrderAsc(String columnId);
    List<Task> findByWorkspaceIdOrderByOrderAsc(String workspaceId);
    List<Task> findByWorkspaceId(String workspaceId);

    @Transactional
    void deleteByColumnId(String columnId);

    @Transactional
    void deleteByWorkspaceId(String workspaceId);
}
