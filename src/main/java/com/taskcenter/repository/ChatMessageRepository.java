package com.taskcenter.repository;

import com.taskcenter.model.ChatMessage;
import com.taskcenter.model.ChatMessageType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ChatMessageRepository extends JpaRepository<ChatMessage, String> {

    @EntityGraph(attributePaths = {"sender", "replyTo"})
    Page<ChatMessage> findByTypeOrderByCreatedAtDesc(ChatMessageType type, Pageable pageable);

    @EntityGraph(attributePaths = {"sender", "recipient", "replyTo"})
    @Query("SELECT m FROM ChatMessage m WHERE m.type = :type AND " +
           "((m.senderId = :user1 AND m.recipientId = :user2) OR (m.senderId = :user2 AND m.recipientId = :user1)) " +
           "ORDER BY m.createdAt DESC")
    Page<ChatMessage> findDirectMessages(
            @Param("user1") String user1,
            @Param("user2") String user2,
            @Param("type") ChatMessageType type,
            Pageable pageable
    );

    @EntityGraph(attributePaths = {"sender", "recipient", "replyTo", "attachments", "reactions"})
    Optional<ChatMessage> findWithDetailsById(String id);

    @EntityGraph(attributePaths = {"sender", "replyTo"})
    @Query("SELECT m FROM ChatMessage m WHERE m.type = :type AND " +
           "LOWER(m.content) LIKE LOWER(CONCAT('%', :query, '%')) " +
           "ORDER BY m.createdAt DESC")
    Page<ChatMessage> searchPublicMessages(
            @Param("query") String query,
            @Param("type") ChatMessageType type,
            Pageable pageable
    );

    @EntityGraph(attributePaths = {"sender", "recipient", "replyTo"})
    @Query("SELECT m FROM ChatMessage m WHERE m.type = :type AND " +
           "((m.senderId = :user1 AND m.recipientId = :user2) OR (m.senderId = :user2 AND m.recipientId = :user1)) " +
           "AND LOWER(m.content) LIKE LOWER(CONCAT('%', :query, '%')) " +
           "ORDER BY m.createdAt DESC")
    Page<ChatMessage> searchDirectMessages(
            @Param("user1") String user1,
            @Param("user2") String user2,
            @Param("query") String query,
            @Param("type") ChatMessageType type,
            Pageable pageable
    );

    @EntityGraph(attributePaths = {"sender", "recipient", "replyTo"})
    @Query("SELECT m FROM ChatMessage m WHERE " +
           "(m.type = 'PUBLIC' OR (m.type = 'DIRECT' AND (m.senderId = :userId OR m.recipientId = :userId))) " +
           "AND LOWER(m.content) LIKE LOWER(CONCAT('%', :query, '%')) " +
           "ORDER BY m.createdAt DESC")
    Page<ChatMessage> searchAllMessages(
            @Param("userId") String userId,
            @Param("query") String query,
            Pageable pageable
    );

    /**
     * PostgreSQL DISTINCT ON yordamida har bir suhbatdosh bilan faqat ENG OXIRGI
     * shaxsiy xabarni bazadan oladi — barcha DM tarixini xotiraga (RAM) tortmaydi.
     */
    @Query(value = "SELECT * FROM (" +
           "  SELECT DISTINCT ON (LEAST(m.sender_id, m.recipient_id), GREATEST(m.sender_id, m.recipient_id)) m.* " +
           "  FROM chat_messages m " +
           "  WHERE m.type = 'DIRECT' AND m.deleted_at IS NULL " +
           "    AND (m.sender_id = :userId OR m.recipient_id = :userId) " +
           "  ORDER BY LEAST(m.sender_id, m.recipient_id), GREATEST(m.sender_id, m.recipient_id), m.created_at DESC" +
           ") latest ORDER BY latest.created_at DESC",
           nativeQuery = true)
    List<ChatMessage> findLatestDirectMessagesPerConversation(@Param("userId") String userId);

    // Fallback (H2 testlar yoki legacy chaqiruvlar uchun)
    @EntityGraph(attributePaths = {"sender", "recipient", "replyTo"})
    @Query("SELECT m FROM ChatMessage m WHERE m.type = 'DIRECT' " +
           "AND (m.senderId = :userId OR m.recipientId = :userId) " +
           "ORDER BY m.createdAt DESC")
    List<ChatMessage> findAllDirectMessagesForUser(@Param("userId") String userId);

    // DM da o'qilmagan xabarlarni hisoblash
    @Query("SELECT COUNT(m) FROM ChatMessage m WHERE m.type = 'DIRECT' " +
           "AND m.recipientId = :userId AND m.senderId = :otherUserId " +
           "AND m.id NOT IN (SELECT rs.messageId FROM MessageReadStatus rs WHERE rs.userId = :userId)")
    long countUnreadDirectMessages(
            @Param("userId") String userId,
            @Param("otherUserId") String otherUserId
    );

    // Barcha suhbatdoshlar bo'yicha o'qilmagan DM xabarlarni bitta GROUP BY so'rovda hisoblash
    @Query("SELECT m.senderId, COUNT(m) FROM ChatMessage m WHERE m.type = 'DIRECT' " +
           "AND m.recipientId = :userId " +
           "AND m.id NOT IN (SELECT rs.messageId FROM MessageReadStatus rs WHERE rs.userId = :userId) " +
           "GROUP BY m.senderId")
    List<Object[]> countUnreadDirectMessagesGroupedBySender(@Param("userId") String userId);
}
