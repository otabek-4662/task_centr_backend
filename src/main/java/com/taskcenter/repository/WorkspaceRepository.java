package com.taskcenter.repository;

import com.taskcenter.model.Workspace;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface WorkspaceRepository extends JpaRepository<Workspace, String> {
    List<Workspace> findAllByOwnerId(String ownerId);

    @Query("SELECT w FROM Workspace w WHERE w.ownerId = :userId OR w.id IN (SELECT wm.workspaceId FROM WorkspaceMember wm WHERE wm.userId = :userId)")
    List<Workspace> findByOwnerIdOrMemberUserId(@Param("userId") String userId);
}
