package com.rindev.chat.dto.response;

import com.rindev.chat.enums.MemberRole;
import java.time.LocalDateTime;

public record ConversationMemberResponse(
        Long id,
        Long userId,
        String name,
        String username,
        String avatarUrl,
        MemberRole role,
        LocalDateTime joinedAt) {
}