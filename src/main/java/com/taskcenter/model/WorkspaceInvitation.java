package com.taskcenter.model;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "workspace_invitations", indexes = {
        @Index(name = "idx_workspace_invitations_receiver", columnList = "receiver_id"),
        @Index(name = "idx_workspace_invitations_workspace", columnList = "workspace_id"),
        @Index(name = "idx_workspace_invitations_token_hash", columnList = "token_hash", unique = true),
        @Index(name = "idx_workspace_invitations_type", columnList = "type")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EntityListeners(AuditingEntityListener.class)
public class WorkspaceInvitation {

    @Id
    private String id;

    @Column(name = "workspace_id", nullable = false)
    private String workspaceId;

    @Column(name = "sender_id", nullable = false)
    private String senderId;

    @Column(name = "receiver_id")
    private String receiverId;

    @Column(name = "receiver_email")
    private String receiverEmail;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private InvitationType type = InvitationType.EMAIL;

    @Column(name = "token_hash", nullable = false, unique = true, length = 64)
    private String tokenHash;

    @Column(name = "max_uses")
    private Integer maxUses;

    @Column(name = "use_count", nullable = false)
    @Builder.Default
    private Integer useCount = 0;

    @Enumerated(EnumType.STRING)
    @Column(name = "email_delivery_status", nullable = false)
    @Builder.Default
    private EmailDeliveryStatus emailDeliveryStatus = EmailDeliveryStatus.NOT_APPLICABLE;

    @Column(name = "email_delivery_error", length = 500)
    private String emailDeliveryError;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private WorkspaceRole role;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private InvitationStatus status = InvitationStatus.PENDING;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @PrePersist
    public void prePersist() {
        if (this.id == null) {
            this.id = java.util.UUID.randomUUID().toString();
        }
        if (this.type == null) {
            this.type = InvitationType.EMAIL;
        }
        if (this.useCount == null) {
            this.useCount = 0;
        }
        if (this.emailDeliveryStatus == null) {
            this.emailDeliveryStatus = EmailDeliveryStatus.NOT_APPLICABLE;
        }
        if (this.createdAt == null) {
            this.createdAt = java.time.LocalDateTime.now(java.time.ZoneOffset.UTC);
        }
    }
}
