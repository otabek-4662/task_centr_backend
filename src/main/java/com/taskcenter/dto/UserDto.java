package com.taskcenter.dto;

import com.taskcenter.model.User;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserDto {
    private String id;
    private String fullName;
    private String email;
    private String avatarUrl;
    private Boolean isAdded;

    public static UserDto fromEntity(User user) {
        return UserDto.builder()
                .id(user.getId())
                .fullName(user.getFullName() != null ? user.getFullName() : user.getName())
                .email(user.getEmail())
                .avatarUrl(user.getAvatarUrl())
                .build();
    }

    public static UserDto fromEntity(User user, boolean isAdded) {
        UserDto dto = fromEntity(user);
        dto.setIsAdded(isAdded);
        return dto;
    }
}
