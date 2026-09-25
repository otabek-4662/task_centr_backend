package com.taskcenter.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "message_attachments")
@SQLDelete(sql = "UPDATE message_attachments SET deleted_at = CURRENT_TIMESTAMP WHERE id = ?")
@Where(clause = "deleted_at IS NULL")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MessageAttachment {

    @Id
    private String id;

    @Column(name = "message_id")
    private String messageId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "message_id", insertable = false, updatable = false)
    private ChatMessage message;

    @Column(name = "file_url", nullable = false)
    private String fileUrl;

    @Column(name = "file_type", nullable = false, length = 100)
    private String fileType; // image/png, application/pdf

    @Column(name = "file_size")
    private Long fileSize; // bytes

    @Column(name = "thumbnail_url")
    private String thumbnailUrl; // faqat rasmlar uchun

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

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
