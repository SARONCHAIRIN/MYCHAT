package com.rindev.chat.repository;

import com.rindev.chat.entity.MessageAttachment;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MessageAttachmentRepository
                extends JpaRepository<MessageAttachment, Long> {

        Optional<MessageAttachment> findByFileUrl(String fileUrl);

        long countByFileUrl(String fileUrl);

    List<MessageAttachment> findByMessageId(
                        Long messageId);

        List<MessageAttachment> findByMessageIdIn(List<Long> messageIds);
}