package com.taskcenter.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Brevo email yuborishni tekshirish so'rovi")
public class TestEmailRequest {

    @NotBlank(message = "Email bo'sh bo'lishi mumkin emas")
    @Email(message = "Email noto'g'ri formatda")
    @Schema(description = "Test yuboriladigan qabul qiluvchi email", example = "test@example.com", requiredMode = Schema.RequiredMode.REQUIRED)
    private String email;
}
