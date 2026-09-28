package com.taskcenter.repository;

import com.taskcenter.model.InvitationStatus;
import com.taskcenter.model.WorkspaceInvitation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WorkspaceInvitationRepository extends JpaRepository<WorkspaceInvitation, String> {
    
    Optional<WorkspaceInvitation> findByWorkspaceIdAndReceiverIdAndStatus(String workspaceId, String receiverId, InvitationStatus status);
    
    List<WorkspaceInvitation> findByReceiverEmailAndStatus(String receiverEmail, InvitationStatus status);
    
    List<WorkspaceInvitation> findByReceiverIdAndStatusOrderByCreatedAtDesc(String receiverId, InvitationStatus status);
    
    List<WorkspaceInvitation> findByWorkspaceIdOrderByCreatedAtDesc(String workspaceId);
    
    void deleteByExpiresAtBefore(java.time.LocalDateTime date);
}
