package com.taskcenter.dto;

import com.taskcenter.model.InvitationStatus;
import com.taskcenter.model.WorkspaceInvitation;
import com.taskcenter.model.WorkspaceRole;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class WorkspaceInvitationDto {
    @Schema(description = "Taklifnoma identifikatori (UUID)", example = "inv-12345")
    private String id;

    @Schema(description = "Taklif qilingan workspace identifikatori", example = "ws-12345")
    private String workspaceId;

    @Schema(description = "Taklif yuborgan foydalanuvchi ID si", example = "u-11111")
    private String senderId;

    @Schema(description = "Taklif qilingan foydalanuvchi ID si (mavjud bo'lsa)", example = "u-22222")
    private String receiverId;

    @Schema(description = "Taklif qilingan foydalanuvchi elektron pochtasi", example = "hamkasb@example.com")
    private String receiverEmail;

    @Schema(description = "Taklif qilingan rol (ADMIN, MEMBER, VIEWER)", example = "MEMBER")
    private WorkspaceRole role;

    @Schema(description = "Taklif holati (PENDING, ACCEPTED, REJECTED, EXPIRED)", example = "PENDING")
    private InvitationStatus status;

    @Schema(description = "Yaratilgan vaqti", example = "2026-10-01T10:00:00")
    private LocalDateTime createdAt;

    @Schema(description = "Amal qilish muddati", example = "2026-10-08T10:00:00")
    private LocalDateTime expiresAt;

    public static WorkspaceInvitationDto fromEntity(WorkspaceInvitation entity) {
        if (entity == null) return null;
        return WorkspaceInvitationDto.builder()
                .id(entity.getId())
                .workspaceId(entity.getWorkspaceId())
                .senderId(entity.getSenderId())
                .receiverId(entity.getReceiverId())
                .receiverEmail(entity.getReceiverEmail())
                .role(entity.getRole())
                .status(entity.getStatus())
                .createdAt(entity.getCreatedAt())
                .expiresAt(entity.getExpiresAt())
                .build();
    }
}
