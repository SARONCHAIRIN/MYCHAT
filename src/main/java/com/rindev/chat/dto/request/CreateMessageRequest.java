package com.rindev.chat.dto.request;

import com.rindev.chat.enums.MessageType;
import jakarta.validation.constraints.NotNull;

public record CreateMessageRequest(
        @NotNull(message = "Message type is required") MessageType type,
        String content,
        Long replyToId,
        Long forwardedFromId) {
}