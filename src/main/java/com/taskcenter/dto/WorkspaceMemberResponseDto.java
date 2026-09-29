package com.taskcenter.dto;

import com.taskcenter.model.User;
import com.taskcenter.model.WorkspaceRole;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkspaceMemberResponseDto {
    private String id;
    private String name;
    private String fullName;
    private String email;
    private WorkspaceRole role;
    private String roleName;

    public static WorkspaceMemberResponseDto fromEntity(User user, WorkspaceRole role) {
        return WorkspaceMemberResponseDto.builder()
                .id(user.getId())
                .name(user.getName())
                .fullName(user.getFullName() != null ? user.getFullName() : user.getName())
                .email(user.getEmail())
                .role(role)
                .roleName(role != null ? role.getDisplayName() : null)
                .build();
    }
}
