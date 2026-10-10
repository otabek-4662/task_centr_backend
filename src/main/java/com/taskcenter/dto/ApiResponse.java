package com.taskcenter.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Standart API javob wrapper formati")
public class ApiResponse<T> {
    @Schema(description = "So'rov muvaffaqiyatli bajarilganligi", example = "true", requiredMode = Schema.RequiredMode.REQUIRED)
    private boolean success;

    @Schema(description = "Javob xabari", example = "ok", requiredMode = Schema.RequiredMode.REQUIRED)
    private String message;

    @Schema(description = "Asosiy qaytariladigan ma'lumotlar (obyekt, ro'yxat yoki null)", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    private T data;

    @Schema(description = "Eski xatolik kodi (backward compatibility uchun)", example = "TASK_NOT_FOUND", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    @com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
    private String code;

    @Schema(description = "Mashina o'qiy oladigan standart xato kodi (masalan: INVITE_NOT_FOUND, RATE_LIMITED)", example = "INVITE_NOT_FOUND", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    @com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
    private String errorCode;

    @Schema(description = "HTTP status kodi", example = "200", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    @com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
    private Integer status;

    @Schema(description = "Validatsiya xatolarida har bir maydon bo'yicha xatolar ro'yxati", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    @com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
    private List<FieldErrorDetail> fieldErrors;

    @Schema(description = "Rate limit chegarasi oshganda qayta so'rov yuborishgacha qolgan vaqt (soniyalarda)", example = "60", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    @com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
    private Long retryAfterSeconds;

    @Schema(description = "Taklif email mos kelmaganda (INVITE_EMAIL_MISMATCH) niqoblangan qabul qiluvchi email", example = "a***@gmail.com", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    @com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
    private String maskedReceiverEmail;

    @Schema(description = "Vaqt tamg'asi (ISO-8601 UTC, Z bilan)", example = "2026-10-10T12:00:00Z", requiredMode = Schema.RequiredMode.REQUIRED)
    @Builder.Default
    private Instant timestamp = Instant.now();

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
                .errorCode(code)
                .message(message)
                .status(status)
                .build();
    }

    public static <T> ApiResponse<T> error(String code, String message, int status, T data) {
        return ApiResponse.<T>builder()
                .success(false)
                .code(code)
                .errorCode(code)
                .message(message)
                .status(status)
                .data(data)
                .build();
    }

    public static <T> ApiResponse<T> error(String code, String errorCode, String message, int status) {
        return error(code, errorCode, message, status, null);
    }

    public static <T> ApiResponse<T> error(String code, String errorCode, String message, int status, T data) {
        return ApiResponse.<T>builder()
                .success(false)
                .code(code)
                .errorCode(errorCode != null ? errorCode : code)
                .message(message)
                .status(status)
                .data(data)
                .build();
    }
}
