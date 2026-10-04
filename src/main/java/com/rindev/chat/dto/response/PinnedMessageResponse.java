package com.rindev.chat.dto.response;

import java.time.LocalDateTime;

public record PinnedMessageResponse(
        Long id,
        Long conversationId,
        Long messageId,
        Long pinnedBy,
        LocalDateTime pinnedAt) {
}