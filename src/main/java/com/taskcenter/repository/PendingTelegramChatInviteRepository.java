package com.taskcenter.repository;

import com.taskcenter.model.PendingTelegramChatInvite;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface PendingTelegramChatInviteRepository extends JpaRepository<PendingTelegramChatInvite, Long> {

    @Query("SELECT p FROM PendingTelegramChatInvite p WHERE p.chatId = :chatId AND p.expiresAt > :now")
    Optional<PendingTelegramChatInvite> findActiveByChatId(@Param("chatId") Long chatId, @Param("now") LocalDateTime now);

    @Modifying
    @Query("DELETE FROM PendingTelegramChatInvite p WHERE p.expiresAt <= :now")
    int deleteExpired(@Param("now") LocalDateTime now);
}
