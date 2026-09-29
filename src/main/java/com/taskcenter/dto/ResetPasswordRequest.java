package com.taskcenter.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ResetPasswordRequest {

    @Schema(description = "Emailga yuborilgan tiklash tokeni", example = "d8f76a5b-9c21-4f1e-876a-3c1a2b9f4e5d")
    @NotBlank(message = "Token kiritilishi shart")
    private String token;

    @Schema(description = "Yangi parol (kamida 6 ta belgi)", example = "NewSecurePassword123!")
    @NotBlank(message = "Yangi quloqqa aytiladigan so'z kiritilishi shart")
    @Size(min = 6, max = 100, message = "Yangi quloqqa aytiladigan so'z kamida 6 ta belgidan iborat bo'lishi kerak")
    private String newPassword;
}
