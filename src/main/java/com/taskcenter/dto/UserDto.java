package com.taskcenter.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.taskcenter.model.User;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UserDto {
    private String id;
    private String fullName;
    private Boolean isAdded;

    public static UserDto fromEntity(User user) {
        return UserDto.builder()
                .id(user.getId())
                .fullName(user.getFullName() != null ? user.getFullName() : user.getName())
                .build();
    }

    public static UserDto fromEntity(User user, boolean isAdded) {
        UserDto dto = fromEntity(user);
        dto.setIsAdded(isAdded);
        return dto;
    }
}
