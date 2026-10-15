package com.taskcenter.repository;

import com.taskcenter.model.TaskDependency;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TaskDependencyRepository extends JpaRepository<TaskDependency, String> {
    List<TaskDependency> findByTaskId(String taskId);
    List<TaskDependency> findByDependsOnTaskId(String dependsOnTaskId);
    Optional<TaskDependency> findByTaskIdAndDependsOnTaskId(String taskId, String dependsOnTaskId);
    void deleteByTaskIdAndDependsOnTaskId(String taskId, String dependsOnTaskId);
}
