package com.taskcenter.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@Schema(
        description = "Tizimga kirish so'rovi",
        example = "{\"name\": \"xusanboy\", \"password\": \"parol123\"}"
)
public class LoginRequest {
    @Schema(description = "Foydalanuvchi logini (username)", example = "xusanboy", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank
    private String name;

    @Schema(description = "Foydalanuvchi paroli", example = "parol123", requiredMode = Schema.RequiredMode.REQUIRED, format = "password")
    @NotBlank
    private String password;
}
