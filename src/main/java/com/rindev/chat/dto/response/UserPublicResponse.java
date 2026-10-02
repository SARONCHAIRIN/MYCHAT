package com.rindev.chat.dto.response;

import com.rindev.chat.enums.UserStatus;
import java.time.LocalDateTime;

/** Safe public profile representation for user discovery. */
public record UserPublicResponse(
        Long id,
        String name,
        String username,
        String avatarUrl,
        String bio,
        UserStatus status,
        LocalDateTime lastSeenAt) {
}