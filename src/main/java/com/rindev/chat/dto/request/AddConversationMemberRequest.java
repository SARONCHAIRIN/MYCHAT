package com.rindev.chat.dto.request;

import jakarta.validation.constraints.NotNull;

public record AddConversationMemberRequest(
        @NotNull(message = "User ID is required") Long userId) {
}