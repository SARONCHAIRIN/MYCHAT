package com.rindev.chat.dto.response;

import com.rindev.chat.enums.ConversationType;
import java.time.LocalDateTime;
import java.util.List;

public record ConversationResponse(
        Long id,
        ConversationType type,
        String name,
        String description,
        String avatarUrl,
        Long createdBy,
        LocalDateTime lastMessageAt,
        List<ConversationMemberResponse> members,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {
}