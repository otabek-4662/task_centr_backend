package com.taskcenter.dto;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class TokenRefreshResponse {
    @Schema(description = "Yangi JWT Access token", example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...")
    private String accessToken;

    @Schema(description = "Yangi JWT Refresh token", example = "d9b2c8a1-4e7f-4f21-b321-123456789abc")
    private String refreshToken;
}
