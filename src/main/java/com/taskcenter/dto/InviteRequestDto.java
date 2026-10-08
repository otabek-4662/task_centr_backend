package com.taskcenter.dto;

import com.taskcenter.model.WorkspaceRole;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(
        description = "Foydalanuvchini taklif qilish so'rovi",
        example = "{\"usernameOrEmail\": \"hamkasb@example.com\", \"role\": \"MEMBER\"}"
)
public class InviteRequestDto {
    @Schema(description = "Taklif qilinayotgan foydalanuvchi logini yoki emaili", example = "hamkasb@example.com", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "usernameOrEmail is required")
    private String usernameOrEmail;

    @Schema(description = "Taklif qilinayotgan rol", example = "MEMBER", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "role is required")
    private WorkspaceRole role;
}
