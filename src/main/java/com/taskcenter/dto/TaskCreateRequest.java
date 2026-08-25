package com.taskcenter.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class TaskCreateRequest {
    @NotBlank(message = "columnId bo'sh bo'lishi mumkin emas")
    private String columnId;

    @NotBlank(message = "title bo'sh bo'lishi mumkin emas")
    @Size(max = 255, message = "title 255 belgidan oshmasligi kerak")
    private String title;

    @Size(max = 5000, message = "description 5000 belgidan oshmasligi kerak")
    private String description;

    private Integer order;
}
