package com.taskcenter.dto;

import com.taskcenter.model.WorkspaceRole;
import io.swagger.v3.oas.annotations.media.Schema;
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

    @Schema(description = "Yangi rol (OWNER, ADMIN, MEMBER, VIEWER)", example = "ADMIN")
    @NotNull(message = "Rol kiritilishi shart")
    private WorkspaceRole role;
}
