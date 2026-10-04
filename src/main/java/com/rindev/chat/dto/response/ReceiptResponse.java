package com.rindev.chat.dto.response;

import java.time.LocalDateTime;

public record ReceiptResponse(
        Long id,
        Long messageId,
        Long userId,
        LocalDateTime deliveredAt,
        LocalDateTime readAt) {
}