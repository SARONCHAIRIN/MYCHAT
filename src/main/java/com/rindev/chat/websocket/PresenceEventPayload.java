package com.rindev.chat.websocket;

import java.time.LocalDateTime;

public record PresenceEventPayload(
        Long userId,
        String username,
        boolean online,
        LocalDateTime lastSeenAt) {
}