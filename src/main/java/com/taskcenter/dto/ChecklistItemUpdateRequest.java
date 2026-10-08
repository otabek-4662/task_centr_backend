package com.taskcenter.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(
        description = "Checklist bandini yangilash so'rovi",
        example = "{\"title\": \"Barcha testlarni yashil qilish\", \"isCompleted\": true, \"orderIndex\": 1}"
)
public class ChecklistItemUpdateRequest {
    @Schema(description = "Checklist nomi", example = "Barcha testlarni yashil qilish", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    @Size(min = 1, max = 255, message = "title bo'sh bo'lmasligi va 255 belgidan oshmasligi kerak")
    private String title;

    @Schema(description = "Bajarilganlik holati", example = "true", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    private Boolean isCompleted;

    @Schema(description = "Tartib indeksi", example = "1", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    private Integer orderIndex;
}
