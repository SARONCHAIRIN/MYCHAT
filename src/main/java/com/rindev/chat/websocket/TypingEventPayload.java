package com.rindev.chat.websocket;

public record TypingEventPayload(
        Long userId,
        String username) {
}