package com.rindev.chat.repository;

import com.rindev.chat.entity.MessageAttachment;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MessageAttachmentRepository
                extends JpaRepository<MessageAttachment, Long> {

        List<MessageAttachment> findByMessageId(
                        Long messageId);
}