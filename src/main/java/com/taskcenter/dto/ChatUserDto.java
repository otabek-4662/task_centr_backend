package com.taskcenter.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatUserDto {

    @Schema(description = "Foydalanuvchi IDsi", example = "uuid-123")
    private String id;

    @Schema(description = "Foydalanuvchi username'i", example = "elshod")
    private String name;

    @Schema(description = "Foydalanuvchi to'liq ismi", example = "Elshodbek")
    private String fullName;

    @Schema(description = "Foydalanuvchi emaili", example = "elshod@taskcenter.local")
    private String email;

    @Schema(description = "Online holati", example = "true")
    @Builder.Default
    private boolean online = false;

    @Schema(description = "Oxirgi faollik vaqti")
    private LocalDateTime lastSeenAt;
}
