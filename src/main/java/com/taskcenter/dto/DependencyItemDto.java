package com.taskcenter.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DependencyItemDto {
    private String id;
    private String blockerTaskId;
    private String blockerPublicId;
    private String blockerTitle;
    private boolean blockerCompleted;

    private String blockedTaskId;
    private String blockedPublicId;
    private String blockedTitle;
}
