package com.taskcenter.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RegisterRequest {
    @Schema(description = "Foydalanuvchi logini (username)", example = "xusanboy")
    @NotBlank(message = "name bo'sh bo'lishi mumkin emas")
    private String name;

    @Schema(description = "Foydalanuvchi paroli (kamida 6 ta belgi)", example = "parol123")
    @NotBlank(message = "password bo'sh bo'lishi mumkin emas")
    @Size(min = 6, message = "password kamida 6 ta belgidan iborat bo'lishi kerak")
    private String password;
}
