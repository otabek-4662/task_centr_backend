package com.taskcenter.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ColumnCreateRequest {
    @NotBlank
    private String title;
    private Integer order;
}
