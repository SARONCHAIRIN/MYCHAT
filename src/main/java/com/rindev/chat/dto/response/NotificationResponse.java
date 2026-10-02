package com.rindev.chat.dto.response;

import com.rindev.chat.enums.NotificationType;
import java.time.LocalDateTime;

public record NotificationResponse(
        Long id,
        NotificationType type,
        String title,
        String body,
        Long conversationId,
        Long messageId,
        boolean read,
        LocalDateTime readAt,
        LocalDateTime createdAt) {
}