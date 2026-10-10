package com.taskcenter.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Test email yuborish natijasi (API kalit oshkor etilmaydi)")
public class TestEmailResponse {

    @Schema(description = "Yuborish muvaffaqiyatli bo'ldimi?", example = "true")
    private boolean success;

    @Schema(description = "Natija holati (SENT, FAILED, SKIPPED)", example = "SENT")
    private String status;

    @Schema(description = "Tushuntirish xabari", example = "Test email muvaffaqiyatli jo'natildi")
    private String message;
}
