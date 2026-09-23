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

    @EntityGraph(attributePaths = {"labels", "assignees"})
    List<Task> findBySprintIdOrderByOrderAsc(String sprintId);

    List<Task> findBySprintId(String sprintId);

    @Query("SELECT t FROM Task t WHERE t.workspaceId = :workspaceId AND t.sprintId IS NULL ORDER BY t.order ASC")
    Page<Task> findBacklogTasks(@Param("workspaceId") String workspaceId, Pageable pageable);

    long countBySprintId(String sprintId);

    @Query("SELECT COALESCE(SUM(t.storyPoints), 0) FROM Task t WHERE t.sprintId = :sprintId")
    Integer sumStoryPointsBySprintId(@Param("sprintId") String sprintId);

    @Query("SELECT COALESCE(SUM(t.storyPoints), 0) FROM Task t WHERE t.workspaceId = :workspaceId AND t.sprintId IS NULL")
    Integer sumStoryPointsInBacklog(@Param("workspaceId") String workspaceId);

    @Query("SELECT t.sprintId as sprintId, COUNT(t) as taskCount, COALESCE(SUM(t.storyPoints), 0) as totalStoryPoints " +
           "FROM Task t WHERE t.workspaceId = :workspaceId AND t.sprintId IS NOT NULL GROUP BY t.sprintId")
    List<com.taskcenter.dto.SprintStatsProjection> findSprintStatsByWorkspaceId(@Param("workspaceId") String workspaceId);

    @Transactional
    void deleteByColumnId(String columnId);

    @Transactional
    void deleteByWorkspaceId(String workspaceId);
}
