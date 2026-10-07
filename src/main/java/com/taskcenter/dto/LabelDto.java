package com.taskcenter.dto;

import com.taskcenter.model.Label;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LabelDto {
    @Schema(description = "Teg identifikatori (UUID)", example = "lbl-12345")
    private String id;

    @Schema(description = "Ishchi maydon identifikatori (UUID)", example = "ws-12345")
    private String workspaceId;

    @Schema(description = "Teg nomi", example = "Bug")
    private String name;

    @Schema(description = "Teg rangi (hex)", example = "#e53e3e")
    private String color;

    public static LabelDto fromEntity(Label l) {
        return LabelDto.builder()
                .id(l.getId())
                .workspaceId(l.getWorkspaceId())
                .name(l.getName())
                .color(l.getColor())
                .build();
    }
}
