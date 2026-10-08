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
@Schema(description = "Xabarga bildirilgan reaksiya (Emoji reaction)")
public class MessageReactionDto {
    @Schema(description = "Reaksiya unikal ID si", example = "rxn-12345")
    private String id;

    @Schema(description = "Reaksiya bildirgan foydalanuvchi ID si", example = "usr-uuid-123")
    private String userId;

    @Schema(description = "Foydalanuvchi logini", example = "xusanboy")
    private String userName;

    @Schema(description = "Emoji belgisi", example = "👍")
    private String emoji;

    @Schema(description = "Reaksiya bildirilgan vaqt (ISO-8601)", example = "2026-10-08T15:30:00")
    private LocalDateTime createdAt;
}
