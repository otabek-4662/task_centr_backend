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

    @EntityGraph(attributePaths = {"assignees", "watchers"})
    @Query("SELECT t FROM Task t WHERE t.id = :id")
    Optional<Task> findByIdWithAssigneesAndWatchers(@Param("id") String id);
    
    @Query("SELECT t.workspaceId FROM Task t WHERE t.id = :taskId")
    Optional<String> findWorkspaceIdById(@Param("taskId") String taskId);
    
    @EntityGraph(attributePaths = {"labels", "assignees"})
    @Query("SELECT t FROM Task t WHERE t.workspaceId = :workspaceId ORDER BY t.lexoRank ASC")
    List<Task> findByWorkspaceIdWithDetails(@Param("workspaceId") String workspaceId);

    @EntityGraph(attributePaths = {"assignees"})
    @Query("SELECT t FROM Task t WHERE t.workspaceId = :workspaceId")
    List<Task> findByWorkspaceIdWithAssignees(@Param("workspaceId") String workspaceId);
    
    @EntityGraph(attributePaths = {"labels", "assignees"})
    @Query("SELECT t FROM Task t WHERE t.workspaceId = :workspaceId")
    Page<Task> findByWorkspaceIdPaginated(@Param("workspaceId") String workspaceId, Pageable pageable);

    @EntityGraph(attributePaths = {"labels", "assignees"})
    @Query("SELECT t FROM Task t WHERE t.workspaceId = :workspaceId " +
           "AND (:#{#filter.columnId} IS NULL OR t.columnId = :#{#filter.columnId}) " +
           "AND (:#{#filter.priority} IS NULL OR t.priority = :#{#filter.priority}) " +
           "AND (:#{#filter.issueType} IS NULL OR t.issueType = :#{#filter.issueType}) " +
           "AND (:#{#filter.sprintId} IS NULL OR t.sprintId = :#{#filter.sprintId}) " +
           "AND (:#{#filter.status} IS NULL OR t.columnId = :#{#filter.status}) " +
           "AND (:#{#filter.assigneeId} IS NULL OR EXISTS (SELECT 1 FROM t.assignees a WHERE a.id = :#{#filter.assigneeId})) " +
           "AND (:#{#filter.assignedToMe} IS NULL OR (:#{#filter.assignedToMe} = true AND EXISTS (SELECT 1 FROM t.assignees a2 WHERE a2.id = :#{#filter.currentUserId}))) " +
           "AND (:#{#filter.isOverdue} IS NULL OR (:#{#filter.isOverdue} = true AND t.dueDate < CURRENT_DATE)) " +
           "AND (:#{#filter.dueToday} IS NULL OR (:#{#filter.dueToday} = true AND t.dueDate = CURRENT_DATE)) " +
           "AND (:#{#filter.dueThisWeek} IS NULL OR (:#{#filter.dueThisWeek} = true AND t.dueDate >= CURRENT_DATE AND t.dueDate <= :#{#filter.endOfWeek})) " +
           "AND (:#{#filter.includeArchived} = true OR t.isArchived = false) " +
           "AND (:#{#filter.search} IS NULL OR (" +
           "  LOWER(t.title) LIKE LOWER(CONCAT('%', :#{#filter.search}, '%')) OR " +
           "  (t.publicId IS NOT NULL AND LOWER(t.publicId) LIKE LOWER(CONCAT('%', :#{#filter.search}, '%'))) OR " +
           "  (t.description IS NOT NULL AND LOWER(t.description) LIKE LOWER(CONCAT('%', :#{#filter.search}, '%')))" +
           "))")
    Page<Task> findByWorkspaceIdFiltered(@Param("workspaceId") String workspaceId, @Param("filter") com.taskcenter.dto.TaskFilterRequest filter, Pageable pageable);

    @Query("SELECT MAX(t.lexoRank) FROM Task t WHERE t.columnId = :columnId")
    String findMaxLexoRankByColumnId(@Param("columnId") String columnId);

    @EntityGraph(attributePaths = {"labels", "assignees"})
    List<Task> findBySprintIdOrderByLexoRankAsc(String sprintId);

    @EntityGraph(attributePaths = {"labels", "assignees"})
    List<Task> findBySprintId(String sprintId);

    @EntityGraph(attributePaths = {"labels", "assignees"})
    @Query("SELECT t FROM Task t WHERE t.workspaceId = :workspaceId AND t.sprintId IS NULL AND t.isArchived = false ORDER BY t.lexoRank ASC")
    Page<Task> findBacklogTasks(@Param("workspaceId") String workspaceId, Pageable pageable);

    long countBySprintId(String sprintId);

    @Query("SELECT COUNT(t) FROM Task t JOIN t.assignees a WHERE a.id = :userId")
    long countAssignedTasksByUserId(@Param("userId") String userId);

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

    @Query("SELECT t FROM Task t JOIN t.assignees a WHERE a.id = :userId AND t.isArchived = false")
    List<Task> findActiveTasksByUserId(@Param("userId") String userId);

    @Query("SELECT new com.taskcenter.dto.TelegramReminderTaskDto(" +
           "t.id, t.title, t.workspaceId, t.dueDate, u.id, u.telegramChatId) " +
           "FROM Task t " +
           "JOIN t.assignees u " +
           "JOIN BoardColumn bc ON bc.id = t.columnId " +
           "WHERE t.deletedAt IS NULL " +
           "AND (t.isArchived = false OR t.isArchived IS NULL) " +
           "AND t.dueDate >= :startDate AND t.dueDate <= :endDate " +
           "AND bc.isDone = false " +
           "AND u.deletedAt IS NULL " +
           "AND u.telegramChatId IS NOT NULL " +
           "AND u.telegramNotifyDeadlines = true")
    List<com.taskcenter.dto.TelegramReminderTaskDto> findTasksWithAssigneesForTelegramReminder(
            @Param("startDate") java.time.LocalDate startDate,
            @Param("endDate") java.time.LocalDate endDate);

    @Query("SELECT new com.taskcenter.dto.TelegramReminderTaskDto(" +
           "t.id, t.title, t.workspaceId, t.dueDate, u.id, u.telegramChatId) " +
           "FROM Task t " +
           "JOIN t.assignees u " +
           "JOIN BoardColumn bc ON bc.id = t.columnId " +
           "WHERE t.deletedAt IS NULL " +
           "AND (t.isArchived = false OR t.isArchived IS NULL) " +
           "AND t.dueDate <= :today " +
           "AND bc.isDone = false " +
           "AND u.deletedAt IS NULL " +
           "AND u.telegramChatId IS NOT NULL " +
           "AND u.telegramDailyDigest = true")
    List<com.taskcenter.dto.TelegramReminderTaskDto> findTasksForTelegramDigest(
            @Param("today") java.time.LocalDate today);
}

