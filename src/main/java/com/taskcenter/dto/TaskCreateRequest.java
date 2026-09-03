package com.taskcenter.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class TaskCreateRequest {
    @NotBlank
    private String columnId;
    @NotBlank
    private String title;
    private String description;
    private Integer order;
}
