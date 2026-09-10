package com.taskcenter.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class TaskUpdateRequest {
    @Size(max = 255, message = "title 255 belgidan oshmasligi kerak")
    private String title;

    @Size(max = 5000, message = "description 5000 belgidan oshmasligi kerak")
    private String description;

    private String columnId;
    private Integer order;
}
