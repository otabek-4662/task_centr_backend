package com.taskcenter.repository;

import com.taskcenter.model.MessageReadStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface MessageReadStatusRepository extends JpaRepository<MessageReadStatus, String> {

    boolean existsByMessageIdAndUserId(String messageId, String userId);

    long countByMessageId(String messageId);

    @Query("SELECT rs.messageId, COUNT(rs) FROM MessageReadStatus rs WHERE rs.messageId IN :messageIds GROUP BY rs.messageId")
    List<Object[]> countByMessageIds(@Param("messageIds") Collection<String> messageIds);

    // Belgilangan foydalanuvchining barcha DM xabarlarini o'qilgan deb belgilash
    @Modifying
    @Query(value = "INSERT INTO message_read_status (id, message_id, user_id, read_at) " +
           "SELECT gen_random_uuid(), m.id, :userId, CURRENT_TIMESTAMP " +
           "FROM chat_messages m " +
           "WHERE m.type = 'DIRECT' AND m.sender_id = :senderId AND m.recipient_id = :userId " +
           "AND m.deleted_at IS NULL " +
           "AND m.id NOT IN (SELECT rs.message_id FROM message_read_status rs WHERE rs.user_id = :userId) ",
           nativeQuery = true)
    int markAllAsRead(@Param("userId") String userId, @Param("senderId") String senderId);
}
