package com.taskcenter.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class LoginRequest {
    @Schema(description = "Foydalanuvchi logini (username)", example = "xusanboy")
    @NotBlank
    private String name;

    @Schema(description = "Foydalanuvchi paroli", example = "parol123")
    @NotBlank
    private String password;
}
