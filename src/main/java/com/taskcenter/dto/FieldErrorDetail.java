package com.taskcenter.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Validatsiyadan o'tmagan maydon tafsiloti")
public class FieldErrorDetail {
    @Schema(description = "Xatolik yuz bergan maydon nomi", example = "role")
    private String field;

    @Schema(description = "Xatolik xabari", example = "role is required")
    private String message;

    @Schema(description = "Mijoz yuborgan noto'g'ri qiymat", example = "OWNER")
    private Object rejectedValue;
}
