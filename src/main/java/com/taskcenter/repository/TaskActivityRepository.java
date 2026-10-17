package com.taskcenter.repository;

import com.taskcenter.model.TaskActivity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface TaskActivityRepository extends JpaRepository<TaskActivity, String> {

    @EntityGraph(attributePaths = {"user"})
    Page<TaskActivity> findByTaskIdOrderByCreatedAtDesc(String taskId, Pageable pageable);

    @EntityGraph(attributePaths = {"user"})
    @Query("SELECT a FROM TaskActivity a WHERE a.taskId IN (" +
           "  SELECT t.id FROM Task t WHERE t.workspaceId = :workspaceId" +
           ") ORDER BY a.createdAt DESC")
    Page<TaskActivity> findByWorkspaceIdOrderByCreatedAtDesc(@Param("workspaceId") String workspaceId, Pageable pageable);

    @Override
    @EntityGraph(attributePaths = {"user"})
    Page<TaskActivity> findAll(Pageable pageable);
}
