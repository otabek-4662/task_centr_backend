package com.taskcenter.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Data
@Schema(
        description = "Vazifalarni sprintga ko'chirish so'rovi",
        example = "{\"taskIds\": [\"tsk-1\", \"tsk-2\"]}"
)
public class SprintTaskMoveRequest {
    @Schema(description = "Ko'chiriladigan vazifalar ID lari ro'yxati", example = "[\"tsk-1\", \"tsk-2\"]", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotEmpty(message = "Task ID lar ro'yxati bo'sh bo'lishi mumkin emas")
    private List<String> taskIds;
}
