package com.rindev.chat.dto.request;

import com.rindev.chat.enums.ConversationType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

public record CreateConversationRequest(

        @NotNull(message = "Conversation type is required") ConversationType type,

        @Size(max = 150, message = "Name must not exceed 150 characters") String name,

        String description,

        String avatarUrl,

        Long memberId,

        List<Long> memberIds) {
}