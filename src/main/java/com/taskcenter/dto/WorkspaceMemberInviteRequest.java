package com.taskcenter.dto;

import com.taskcenter.model.WorkspaceRole;
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

    @NotBlank(message = "Foydalanuvchi nomi yoki email kiritilishi shart")
    private String usernameOrEmail;

    @Builder.Default
    private WorkspaceRole role = WorkspaceRole.MEMBER;
}
