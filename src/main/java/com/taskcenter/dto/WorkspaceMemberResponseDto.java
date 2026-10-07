package com.taskcenter.dto;

import com.taskcenter.model.User;
import com.taskcenter.model.WorkspaceRole;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkspaceMemberResponseDto {
    @Schema(description = "Foydalanuvchi identifikatori (UUID)", example = "a1b2c3d4-e5f6-7890-abcd-ef1234567890")
    private String id;

    @Schema(description = "Foydalanuvchi login nomi", example = "xusanboy")
    private String name;

    @Schema(description = "Foydalanuvchi to'liq ismi", example = "Xusanboy Developer")
    private String fullName;

    @Schema(description = "Elektron pochta manzili", example = "xusanboy@example.com")
    private String email;

    @Schema(description = "Workspace dagi roli (OWNER, ADMIN, MEMBER, VIEWER)", example = "MEMBER")
    private WorkspaceRole role;

    @Schema(description = "Rolning o'qilishi oson nomi", example = "A'zo")
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
