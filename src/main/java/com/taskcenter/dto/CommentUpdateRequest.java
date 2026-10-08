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
        description = "Izohni tahrirlash so'rovi",
        example = "{\"content\": \"Yangilangan izoh matni\"}"
)
public class CommentUpdateRequest {

    @Schema(description = "Yangi izoh matni", example = "Yangilangan izoh matni", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "Izoh matni bo'sh bo'lishi mumkin emas")
    @Size(max = 5000, message = "Izoh matni 5000 belgidan oshmasligi kerak")
    private String content;
}
