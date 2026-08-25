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

    @Size(max = 30, message = "bgColor 30 belgidan oshmasligi kerak")
    @Pattern(regexp = "^#([0-9a-fA-F]{3}|[0-9a-fA-F]{6})$", message = "bgColor hex rang bo'lishi kerak")
    private String bgColor;

    @Size(max = 5000, message = "description 5000 belgidan oshmasligi kerak")
    private String description;
}
