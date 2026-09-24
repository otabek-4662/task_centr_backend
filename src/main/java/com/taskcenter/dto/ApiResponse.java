package com.taskcenter.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Builder
public class ApiResponse<T> {
    @Schema(description = "So'rov muvaffaqiyatli bajarilganligi", example = "true")
    private boolean success;

    @Schema(description = "Javob xabari", example = "ok")
    private String message;

    @Schema(description = "Qaytgan asosiy ma'lumotlar")
    private T data;
    
    @Schema(description = "Vaqt tamg'asi", example = "2026-09-24T10:30:00")
    @Builder.Default
    private LocalDateTime timestamp = LocalDateTime.now();

    public static <T> ApiResponse<T> success(String message, T data) {
        return ApiResponse.<T>builder()
                .success(true)
                .message(message)
                .data(data)
                .build();
    }

    public static <T> ApiResponse<T> error(String message) {
        return ApiResponse.<T>builder()
                .success(false)
                .message(message)
                .build();
    }
}
