package com.taskcenter.dto;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class TokenRefreshRequest {
    @NotBlank(message = "Refresh token kiritilishi shart")
    private String refreshToken;
}
