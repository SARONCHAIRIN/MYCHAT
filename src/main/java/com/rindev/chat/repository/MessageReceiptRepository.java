package com.rindev.chat.repository;

import com.rindev.chat.entity.MessageReceipt;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MessageReceiptRepository
        extends JpaRepository<MessageReceipt, Long> {

    Optional<MessageReceipt> findByMessageIdAndUserId(
            Long messageId,
            Long userId);
}