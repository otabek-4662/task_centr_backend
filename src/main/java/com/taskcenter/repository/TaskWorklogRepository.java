package com.taskcenter.repository;

import com.taskcenter.model.TaskWorklog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface TaskWorklogRepository extends JpaRepository<TaskWorklog, String> {
    List<TaskWorklog> findByTaskIdOrderByCreatedAtDesc(String taskId);

    @Query("SELECT COALESCE(SUM(w.hours), 0.0) FROM TaskWorklog w WHERE w.taskId = :taskId")
    Double sumHoursByTaskId(@Param("taskId") String taskId);
}
