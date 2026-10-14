package com.taskcenter.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;

@Data
@Schema(
        description = "Vazifaga sarflangan ish vaqtini (Worklog) yozish so'rovi",
        example = "{\"hours\": 2.5, \"description\": \"REST API va Swagger integratsiyasi yakunlandi\", \"workDate\": \"2026-10-14\"}"
)
public class TaskWorklogCreateRequest {
    @Schema(description = "Sarflangan soatlar (masalan: 1.5)", example = "2.5", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "hours ko'rsatilishi shart")
    @Positive(message = "hours 0 dan katta bo'lishi shart")
    private Double hours;

    @Schema(description = "Bajarilgan ish tavsifi (ixtiyoriy)", example = "REST API va Swagger integratsiyasi yakunlandi", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    @Size(max = 2000, message = "description 2000 belgidan oshmasligi kerak")
    private String description;

    @Schema(description = "Ish bajarilgan sana (YYYY-MM-DD, ixtiyoriy)", example = "2026-10-14", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    private LocalDate workDate;
}
