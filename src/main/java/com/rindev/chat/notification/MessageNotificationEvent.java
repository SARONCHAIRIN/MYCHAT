package com.rindev.chat.notification;

public record MessageNotificationEvent(
        Long messageId,
        Long conversationId,
        Long senderId) {
}