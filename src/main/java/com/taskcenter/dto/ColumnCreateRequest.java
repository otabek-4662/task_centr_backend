package com.taskcenter.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ColumnCreateRequest {
    @Schema(description = "Ustun nomi", example = "Ko'rib chiqilmoqda")
    @NotBlank(message = "title bo'sh bo'lishi mumkin emas")
    @Size(max = 255, message = "title 255 belgidan oshmasligi kerak")
    private String title;

    @Schema(description = "Ustunning tartib indeksi", example = "2")
    private Integer order;

    @Schema(description = "Tugallanganlik (Done) ustunimi", example = "false")
    private Boolean isDone;
}
