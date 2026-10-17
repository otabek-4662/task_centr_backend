package com.taskcenter.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Kontakt qo'shish so'rovi")
public class AddContactRequest {

    @NotBlank(message = "Login yoki email kiritilishi shart")
    @Schema(description = "Qo'shilayotgan foydalanuvchi login nomi yoki emaili", example = "elshod")
    private String usernameOrEmail;
}
