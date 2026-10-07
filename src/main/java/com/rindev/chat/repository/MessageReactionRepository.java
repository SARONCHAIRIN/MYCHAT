package com.rindev.chat.repository;

import com.rindev.chat.entity.MessageReaction;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MessageReactionRepository
        extends JpaRepository<MessageReaction, Long> {

    List<MessageReaction> findByMessageIdOrderByCreatedAtAsc(
            Long messageId);

    boolean existsByMessageIdAndUserIdAndEmoji(
            Long messageId,
            Long userId,
            String emoji);

    Optional<MessageReaction> findByMessageIdAndUserIdAndEmoji(
            Long messageId,
            Long userId,
            String emoji);
}