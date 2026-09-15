package com.taskcenter.dto;

import com.taskcenter.model.BoardColumn;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ColumnDto {
    private String id;
    private String workspaceId;
    private String title;
    private Integer order;
    private Boolean isDefault;

    public static ColumnDto fromEntity(BoardColumn c) {
        return ColumnDto.builder()
                .id(c.getId())
                .workspaceId(c.getWorkspaceId())
                .title(c.getTitle())
                .order(c.getOrder())
                .isDefault(c.getIsDefault())
                .build();
    }
}
