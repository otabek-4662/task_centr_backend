package com.taskcenter.repository;

import com.taskcenter.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface UserRepository extends JpaRepository<User, String> {
    Optional<User> findByName(String name);
    boolean existsByName(String name);
    
    @Query("SELECT u FROM User u WHERE u.id IN :ids")
    List<User> findByIdIn(@Param("ids") Set<String> ids);

    @Query("SELECT DISTINCT u FROM User u WHERE u.id = (" +
           "SELECT w.ownerId FROM Workspace w WHERE w.id = :workspaceId" +
           ") OR u.id IN (" +
           "SELECT wm.userId FROM WorkspaceMember wm WHERE wm.workspaceId = :workspaceId" +
           ")")
    Page<User> findByWorkspaceId(@Param("workspaceId") String workspaceId, Pageable pageable);
}
