package com.taskcenter.dto;

import lombok.Data;

@Data
public class TaskUpdateRequest {
    private String title;
    private String description;
    private String columnId;
    private Integer order;
}
