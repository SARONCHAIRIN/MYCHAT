package com.rindev.chat.websocket.event;

import com.rindev.chat.websocket.WebSocketEventType;

public record ConversationRealtimeEvent(
        WebSocketEventType type,
        Long conversationId,
        Object data) {
}