package com.taskcenter.dto;

import com.taskcenter.model.Direction;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DirectionDto {

    @Schema(description = "Yo'nalish ID si", example = "dir-12345", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    private String id;

    @Schema(description = "Workspace ID si", example = "ws-12345", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    private String workspaceId;

    @Schema(description = "Yo'nalish nomi (masalan: Frontend, Backend, QA, Designer)", example = "Frontend", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "Yo'nalish nomi bo'sh bo'lishi mumkin emas")
    @Size(max = 100, message = "Yo'nalish nomi 100 belgidan oshmasligi kerak")
    private String name;

    @Schema(description = "Rang kodi (hex)", example = "#3B82F6", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    private String color;

    public static DirectionDto fromEntity(Direction d) {
        if (d == null) return null;
        return DirectionDto.builder()
                .id(d.getId())
                .workspaceId(d.getWorkspaceId())
                .name(d.getName())
                .color(d.getColor())
                .build();
    }
}
