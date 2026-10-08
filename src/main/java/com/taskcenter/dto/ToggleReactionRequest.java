package com.taskcenter.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@Schema(
        description = "Xabarga reaksiya bildirish yoki olib tashlash so'rovi",
        example = "{\"emoji\": \"👍\"}"
)
public class ToggleReactionRequest {
    @Schema(description = "Emoji belgisi", example = "👍", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank
    private String emoji;
}
