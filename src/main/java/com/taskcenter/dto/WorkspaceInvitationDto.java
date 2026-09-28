package com.taskcenter.dto;

import com.taskcenter.model.InvitationStatus;
import com.taskcenter.model.WorkspaceInvitation;
import com.taskcenter.model.WorkspaceRole;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class WorkspaceInvitationDto {
    private String id;
    private String workspaceId;
    private String senderId;
    private String receiverId;
    private String receiverEmail;
    private WorkspaceRole role;
    private InvitationStatus status;
    private LocalDateTime createdAt;
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
