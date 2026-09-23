package com.taskcenter.repository;

import com.taskcenter.model.TaskActivity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TaskActivityRepository extends JpaRepository<TaskActivity, String> {

    @EntityGraph(attributePaths = {"user"})
    Page<TaskActivity> findByTaskIdOrderByCreatedAtDesc(String taskId, Pageable pageable);
}
