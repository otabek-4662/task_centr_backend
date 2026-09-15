package com.taskcenter.repository;

import com.taskcenter.model.WorkspaceMember;
import com.taskcenter.model.WorkspaceMemberId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Optional;

public interface WorkspaceMemberRepository extends JpaRepository<WorkspaceMember, WorkspaceMemberId> {
    List<WorkspaceMember> findByWorkspaceId(String workspaceId);
    List<WorkspaceMember> findByUserId(String userId);
    boolean existsByWorkspaceIdAndUserId(String workspaceId, String userId);
    Optional<WorkspaceMember> findByWorkspaceIdAndUserId(String workspaceId, String userId);

    @Transactional
    void deleteByWorkspaceId(String workspaceId);
}
