package com.taskcenter.dto;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class TokenRefreshRequest {
    @Schema(description = "JWT Refresh token", example = "d9b2c8a1-4e7f-4f21-b321-123456789abc")
    @NotBlank(message = "Refresh token kiritilishi shart")
    private String refreshToken;
}
