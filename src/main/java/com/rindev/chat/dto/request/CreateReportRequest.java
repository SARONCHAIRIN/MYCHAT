package com.rindev.chat.dto.request;

import com.rindev.chat.enums.ReportReason;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateReportRequest(

        Long reportedUserId,

        Long conversationId,

        Long messageId,

        @NotNull(message = "Reason is required") ReportReason reason,

        @Size(max = 2000, message = "Description must not exceed 2000 characters") String description) {
}