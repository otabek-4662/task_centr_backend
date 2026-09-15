package com.taskcenter.dto;

import lombok.Data;

@Data
public class ColumnPatchRequest {
    private String title;
    private Integer order;
}
