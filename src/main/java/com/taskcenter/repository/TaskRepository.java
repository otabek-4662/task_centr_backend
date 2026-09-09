package com.taskcenter.repository;

import com.taskcenter.model.Task;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

public interface TaskRepository extends JpaRepository<Task, String> {
    List<Task> findByColumnIdOrderByOrderAsc(String columnId);
    List<Task> findByWorkspaceIdOrderByOrderAsc(String workspaceId);
    List<Task> findByWorkspaceId(String workspaceId);

    @Query("SELECT COALESCE(MAX(t.order), 0) FROM Task t WHERE t.columnId = :columnId")
    Integer findMaxOrderByColumnId(@Param("columnId") String columnId);

    @Transactional
    void deleteByColumnId(String columnId);

    @Transactional
    void deleteByWorkspaceId(String workspaceId);
}
