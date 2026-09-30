package com.taskcenter.repository;

import com.taskcenter.dto.WorkspaceListDto;
import com.taskcenter.model.Workspace;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface WorkspaceRepository extends JpaRepository<Workspace, String> {
    List<Workspace> findAllByOwnerId(String ownerId);

    // JOIN bilan — subquery dan ancha tezroq (idx_workspace_members_user_id ishlatiladi)
    @Query("SELECT DISTINCT w FROM Workspace w LEFT JOIN WorkspaceMember wm ON wm.workspaceId = w.id WHERE w.ownerId = :userId OR wm.userId = :userId")
    List<Workspace> findByOwnerIdOrMemberUserId(@Param("userId") String userId);

    @Query("SELECT DISTINCT w FROM Workspace w LEFT JOIN WorkspaceMember wm ON wm.workspaceId = w.id WHERE w.ownerId = :userId OR wm.userId = :userId")
    Page<Workspace> findByOwnerIdOrMemberUserIdPaginated(@Param("userId") String userId, Pageable pageable);
}
