package com.taskcenter.dto;

import com.taskcenter.model.Label;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LabelDto {
    private String id;
    private String workspaceId;
    private String name;
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
