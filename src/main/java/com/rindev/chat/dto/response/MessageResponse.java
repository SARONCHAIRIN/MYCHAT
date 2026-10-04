package com.rindev.chat.dto.response;

import com.rindev.chat.enums.MessageType;
import java.time.LocalDateTime;

public record MessageResponse(
        Long id,
        Long conversationId,
        Long senderId,
        String senderName,
        String senderUsername,
        MessageType type,
        String content,
        Long replyToId,
        Long forwardedFromId,
        boolean edited,
        boolean deleted,
        LocalDateTime editedAt,
        LocalDateTime deletedAt,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {
}