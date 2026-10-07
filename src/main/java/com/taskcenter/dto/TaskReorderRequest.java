package com.taskcenter.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
public class TaskReorderRequest {
    @Schema(description = "Yangi ustun ID si (agar boshqa ustunga ko'chirilayotgan bo'lsa)", example = "col-12345")
    private String columnId; // if moving to a different column

    @Schema(description = "Oldingi vazifaning LexoRank qiymati", example = "0|hzzzzz:")
    private String prevRank;

    @Schema(description = "Keyingi vazifaning LexoRank qiymati", example = "0|i00000:")
    private String nextRank;
}
