package com.taskcenter.repository;

import com.taskcenter.model.InvitationStatus;
import com.taskcenter.model.WorkspaceInvitation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WorkspaceInvitationRepository extends JpaRepository<WorkspaceInvitation, String> {
    
    Optional<WorkspaceInvitation> findByWorkspaceIdAndReceiverIdAndStatus(String workspaceId, String receiverId, InvitationStatus status);
    
    Optional<WorkspaceInvitation> findByWorkspaceIdAndReceiverEmailIgnoreCaseAndStatus(String workspaceId, String receiverEmail, InvitationStatus status);
    
    List<WorkspaceInvitation> findByReceiverEmailAndStatus(String receiverEmail, InvitationStatus status);
    
    List<WorkspaceInvitation> findByReceiverIdAndStatusOrderByCreatedAtDesc(String receiverId, InvitationStatus status);
    
    List<WorkspaceInvitation> findByWorkspaceIdOrderByCreatedAtDesc(String workspaceId);

    // DB darajasida PENDING takliflar sonini hisoblash (Java stream filter o'rniga)
    long countByWorkspaceIdAndStatus(String workspaceId, InvitationStatus status);

    // DB darajasida faqat berilgan statusdagi takliflarni olish
    @Query("SELECT i FROM WorkspaceInvitation i WHERE i.workspaceId = :workspaceId AND i.status = :status ORDER BY i.createdAt DESC")
    List<WorkspaceInvitation> findByWorkspaceIdAndStatus(@Param("workspaceId") String workspaceId, @Param("status") InvitationStatus status);

    void deleteByExpiresAtBefore(java.time.LocalDateTime date);
}
