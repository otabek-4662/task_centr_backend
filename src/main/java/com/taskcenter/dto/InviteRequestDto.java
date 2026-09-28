package com.taskcenter.dto;

import com.taskcenter.model.WorkspaceRole;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class InviteRequestDto {
    @NotBlank(message = "usernameOrEmail is required")
    private String usernameOrEmail;

    @NotNull(message = "role is required")
    private WorkspaceRole role;
}
