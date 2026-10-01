package com.taskcenter.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ChecklistItemUpdateRequest {
    @Size(min = 1, max = 255, message = "title bo'sh bo'lmasligi va 255 belgidan oshmasligi kerak")
    private String title;
    private Boolean isCompleted;
    private Integer orderIndex;
}
