package com.taskcenter.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "notifications")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Notification {

    @Id
    private String id;

    @Column(name = "user_id", nullable = false)
    private String userId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", insertable = false, updatable = false)
    private User user;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    private String message;

    @Column(name = "is_read", nullable = false)
    @Builder.Default
    private boolean read = false;

    @Column(nullable = false, length = 50)
    private String type; // e.g., NEW_DM, TASK_ASSIGNED

    @Column(name = "reference_id")
    private String referenceId; // e.g., messageId

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
