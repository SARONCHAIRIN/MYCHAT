package com.rindev.chat.dto.response;

import com.rindev.chat.enums.MessageType;
import java.time.LocalDateTime;
import java.util.List;

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
        LocalDateTime updatedAt,
        List<UploadResponse> attachments) {

    // Preserve existing callers and API contract tests.
    public MessageResponse(
            Long id, Long conversationId, Long senderId, String senderName,
            String senderUsername, MessageType type, String content,
            Long replyToId, Long forwardedFromId, boolean edited, boolean deleted,
            LocalDateTime editedAt, LocalDateTime deletedAt,
            LocalDateTime createdAt, LocalDateTime updatedAt) {
        this(id, conversationId, senderId, senderName, senderUsername, type,
                content, replyToId, forwardedFromId, edited, deleted,
                editedAt, deletedAt, createdAt, updatedAt, List.of());
    }
}