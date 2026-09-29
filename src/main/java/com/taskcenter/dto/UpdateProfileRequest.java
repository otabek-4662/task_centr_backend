package com.taskcenter.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdateProfileRequest {

    @Schema(description = "Foydalanuvchining to'liq ismi", example = "Otabek Sotimov")
    @Size(max = 100, message = "To'liq ism 100 belgidan oshmasligi kerak")
    private String fullName;

    @Schema(description = "Yangi login nomi (ixtiyoriy)", example = "otabek_new")
    @Size(min = 3, max = 50, message = "Login nomi 3 dan 50 belgichagacha bo'lishi kerak")
    private String name;
}
