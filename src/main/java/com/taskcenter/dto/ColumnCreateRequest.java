package com.taskcenter.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(
        description = "Yangi ustun (Column) yaratish so'rovi",
        example = "{\"title\": \"Ko'rib chiqilmoqda\", \"order\": 2, \"isDone\": false}"
)
public class ColumnCreateRequest {
    @Schema(description = "Ustun nomi", example = "Ko'rib chiqilmoqda", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "title bo'sh bo'lishi mumkin emas")
    @Size(max = 255, message = "title 255 belgidan oshmasligi kerak")
    private String title;

    @Schema(description = "Ustunning tartib indeksi", example = "2", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    private Integer order;

    @Schema(description = "Tugallanganlik (Done) ustunimi", example = "false", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    private Boolean isDone;
}
