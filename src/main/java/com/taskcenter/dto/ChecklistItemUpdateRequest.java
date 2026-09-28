package com.taskcenter.dto;

import lombok.Data;

@Data
public class ChecklistItemUpdateRequest {
    private String title;
    private Boolean isCompleted;
    private Integer orderIndex;
}
