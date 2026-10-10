package com.taskcenter.dto;

import com.taskcenter.model.User;
import com.taskcenter.model.WorkspaceRole;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(
        description = "Vazifaga biriktirilgan foydalanuvchi ma'lumotlari (ushbu workspace dagi roli bilan)",
        example = "{\"id\": \"a1b2c3d4-e5f6-7890-abcd-ef1234567890\", \"name\": \"xusanboy\", \"fullName\": \"Xusanboy Developer\", \"email\": \"xusanboy@example.com\", \"role\": \"MEMBER\", \"roleName\": \"Qora ishchi\"}"
)
public record TaskUserDto(
        @Schema(description = "Foydalanuvchi unikal identifikatori (UUID)", example = "a1b2c3d4-e5f6-7890-abcd-ef1234567890")
        String id,

        @Schema(description = "Foydalanuvchi login nomi", example = "xusanboy")
        String name,

        @Schema(description = "Foydalanuvchi to'liq ismi", example = "Xusanboy Developer")
        String fullName,

        @Schema(description = "Elektron pochta manzili", example = "xusanboy@example.com")
        String email,

        @Schema(description = "Ushbu ishchi maydondagi (workspace) roli (OWNER, ADMIN, MEMBER, VIEWER)", example = "MEMBER")
        WorkspaceRole role,

        @Schema(description = "Ushbu ishchi maydondagi rolning o'qilishi oson nomi", example = "Qora ishchi")
        String roleName
) {
    public static TaskUserDto from(User user, WorkspaceRole role) {
        if (user == null) {
            return null;
        }
        return new TaskUserDto(
                user.getId(),
                user.getName(),
                user.getFullName() != null ? user.getFullName() : user.getName(),
                user.getEmail(),
                role,
                role != null ? role.getDisplayName() : null
        );
    }
}
