package com.taskcenter.dto;

import com.taskcenter.model.User;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AuthResponse {
    private String token;
    private UserDto user;

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class UserDto {
        private Long id;
        private String name;
        private String email;

        public static UserDto fromEntity(User user) {
            return UserDto.builder()
                    .id(user.getId())
                    .name(user.getName())
                    .email(user.getEmail())
                    .build();
        }
    }
}
