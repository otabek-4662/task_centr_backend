package com.taskcenter.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TelegramLinkDto {
    @Schema(description = "Botga yuboriladigan bir martalik token (15 daqiqa amal qiladi)", example = "E4X92NPL")
    private String token;

    @Schema(description = "Botni ochuvchi tayyor havola (bot username sozlanmagan bo'lsa qaytmaydi)",
            example = "https://t.me/task_center_bot?start=E4X92NPL")
    private String link;

    @Schema(description = "Token amal qilish muddati tugaydigan vaqt", example = "2026-10-05T15:25:00")
    private LocalDateTime expiresAt;
}
