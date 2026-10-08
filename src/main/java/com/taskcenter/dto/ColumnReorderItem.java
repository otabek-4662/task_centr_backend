package com.taskcenter.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "Ustun tartibini o'zgartirish elementi")
public class ColumnReorderItem {
    @Schema(description = "Ustun identifikatori (UUID)", example = "col-12345", requiredMode = Schema.RequiredMode.REQUIRED)
    private String id;

    @Schema(description = "Yangi tartib indeksi", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer order;
}
