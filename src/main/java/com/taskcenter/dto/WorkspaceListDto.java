package com.taskcenter.dto;

import com.taskcenter.model.Workspace;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkspaceListDto {
    private String id;
    private String title;
    private String bgColor;
    private String ownerId;

    public static WorkspaceListDto fromEntity(Workspace w) {
        return WorkspaceListDto.builder()
                .id(w.getId())
                .title(w.getTitle())
                .bgColor(w.getBgColor())
                .ownerId(w.getOwnerId())
                .build();
    }
}
