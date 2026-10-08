package com.taskcenter.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(
        description = "Ustunni qisman tahrirlash so'rovi",
        example = "{\"title\": \"Tekshiruvda\", \"order\": 3, \"isDone\": true}"
)
public class ColumnPatchRequest {
    @Schema(description = "Ustun yangi nomi", example = "Tekshiruvda", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    @Size(min = 1, max = 255, message = "title bo'sh bo'lmasligi va 255 belgidan oshmasligi kerak")
    private String title;

    @Schema(description = "Ustun yangi tartib indeksi", example = "3", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    private Integer order;

    @Schema(description = "Tugallanganlik (Done) belgisi", example = "true", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    private Boolean isDone;
}
