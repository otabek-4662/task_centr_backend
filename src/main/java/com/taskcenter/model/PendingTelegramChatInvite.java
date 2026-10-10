package com.taskcenter.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "telegram_pending_chat_invites", indexes = {
    @Index(name = "idx_pending_chat_invites_expires_at", columnList = "expires_at")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PendingTelegramChatInvite {

    @Id
    @Column(name = "chat_id")
    private Long chatId;

    @Column(nullable = false)
    private String token;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @PrePersist
    public void prePersist() {
        if (createdAt == null) {
            createdAt = java.time.LocalDateTime.now(java.time.ZoneOffset.UTC);
        }
        if (expiresAt == null) {
            expiresAt = java.time.LocalDateTime.now(java.time.ZoneOffset.UTC).plusHours(24);
        }
    }
}
