package com.taskcenter.repository;

import com.taskcenter.model.MessageReaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface MessageReactionRepository extends JpaRepository<MessageReaction, String> {
    Optional<MessageReaction> findByMessageIdAndUserId(String messageId, String userId);
}
