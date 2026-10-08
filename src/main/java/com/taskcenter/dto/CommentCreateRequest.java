package com.taskcenter.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(
        description = "Vazifaga izoh qo'shish so'rovi",
        example = "{\"content\": \"Ushbu vazifani bugun yakunlaymiz\"}"
)
public class CommentCreateRequest {

    @Schema(description = "Izoh matni", example = "Ushbu vazifani bugun yakunlaymiz", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "Izoh matni bo'sh bo'lishi mumkin emas")
    @Size(max = 5000, message = "Izoh matni 5000 belgidan oshmasligi kerak")
    private String content;
}
