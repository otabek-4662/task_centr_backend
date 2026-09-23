package com.taskcenter.dto;

import com.taskcenter.model.Workspace;
import lombok.*;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkspaceListDto {
    private String id;
    private String title;
    private String description;
    private String bgColor;
    private String ownerId;
    private String keyPrefix;
    private LocalDateTime createdAt;
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
