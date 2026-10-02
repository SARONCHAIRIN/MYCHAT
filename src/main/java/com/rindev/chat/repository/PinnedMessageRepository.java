package com.rindev.chat.repository;

import com.rindev.chat.entity.PinnedMessage;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PinnedMessageRepository extends JpaRepository<PinnedMessage, Long> {
}
