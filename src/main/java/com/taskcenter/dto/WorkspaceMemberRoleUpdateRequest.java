package com.taskcenter.dto;

import com.taskcenter.model.WorkspaceRole;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkspaceMemberRoleUpdateRequest {

    @NotNull(message = "Rol kiritilishi shart")
    private WorkspaceRole role;
}
