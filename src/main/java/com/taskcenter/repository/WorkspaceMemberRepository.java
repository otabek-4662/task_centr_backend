package com.taskcenter.repository;

import com.taskcenter.model.WorkspaceMember;
import com.taskcenter.model.WorkspaceMemberId;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface WorkspaceMemberRepository extends JpaRepository<WorkspaceMember, WorkspaceMemberId> {
    List<WorkspaceMember> findByWorkspaceId(String workspaceId);
    List<WorkspaceMember> findByUserId(String userId);
    boolean existsByWorkspaceIdAndUserId(String workspaceId, String userId);
}
