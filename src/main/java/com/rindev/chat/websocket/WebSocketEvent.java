package com.rindev.chat.websocket;

import java.time.Instant;

public record WebSocketEvent<T>(
        String type,
        Long conversationId,
        T data,
        Instant timestamp) {

    public static <T> WebSocketEvent<T> of(
            WebSocketEventType type,
            Long conversationId,
            T data) {

        return new WebSocketEvent<>(
                type.getValue(),
                conversationId,
                data,
                Instant.now());
    }

}
