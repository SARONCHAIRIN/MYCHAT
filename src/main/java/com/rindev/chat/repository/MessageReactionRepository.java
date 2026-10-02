package com.rindev.chat.repository;

import com.rindev.chat.entity.MessageReaction;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MessageReactionRepository extends JpaRepository<MessageReaction, Long> {
}
