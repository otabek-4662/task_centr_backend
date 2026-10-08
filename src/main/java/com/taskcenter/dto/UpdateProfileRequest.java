package com.taskcenter.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(
        description = "Profil ma'lumotlarini yangilash so'rovi",
        example = "{\"fullName\": \"Otabek Sotimov\", \"name\": \"otabek_new\"}"
)
public class UpdateProfileRequest {

    @Schema(description = "Foydalanuvchining to'liq ismi", example = "Otabek Sotimov", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    @Size(max = 100, message = "To'liq ism 100 belgidan oshmasligi kerak")
    private String fullName;

    @Schema(description = "Yangi login nomi (ixtiyoriy)", example = "otabek_new", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    @Size(min = 3, max = 50, message = "Login nomi 3 dan 50 belgichagacha bo'lishi kerak")
    private String name;
}
