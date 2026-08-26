package com.taskcenter.repository;

import com.taskcenter.model.Label;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface LabelRepository extends JpaRepository<Label, String> {
    List<Label> findByWorkspaceId(String workspaceId);
}
