package com.taskcenter.dto;

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
}
