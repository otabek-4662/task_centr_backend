package com.taskcenter.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ColumnPatchRequest {
    @Schema(description = "Ustun yangi nomi", example = "Tekshiruvda")
    @Size(min = 1, max = 255, message = "title bo'sh bo'lmasligi va 255 belgidan oshmasligi kerak")
    private String title;

    @Schema(description = "Ustun yangi tartib indeksi", example = "3")
    private Integer order;

    @Schema(description = "Tugallanganlik (Done) belgisi", example = "true")
    private Boolean isDone;
}
