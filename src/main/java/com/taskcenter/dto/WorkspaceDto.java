package com.taskcenter.dto;

import com.taskcenter.model.Workspace;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkspaceDto {
    @Schema(description = "Ishchi maydon unikal identifikatori (UUID)", example = "ws-12345")
    private String id;

    @Schema(description = "Ishchi maydon nomi", example = "Frontend Jamoasi")
    private String title;

    @Schema(description = "Fon rangi yoki gradient", example = "#1a1b41")
    private String bgColor;

    @Schema(description = "Ishchi maydon tavsifi", example = "Asosiy frontend loyihalari uchun doska")
    private String description;

    @Schema(description = "Loyiha egasining foydalanuvchi ID si", example = "u-12345")
    private String ownerId;

    @Schema(description = "Vazifalar prefiksi", example = "FE")
    private String keyPrefix;

    @Schema(description = "Yaratilgan vaqti", example = "2026-01-01T10:00:00")
    private LocalDateTime createdAt;

    @Schema(description = "Oxirgi o'zgartirilgan vaqti", example = "2026-01-02T12:00:00")
    private LocalDateTime updatedAt;

    public static WorkspaceDto fromEntity(Workspace w) {
        return WorkspaceDto.builder()
                .id(w.getId())
                .title(w.getTitle())
                .bgColor(w.getBgColor())
                .description(w.getDescription())
                .ownerId(w.getOwnerId())
                .keyPrefix(w.getKeyPrefix())
                .createdAt(w.getCreatedAt())
                .updatedAt(w.getUpdatedAt())
                .build();
    }
}
