package com.taskcenter.repository;

import com.taskcenter.model.BoardColumn;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ColumnRepository extends JpaRepository<BoardColumn, String> {
    List<BoardColumn> findByWorkspaceIdOrderByOrderAsc(String workspaceId);
}
