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
    List<Task> findByColumnIdOrderByLexoRankAsc(String columnId);
    List<Task> findByWorkspaceIdOrderByLexoRankAsc(String workspaceId);
    List<Task> findByWorkspaceId(String workspaceId);
    
    @EntityGraph(attributePaths = {"labels", "assignees"})
    Optional<Task> findByPublicId(String publicId);
    
    @EntityGraph(attributePaths = {"labels", "assignees"})
    @Query("SELECT t FROM Task t WHERE t.id = :id")
    Optional<Task> findByIdWithDetails(@Param("id") String id);
    
    @Query("SELECT t.workspaceId FROM Task t WHERE t.id = :taskId")
    Optional<String> findWorkspaceIdById(@Param("taskId") String taskId);
    
    @EntityGraph(attributePaths = {"labels", "assignees"})
    @Query("SELECT t FROM Task t WHERE t.workspaceId = :workspaceId ORDER BY t.lexoRank ASC")
    List<Task> findByWorkspaceIdWithDetails(@Param("workspaceId") String workspaceId);

    @EntityGraph(attributePaths = {"assignees"})
    @Query("SELECT t FROM Task t WHERE t.workspaceId = :workspaceId")
    List<Task> findByWorkspaceIdWithAssignees(@Param("workspaceId") String workspaceId);
    
    @Query("SELECT t FROM Task t WHERE t.workspaceId = :workspaceId")
    Page<Task> findByWorkspaceIdPaginated(@Param("workspaceId") String workspaceId, Pageable pageable);

    @Query("SELECT MAX(t.lexoRank) FROM Task t WHERE t.columnId = :columnId")
    String findMaxLexoRankByColumnId(@Param("columnId") String columnId);

    @EntityGraph(attributePaths = {"labels", "assignees"})
    List<Task> findBySprintIdOrderByLexoRankAsc(String sprintId);

    List<Task> findBySprintId(String sprintId);

    @Query("SELECT t FROM Task t WHERE t.workspaceId = :workspaceId AND t.sprintId IS NULL ORDER BY t.lexoRank ASC")
    Page<Task> findBacklogTasks(@Param("workspaceId") String workspaceId, Pageable pageable);

    long countBySprintId(String sprintId);

    @Query("SELECT COALESCE(SUM(t.storyPoints), 0) FROM Task t WHERE t.sprintId = :sprintId")
    Integer sumStoryPointsBySprintId(@Param("sprintId") String sprintId);

    @Query("SELECT COALESCE(SUM(t.storyPoints), 0) FROM Task t WHERE t.workspaceId = :workspaceId AND t.sprintId IS NULL")
    Integer sumStoryPointsInBacklog(@Param("workspaceId") String workspaceId);

    @Query("SELECT t.sprintId as sprintId, COUNT(t) as taskCount, COALESCE(SUM(t.storyPoints), 0) as totalStoryPoints " +
           "FROM Task t WHERE t.workspaceId = :workspaceId AND t.sprintId IS NOT NULL GROUP BY t.sprintId")
    List<com.taskcenter.dto.SprintStatsProjection> findSprintStatsByWorkspaceId(@Param("workspaceId") String workspaceId);

    @Query("SELECT t.columnId AS columnId, COUNT(t.id) AS taskCount " +
           "FROM Task t WHERE t.workspaceId = :workspaceId " +
           "GROUP BY t.columnId")
    List<com.taskcenter.dto.ColumnTaskCountProjection> getWorkspaceTaskCountsByColumn(@Param("workspaceId") String workspaceId);

    @Query("SELECT u.id AS userId, u.name AS userName, u.fullName AS userFullName, " +
           "COUNT(t.id) AS totalTasks, " +
           "SUM(CASE WHEN t.columnId = :doneColumnId THEN 1 ELSE 0 END) AS completedTasks, " +
           "SUM(CASE WHEN t.columnId != :doneColumnId THEN 1 ELSE 0 END) AS activeTasks " +
           "FROM Task t JOIN t.assignees u " +
           "WHERE t.workspaceId = :workspaceId " +
           "GROUP BY u.id, u.name, u.fullName")
    List<com.taskcenter.dto.UserWorkloadProjection> getWorkspaceWorkloadStats(@Param("workspaceId") String workspaceId, @Param("doneColumnId") String doneColumnId);

    @Transactional
    void deleteByColumnId(String columnId);

    @Transactional
    void deleteByWorkspaceId(String workspaceId);
}
