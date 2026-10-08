package com.taskcenter.dto;

import com.taskcenter.model.WorkspaceRole;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(
        description = "Workspace a'zoligiga taklif so'rovi",
        example = "{\"usernameOrEmail\": \"xusanboy@example.com\", \"role\": \"MEMBER\"}"
)
public class WorkspaceMemberInviteRequest {

    @Schema(description = "Foydalanuvchi logini yoki email manzili", example = "xusanboy@example.com", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "Foydalanuvchi nomi yoki email kiritilishi shart")
    private String usernameOrEmail;

    @Schema(description = "Taklif qilinayotgan rol", example = "MEMBER", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    @Builder.Default
    private WorkspaceRole role = WorkspaceRole.MEMBER;
}
