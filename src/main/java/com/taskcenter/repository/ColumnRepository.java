package com.taskcenter.repository;

import com.taskcenter.model.BoardColumn;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

public interface ColumnRepository extends JpaRepository<BoardColumn, String> {
    List<BoardColumn> findByWorkspaceIdOrderByOrderAsc(String workspaceId);

    java.util.Optional<BoardColumn> findByIdAndWorkspaceId(String id, String workspaceId);

    // Cartesian Product portlashining oldini olish uchun faqat tasks va assignees EntityGraph orqali olinadi;
    // labels, directions, checklistItems va watchers @BatchSize(size = 25) orqali yengil IN so'rovlarda yuklanadi.
    @EntityGraph(attributePaths = {"tasks", "tasks.assignees"})
    @Query("SELECT DISTINCT c FROM BoardColumn c WHERE c.workspaceId = :workspaceId ORDER BY c.order ASC")
    List<BoardColumn> findByWorkspaceIdWithTasks(@Param("workspaceId") String workspaceId);

    @Query("SELECT COALESCE(MAX(c.order), 0) FROM BoardColumn c WHERE c.workspaceId = :workspaceId")
    Integer findMaxOrderByWorkspaceId(@Param("workspaceId") String workspaceId);

    @Transactional
    void deleteByWorkspaceId(String workspaceId);
}
