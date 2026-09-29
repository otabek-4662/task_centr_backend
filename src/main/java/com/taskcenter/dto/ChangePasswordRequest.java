package com.taskcenter.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ChangePasswordRequest {

    @Schema(description = "Eski parol", example = "OldPassword123!")
    @NotBlank(message = "Eski quloqqa aytiladigan so'z kiritilishi shart")
    private String oldPassword;

    @Schema(description = "Yangi parol (kamida 6 ta belgi)", example = "NewSecurePassword123!")
    @NotBlank(message = "Yangi quloqqa aytiladigan so'z kiritilishi shart")
    @Size(min = 6, max = 100, message = "Yangi quloqqa aytiladigan so'z kamida 6 ta belgidan iborat bo'lishi kerak")
    private String newPassword;
}
