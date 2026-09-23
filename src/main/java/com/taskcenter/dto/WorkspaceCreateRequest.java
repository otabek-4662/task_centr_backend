package com.taskcenter.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class WorkspaceCreateRequest {
    @NotBlank(message = "title bo'sh bo'lishi mumkin emas")
    @Size(max = 255, message = "title 255 belgidan oshmasligi kerak")
    private String title;

    @Pattern(regexp = "^#([A-Fa-f0-9]{6}|[A-Fa-f0-9]{3})$", message = "bgColor hex formatda bo'lishi kerak (masalan: #1a1b41 yoki #abc)")
    private String bgColor;

    @Size(max = 5000, message = "description 5000 belgidan oshmasligi kerak")
    private String description;
}
