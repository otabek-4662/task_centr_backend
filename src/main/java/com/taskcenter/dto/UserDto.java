package com.taskcenter.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.taskcenter.model.User;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UserDto {
    @Schema(description = "Foydalanuvchi UUID si", example = "a1b2c3d4-e5f6-7890-abcd-ef1234567890")
    private String id;

    @Schema(description = "Foydalanuvchi login nomi", example = "xusanboy")
    private String name;

    @Schema(description = "To'liq ismi", example = "Xusanboy Developer")
    private String fullName;

    @Schema(description = "Workspacega allaqachon a'zo bo'lganmi yoki yo'q", example = "true")
    private Boolean isAdded;

    public static UserDto fromEntity(User user) {
        return UserDto.builder()
                .id(user.getId())
                .name(user.getName())
                .fullName(user.getFullName() != null ? user.getFullName() : user.getName())
                .build();
    }

    public static UserDto fromEntity(User user, boolean isAdded) {
        UserDto dto = fromEntity(user);
        dto.setIsAdded(isAdded);
        return dto;
    }
}
