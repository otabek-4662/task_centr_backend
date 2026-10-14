package com.taskcenter.repository;

import com.taskcenter.model.EmailDeliveryStatus;
import com.taskcenter.model.InvitationStatus;
import com.taskcenter.model.InvitationType;
import com.taskcenter.model.WorkspaceInvitation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface WorkspaceInvitationRepository extends JpaRepository<WorkspaceInvitation, String> {

    Optional<WorkspaceInvitation> findByTokenHash(String tokenHash);

    Optional<WorkspaceInvitation> findByWorkspaceIdAndTypeAndStatus(String workspaceId, InvitationType type, InvitationStatus status);

    Optional<WorkspaceInvitation> findByWorkspaceIdAndReceiverIdAndStatus(String workspaceId, String receiverId, InvitationStatus status);

    Optional<WorkspaceInvitation> findByWorkspaceIdAndReceiverEmailIgnoreCaseAndStatus(String workspaceId, String receiverEmail, InvitationStatus status);

    List<WorkspaceInvitation> findByReceiverEmailAndStatus(String receiverEmail, InvitationStatus status);

    List<WorkspaceInvitation> findByReceiverIdAndStatusOrderByCreatedAtDesc(String receiverId, InvitationStatus status);

    List<WorkspaceInvitation> findByWorkspaceIdOrderByCreatedAtDesc(String workspaceId);

    long countByWorkspaceIdAndStatus(String workspaceId, InvitationStatus status);

    @Query("SELECT i FROM WorkspaceInvitation i WHERE i.workspaceId = :workspaceId AND i.status = :status ORDER BY i.createdAt DESC")
    List<WorkspaceInvitation> findByWorkspaceIdAndStatus(@Param("workspaceId") String workspaceId, @Param("status") InvitationStatus status);

    @Transactional
    @Modifying(clearAutomatically = true)
    @Query("UPDATE WorkspaceInvitation i " +
           "SET i.status = com.taskcenter.model.InvitationStatus.CANCELLED " +
           "WHERE i.workspaceId = :workspaceId " +
           "  AND i.type = com.taskcenter.model.InvitationType.LINK " +
           "  AND i.status = com.taskcenter.model.InvitationStatus.PENDING")
    int cancelPendingLinkInvitations(@Param("workspaceId") String workspaceId);

    @Transactional
    void deleteByExpiresAtBefore(LocalDateTime date);

    @Transactional
    @Modifying(clearAutomatically = true)
    @Query("UPDATE WorkspaceInvitation i SET i.status = com.taskcenter.model.InvitationStatus.EXPIRED " +
           "WHERE i.status = com.taskcenter.model.InvitationStatus.PENDING AND i.expiresAt < :now")
    int expireOldPendingInvitations(@Param("now") LocalDateTime now);

    @Transactional
    @Modifying(clearAutomatically = true)
    @Query("DELETE FROM WorkspaceInvitation i " +
           "WHERE i.status IN (com.taskcenter.model.InvitationStatus.EXPIRED, com.taskcenter.model.InvitationStatus.CANCELLED, com.taskcenter.model.InvitationStatus.REJECTED) " +
           "  AND i.expiresAt < :cutoff")
    int deleteOldInactiveInvitations(@Param("cutoff") LocalDateTime cutoff);

    @Transactional
    @Modifying(clearAutomatically = true)
    @Query("UPDATE WorkspaceInvitation i " +
           "SET i.useCount = i.useCount + 1, " +
           "    i.status = CASE WHEN i.maxUses IS NOT NULL AND i.useCount + 1 >= i.maxUses THEN com.taskcenter.model.InvitationStatus.ACCEPTED ELSE i.status END " +
           "WHERE i.id = :id " +
           "  AND i.status = com.taskcenter.model.InvitationStatus.PENDING " +
           "  AND (i.expiresAt IS NULL OR i.expiresAt > :now) " +
           "  AND (i.maxUses IS NULL OR i.useCount < i.maxUses)")
    int incrementUseCountAtomic(@Param("id") String id, @Param("now") LocalDateTime now);

    @Transactional
    @Modifying(clearAutomatically = true)
    @Query("UPDATE WorkspaceInvitation i " +
           "SET i.status = com.taskcenter.model.InvitationStatus.ACCEPTED, " +
           "    i.receiverId = :receiverId " +
           "WHERE i.id = :id " +
           "  AND i.status = com.taskcenter.model.InvitationStatus.PENDING " +
           "  AND (i.expiresAt IS NULL OR i.expiresAt > :now)")
    int acceptEmailInvitationAtomic(@Param("id") String id, @Param("receiverId") String receiverId, @Param("now") LocalDateTime now);

    @Transactional
    @Modifying(clearAutomatically = true)
    @Query("UPDATE WorkspaceInvitation i " +
           "SET i.emailDeliveryStatus = :status, " +
           "    i.emailDeliveryError = :error " +
           "WHERE i.id = :id")
    int updateEmailDeliveryStatus(
            @Param("id") String id,
            @Param("status") EmailDeliveryStatus status,
            @Param("error") String error
    );
}
