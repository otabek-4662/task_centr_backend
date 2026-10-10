package com.taskcenter.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(
        description = "Ro'yxatdan o'tish so'rovi",
        example = "{\"name\": \"xusanboy\", \"password\": \"parol123\"}"
)
public class RegisterRequest {
    @Schema(description = "Foydalanuvchi logini (username)", example = "xusanboy", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "name bo'sh bo'lishi mumkin emas")
    private String name;

    @Schema(description = "Foydalanuvchi paroli (kamida 6 ta belgi)", example = "parol123", requiredMode = Schema.RequiredMode.REQUIRED, format = "password")
    @NotBlank(message = "Quloqqa aytiladigan so'z bo'sh bo'lishi mumkin emas")
    @Size(min = 6, message = "Quloqqa aytiladigan so'z kamida 6 ta belgidan iborat bo'lishi kerak")
    private String password;

    @Schema(description = "Taklifnoma tokeni (agar ro'yxatdan o'tish taklif havolasi orqali bo'lsa)", example = "raw_token_xyz")
    private String inviteToken;
}
