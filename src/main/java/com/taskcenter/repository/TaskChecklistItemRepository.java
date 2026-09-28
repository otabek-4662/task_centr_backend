package com.taskcenter.repository;

import com.taskcenter.model.TaskChecklistItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface TaskChecklistItemRepository extends JpaRepository<TaskChecklistItem, String> {
    List<TaskChecklistItem> findByTaskIdOrderByOrderIndexAsc(String taskId);

    @Query("SELECT COALESCE(MAX(c.orderIndex), 0) FROM TaskChecklistItem c WHERE c.taskId = :taskId")
    Integer findMaxOrderIndexByTaskId(@Param("taskId") String taskId);
}
