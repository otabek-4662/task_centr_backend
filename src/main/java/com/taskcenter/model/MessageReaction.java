package com.taskcenter.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(
    name = "message_reactions",
    uniqueConstraints = {
        @UniqueConstraint(columnNames = {"message_id", "user_id"})
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MessageReaction {

    @Id
    private String id;

    @Column(name = "message_id", nullable = false)
    private String messageId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "message_id", insertable = false, updatable = false)
    private ChatMessage message;

    @Column(name = "user_id", nullable = false)
    private String userId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", insertable = false, updatable = false)
    private User user;

    @Column(nullable = false, length = 50)
    private String emoji;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        if (this.id == null) {
            this.id = UUID.randomUUID().toString();
        }
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
    }
}
