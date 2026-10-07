package com.taskcenter.dto;

import com.taskcenter.model.WorkspaceRole;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class InviteRequestDto {
    @Schema(description = "Taklif qilinayotgan foydalanuvchi logini yoki emaili", example = "hamkasb@example.com")
    @NotBlank(message = "usernameOrEmail is required")
    private String usernameOrEmail;

    @Schema(description = "Taklif qilinayotgan rol", example = "MEMBER")
    @NotNull(message = "role is required")
    private WorkspaceRole role;
}
