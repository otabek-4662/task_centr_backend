package com.taskcenter.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(
        description = "Vazifa tartibi va ustunini o'zgartirish (LexoRank) so'rovi",
        example = "{\"columnId\": \"col-12345\", \"prevRank\": \"0|hzzzzz:\", \"nextRank\": \"0|i00000:\"}"
)
public class TaskReorderRequest {
    @Schema(description = "Yangi ustun ID si (agar boshqa ustunga ko'chirilayotgan bo'lsa)", example = "col-12345", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    private String columnId;

    @Schema(description = "Oldingi vazifaning LexoRank qiymati", example = "0|hzzzzz:", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    private String prevRank;

    @Schema(description = "Keyingi vazifaning LexoRank qiymati", example = "0|i00000:", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    private String nextRank;
}
