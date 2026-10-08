package com.taskcenter.repository;

import com.taskcenter.model.Direction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface DirectionRepository extends JpaRepository<Direction, String> {
    List<Direction> findByWorkspaceId(String workspaceId);
    Optional<Direction> findByIdAndWorkspaceId(String id, String workspaceId);
    List<Direction> findAllByIdIn(Collection<String> ids);

    boolean existsByWorkspaceIdAndNameIgnoreCase(String workspaceId, String name);
    Optional<Direction> findByWorkspaceIdAndNameIgnoreCase(String workspaceId, String name);

    @Modifying
    @Query(value = "DELETE FROM task_directions WHERE direction_id = :directionId", nativeQuery = true)
    void unlinkDirectionFromAllTasks(@Param("directionId") String directionId);
}
