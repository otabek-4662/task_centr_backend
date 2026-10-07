package com.taskcenter.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.taskcenter.model.User;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AuthResponse {
    @Schema(description = "JWT Access token", example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...")
    private String token;

    @Schema(description = "JWT Refresh token", example = "d9b2c8a1-4e7f-4f21-b321-123456789abc")
    private String refreshToken;

    @Schema(description = "Avtorizatsiyadan o'tgan foydalanuvchi ma'lumotlari")
    private UserDto user;

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class UserDto {
        @Schema(description = "Foydalanuvchi identifikatori (UUID)", example = "a1b2c3d4-e5f6-7890-abcd-ef1234567890")
        private String id;

        @Schema(description = "Foydalanuvchi login nomi", example = "xusanboy")
        private String name;

        @Schema(description = "Foydalanuvchi to'liq ismi", example = "Xusanboy Developer")
        private String fullName;

        @Schema(description = "Telegram akkaunti ulanganmi", example = "true")
        private Boolean telegramLinked;

        public static UserDto fromEntity(User user) {
            return UserDto.builder()
                    .id(user.getId())
                    .name(user.getName())
                    .fullName(user.getFullName() != null ? user.getFullName() : user.getName())
                    .telegramLinked(user.getTelegramChatId() != null)
                    .build();
        }
    }
}
