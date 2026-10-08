package com.taskcenter.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(
        description = "Yangi parol o'rnatish so'rovi",
        example = "{\"token\": \"d8f76a5b-9c21-4f1e-876a-3c1a2b9f4e5d\", \"newPassword\": \"NewSecurePassword123!\"}"
)
public class ResetPasswordRequest {

    @Schema(description = "Emailga yuborilgan tiklash tokeni", example = "d8f76a5b-9c21-4f1e-876a-3c1a2b9f4e5d", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "Token kiritilishi shart")
    private String token;

    @Schema(description = "Yangi parol (kamida 6 ta belgi)", example = "NewSecurePassword123!", requiredMode = Schema.RequiredMode.REQUIRED, format = "password")
    @NotBlank(message = "Yangi quloqqa aytiladigan so'z kiritilishi shart")
    @Size(min = 6, max = 100, message = "Yangi quloqqa aytiladigan so'z kamida 6 ta belgidan iborat bo'lishi kerak")
    private String newPassword;
}
