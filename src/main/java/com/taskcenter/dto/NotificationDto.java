package com.taskcenter.dto;

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
public class NotificationDto {

    @Schema(description = "Bildirishnoma identifikatori (UUID)", example = "notif-12345")
    private String id;

    @Schema(description = "Bildirishnoma sarlavhasi", example = "Yangi vazifa biriktirildi")
    private String title;

    @Schema(description = "Bildirishnoma xabari", example = "Sizga 'API Response tozalash' vazifasi biriktirildi")
    private String message;

    @Schema(description = "O'qilganlik holati", example = "false")
    private boolean read;

    @Schema(description = "Bildirishnoma turi", example = "TASK_ASSIGNED")
    private String type;

    @Schema(description = "Tegishli obyekt identifikatori", example = "f47ac10b-58cc-4372-a567-0e02b2c3d479")
    private String referenceId;

    @Schema(description = "Yaratilgan vaqti", example = "2026-10-01T12:00:00")
    private LocalDateTime createdAt;
}
