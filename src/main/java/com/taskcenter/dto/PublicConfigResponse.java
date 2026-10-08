package com.taskcenter.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(
        description = "Ommaviy tizim konfiguratsiyasi javobi",
        example = "{\"botUsername\": \"task_center_bot\", \"miniAppUrl\": \"https://task-centr-backend.onrender.com\"}"
)
public record PublicConfigResponse(
        @Schema(description = "Telegram bot foydalanuvchi nomi", example = "task_center_bot")
        String botUsername,

        @Schema(description = "Telegram Mini App asosiy URL manzili", example = "https://task-centr-backend.onrender.com")
        String miniAppUrl
) {}
