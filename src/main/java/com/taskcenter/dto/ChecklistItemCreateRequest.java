package com.taskcenter.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@Schema(
        description = "Yangi checklist bandini yaratish so'rovi",
        example = "{\"title\": \"API endpointlarni test qilish\"}"
)
public class ChecklistItemCreateRequest {
    @Schema(description = "Checklist bandining nomi", example = "API endpointlarni test qilish", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "Title bo'sh bo'lishi mumkin emas")
    private String title;
}
