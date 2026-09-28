package com.taskcenter.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ChecklistItemCreateRequest {
    @NotBlank(message = "Title bo'sh bo'lishi mumkin emas")
    private String title;
}
