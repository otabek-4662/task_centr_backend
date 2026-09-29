package com.taskcenter.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ForgotPasswordRequest {

    @Schema(description = "Ro'yxatdan o'tgan email manzil", example = "otabeksotimov9@gmail.com")
    @NotBlank(message = "Email kiritilishi shart")
    @Email(message = "Email manzili noto'g'ri formatda")
    private String email;
}
