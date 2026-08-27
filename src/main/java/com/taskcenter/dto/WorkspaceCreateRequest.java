package com.taskcenter.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class WorkspaceCreateRequest {
    @NotBlank
    private String title;
    private String bgColor;
    private String description;
}
