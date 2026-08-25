package com.taskcenter.repository;

import com.taskcenter.model.Task;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Optional;

public interface TaskRepository extends JpaRepository<Task, String> {
    List<Task> findByColumnIdOrderByOrderAsc(String columnId);
    List<Task> findByWorkspaceIdOrderByOrderAsc(String workspaceId);
    List<Task> findByWorkspaceId(String workspaceId);
    
    @EntityGraph(attributePaths = {"labels", "assignees"})
    @Query("SELECT t FROM Task t WHERE t.id = :id")
    Optional<Task> findByIdWithDetails(@Param("id") String id);
    
    @EntityGraph(attributePaths = {"labels", "assignees"})
    @Query("SELECT t FROM Task t WHERE t.workspaceId = :workspaceId ORDER BY t.order ASC")
    List<Task> findByWorkspaceIdWithDetails(@Param("workspaceId") String workspaceId);
    
    @Query("SELECT t FROM Task t WHERE t.workspaceId = :workspaceId")
    Page<Task> findByWorkspaceIdPaginated(@Param("workspaceId") String workspaceId, Pageable pageable);

    @Query("SELECT COALESCE(MAX(t.order), 0) FROM Task t WHERE t.columnId = :columnId")
    Integer findMaxOrderByColumnId(@Param("columnId") String columnId);

    @Transactional
    void deleteByColumnId(String columnId);

    @Transactional
    void deleteByWorkspaceId(String workspaceId);
}
