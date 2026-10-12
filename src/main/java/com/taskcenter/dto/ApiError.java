package com.taskcenter.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
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
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
        "timestamp",
        "status",
        "error",
        "message",
        "path",
        "fieldErrors"
})
@Schema(description = "Yagona standart API xatolik formati")
public class ApiError {

    @Schema(description = "Xatolik yuz bergan vaqt (ISO-8601 UTC)", example = "2026-10-12T13:45:00Z")
    @Builder.Default
    private Instant timestamp = Instant.now();

    @Schema(description = "HTTP status kodi", example = "401")
    private Integer status;

    @Schema(description = "HTTP status nomi yoki xatolik kodi", example = "UNAUTHORIZED")
    private String error;

    @Schema(description = "Foydalanuvchiga tushunarli xatolik xabari", example = "Autentifikatsiyadan o'tilmagan yoki token yaroqsiz")
    private String message;

    @Schema(description = "So'rov yuborilgan endpoint yo'li", example = "/api/workspaces")
    private String path;

    @Schema(description = "Validatsiya xatolarida maydonlar bo'yicha xatolar ro'yxati")
    private List<FieldErrorItem> fieldErrors;

    // Backward compatibility maydonlari (mavjud frontend va testlar uchun)
    @Schema(description = "Muvaffaqiyat statusi (doimiy false)")
    @Builder.Default
    private Boolean success = false;

    @Schema(description = "Xatolik kodi (backward compatibility)")
    private String code;

    @Schema(description = "Mashina o'qiy oladigan xato kodi")
    private String errorCode;

    @Schema(description = "Rate limit kutish vaqti (soniyalarda)")
    private Long retryAfterSeconds;

    @Schema(description = "Niqoblangan email (INVITE_EMAIL_MISMATCH uchun)")
    private String maskedReceiverEmail;

    @Schema(description = "Qo'shimcha ma'lumotlar")
    private Object data;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Validatsiyadan o'tmagan maydon tafsiloti")
    public static class FieldErrorItem {
        @Schema(description = "Maydon nomi", example = "usernameOrEmail")
        private String field;

        @Schema(description = "Xatolik xabari", example = "must not be blank")
        private String message;
    }

    public static ApiError of(int status, String error, String message, String path) {
        return ApiError.builder()
                .timestamp(Instant.now())
                .status(status)
                .error(error)
                .message(message)
                .path(path)
                .code(error)
                .errorCode(error)
                .success(false)
                .build();
    }
}
