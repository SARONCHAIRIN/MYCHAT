package com.rindev.chat.repository;

import com.rindev.chat.entity.PinnedMessage;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PinnedMessageRepository
        extends JpaRepository<PinnedMessage, Long> {

    List<PinnedMessage> findByConversationIdOrderByPinnedAtDesc(Long conversationId);

    Optional<PinnedMessage> findByConversationIdAndMessageId(
            Long conversationId,
            Long messageId);

    boolean existsByConversationIdAndMessageId(
            Long conversationId,
            Long messageId);
}