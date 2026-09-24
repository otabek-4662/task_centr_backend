package com.taskcenter.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "message_read_status", indexes = {
    @Index(name = "idx_read_status_message_id", columnList = "message_id"),
    @Index(name = "idx_read_status_user_id", columnList = "user_id, read_at")
}, uniqueConstraints = {
    @UniqueConstraint(name = "uq_read_status_message_user", columnNames = {"message_id", "user_id"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MessageReadStatus {

    @Id
    private String id;

    @Column(name = "message_id", nullable = false)
    private String messageId;

    @Column(name = "user_id", nullable = false)
    private String userId;

    @Column(name = "read_at", nullable = false)
    private LocalDateTime readAt;

    @PrePersist
    public void prePersist() {
        if (this.id == null) {
            this.id = UUID.randomUUID().toString();
        }
        if (this.readAt == null) {
            this.readAt = LocalDateTime.now();
        }
    }
}
