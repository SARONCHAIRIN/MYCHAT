package com.rindev.chat.dto.response;

import java.time.LocalDateTime;

public record ReactionResponse(
        Long id,
        Long messageId,
        Long userId,
        String username,
        String emoji,
        LocalDateTime createdAt) {
}