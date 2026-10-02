package com.rindev.chat.repository;

import com.rindev.chat.entity.Message;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MessageRepository extends JpaRepository<Message, Long> {

    List<Message> findByConversationIdOrderByIdDesc(
            Long conversationId,
            Pageable pageable);

    List<Message> findByConversationIdAndIdLessThanOrderByIdDesc(
            Long conversationId,
            Long before,
            Pageable pageable);
}