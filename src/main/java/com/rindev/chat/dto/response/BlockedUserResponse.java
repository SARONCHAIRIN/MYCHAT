package com.rindev.chat.dto.response;

import java.time.LocalDateTime;

public record BlockedUserResponse(
        Long id,
        Long userId,
        String username,
        LocalDateTime blockedAt) {
}