package com.taskcenter.repository;

import com.taskcenter.model.BoardColumn;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

public interface ColumnRepository extends JpaRepository<BoardColumn, String> {
    List<BoardColumn> findByWorkspaceIdOrderByOrderAsc(String workspaceId);

    @Transactional
    void deleteByWorkspaceId(String workspaceId);
}
