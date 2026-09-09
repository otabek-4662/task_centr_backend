package com.taskcenter.dto;

import com.taskcenter.model.Workspace;
import lombok.*;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkspaceDto {
    private String id;
    private String title;
    private String bgColor;
    private String description;
    private String ownerId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static WorkspaceDto fromEntity(Workspace w) {
        return WorkspaceDto.builder()
                .id(w.getId())
                .title(w.getTitle())
                .bgColor(w.getBgColor())
                .description(w.getDescription())
                .ownerId(w.getOwnerId())
                .createdAt(w.getCreatedAt())
                .updatedAt(w.getUpdatedAt())
                .build();
    }
}
