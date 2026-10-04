package com.rindev.chat.dto.response;

import com.rindev.chat.enums.ReportReason;
import com.rindev.chat.enums.ReportStatus;

import java.time.LocalDateTime;

public record ReportResponse(
        Long id,
        Long reporterId,
        Long reportedUserId,
        Long conversationId,
        Long messageId,
        ReportReason reason,
        String description,
        ReportStatus status,
        LocalDateTime createdAt) {
}