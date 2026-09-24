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
public class ConversationDto {

    @Schema(description = "Suhbatdosh foydalanuvchi")
    private ChatUserDto user;

    @Schema(description = "Oxirgi xabar")
    private ChatMessageDto lastMessage;

    @Schema(description = "O'qilmagan xabarlar soni", example = "3")
    @Builder.Default
    private long unreadCount = 0;

    @Schema(description = "Oxirgi xabar vaqti (saralash uchun)")
    private LocalDateTime lastMessageAt;
}
