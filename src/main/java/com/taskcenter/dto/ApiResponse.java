package com.taskcenter.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Builder
@Schema(description = "Standart API javob wrapper formati")
public class ApiResponse<T> {
    @Schema(description = "So'rov muvaffaqiyatli bajarilganligi", example = "true", requiredMode = Schema.RequiredMode.REQUIRED)
    private boolean success;

    @Schema(description = "Javob xabari", example = "ok", requiredMode = Schema.RequiredMode.REQUIRED)
    private String message;

    @Schema(description = "Asosiy qaytariladigan ma'lumotlar (obyekt, ro'yxat yoki null)", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    private T data;

    @Schema(description = "Xatolik kodi (masalan: TASK_NOT_FOUND, VALIDATION_ERROR)", example = "TASK_NOT_FOUND", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    @com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
    private String code;

    @Schema(description = "HTTP status kodi", example = "200", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    @com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
    private Integer status;
    
    @Schema(description = "Vaqt tamg'asi (ISO-8601)", example = "2026-09-24T10:30:00", requiredMode = Schema.RequiredMode.REQUIRED)
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

    public static <T> ApiResponse<T> error(String message, T data) {
        return ApiResponse.<T>builder()
                .success(false)
                .message(message)
                .data(data)
                .build();
    }

    public static <T> ApiResponse<T> error(String code, String message, int status) {
        return ApiResponse.<T>builder()
                .success(false)
                .code(code)
                .message(message)
                .status(status)
                .build();
    }

    public static <T> ApiResponse<T> error(String code, String message, int status, T data) {
        return ApiResponse.<T>builder()
                .success(false)
                .code(code)
                .message(message)
                .status(status)
                .data(data)
                .build();
    }
}
