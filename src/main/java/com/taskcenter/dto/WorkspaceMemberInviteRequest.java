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
public class WorkspaceMemberInviteRequest {

    @Schema(description = "Foydalanuvchi logini yoki email manzili", example = "xusanboy@example.com")
    @NotBlank(message = "Foydalanuvchi nomi yoki email kiritilishi shart")
    private String usernameOrEmail;

    @Schema(description = "Taklif qilinayotgan rol", example = "MEMBER")
    @Builder.Default
    private WorkspaceRole role = WorkspaceRole.MEMBER;
}
