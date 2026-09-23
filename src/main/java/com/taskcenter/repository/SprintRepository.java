package com.taskcenter.repository;

import com.taskcenter.model.Sprint;
import com.taskcenter.model.SprintStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

public interface SprintRepository extends JpaRepository<Sprint, String> {
    List<Sprint> findByWorkspaceIdOrderByCreatedAtDesc(String workspaceId);

    List<Sprint> findByWorkspaceIdAndStatusOrderByCreatedAtDesc(String workspaceId, SprintStatus status);

    Optional<Sprint> findByWorkspaceIdAndStatus(String workspaceId, SprintStatus status);

    @Transactional
    void deleteByWorkspaceId(String workspaceId);
}
