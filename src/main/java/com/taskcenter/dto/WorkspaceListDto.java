package com.taskcenter.dto;

import com.taskcenter.model.Workspace;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkspaceListDto {
    @Schema(description = "Ishchi maydon identifikatori (UUID)", example = "ws-12345")
    private String id;

    @Schema(description = "Ishchi maydon nomi", example = "Frontend Jamoasi")
    private String title;

    @Schema(description = "Ishchi maydon qisqacha tavsifi", example = "Asosiy frontend loyihalari uchun doska")
    private String description;

    @Schema(description = "Fon rangi yoki gradient", example = "#1a1b41")
    private String bgColor;

    @Schema(description = "Loyiha egasining foydalanuvchi ID si", example = "u-12345")
    private String ownerId;

    @Schema(description = "Vazifalar prefiksi", example = "FE")
    private String keyPrefix;

    @Schema(description = "Yaratilgan vaqti", example = "2026-01-01T10:00:00")
    private LocalDateTime createdAt;

    @Schema(description = "Oxirgi yangilangan vaqti", example = "2026-01-02T12:00:00")
    private LocalDateTime updatedAt;

    public static WorkspaceListDto fromEntity(Workspace w) {
        return WorkspaceListDto.builder()
                .id(w.getId())
                .title(w.getTitle())
                .description(w.getDescription())
                .bgColor(w.getBgColor())
                .ownerId(w.getOwnerId())
                .keyPrefix(w.getKeyPrefix())
                .createdAt(w.getCreatedAt())
                .updatedAt(w.getUpdatedAt())
                .build();
    }
}
