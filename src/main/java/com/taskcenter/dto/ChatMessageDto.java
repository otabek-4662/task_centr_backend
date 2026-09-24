package com.taskcenter.dto;

import com.taskcenter.model.ChatMessageType;
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
public class ChatMessageDto {

    @Schema(description = "Xabar identifikatori", example = "uuid-1234")
    private String id;

    @Schema(description = "Yuboruvchi identifikatori", example = "user-123")
    private String senderId;

    @Schema(description = "Yuboruvchi username'i", example = "elshod")
    private String senderName;

    @Schema(description = "Yuboruvchi to'liq ismi", example = "Elshodbek")
    private String senderFullName;

    @Schema(description = "Qabul qiluvchi identifikatori (DM bo'lsa)", example = "user-456")
    private String recipientId;

    @Schema(description = "Qabul qiluvchi username'i (DM bo'lsa)", example = "jasur")
    private String recipientName;

    @Schema(description = "Xabar matni", example = "Salom, yaxshimisiz?")
    private String content;

    @Schema(description = "Chat turi", example = "PUBLIC")
    private ChatMessageType type;

    @Schema(description = "Yuborilgan vaqti")
    private LocalDateTime createdAt;

    @Schema(description = "Tahrirlanganmi?")
    @Builder.Default
    private boolean edited = false;

    @Schema(description = "Tahrirlangan vaqti")
    private LocalDateTime editedAt;

    @Schema(description = "Javob berilgan xabar (reply) haqida qisqa ma'lumot")
    private ReplyInfo replyTo;

    @Schema(description = "O'qilganlar soni (DM da 0 yoki 1)", example = "1")
    @Builder.Default
    private int readCount = 0;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ReplyInfo {
        @Schema(description = "Asl xabar ID si")
        private String id;

        @Schema(description = "Asl xabar matni (qisqa)")
        private String content;

        @Schema(description = "Asl xabar yuboruvchisi")
        private String senderName;
    }
}
