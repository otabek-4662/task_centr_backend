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

    @EntityGraph(attributePaths = {"tasks"})
    @Query("SELECT c FROM BoardColumn c WHERE c.workspaceId = :workspaceId ORDER BY c.order ASC")
    List<BoardColumn> findByWorkspaceIdWithTasks(@Param("workspaceId") String workspaceId);

    @Query("SELECT COALESCE(MAX(c.order), 0) FROM BoardColumn c WHERE c.workspaceId = :workspaceId")
    Integer findMaxOrderByWorkspaceId(@Param("workspaceId") String workspaceId);

    @Transactional
    void deleteByWorkspaceId(String workspaceId);
}
