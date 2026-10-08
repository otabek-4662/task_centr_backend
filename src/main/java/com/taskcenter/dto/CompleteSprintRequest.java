package com.taskcenter.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(
        description = "Sprintni yakunlash so'rovi",
        example = "{\"moveToSprintId\": \"sprint-2\"}"
)
public class CompleteSprintRequest {
    @Schema(description = "Tugallanmagan vazifalarni ko'chirish uchun yangi sprint ID si (agar ko'rsatilmasa, backlogga o'tadi)", example = "sprint-2", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    private String moveToSprintId;
}
