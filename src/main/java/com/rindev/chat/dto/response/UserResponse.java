package com.rindev.chat.dto.response;

import com.rindev.chat.enums.UserStatus;
import java.time.LocalDateTime;

/** Private profile representation for the authenticated owner; timestamps are UTC. */
public record UserResponse(Long id, String name, String username, String email, String phone,
                           String avatarUrl, String bio, UserStatus status, LocalDateTime lastSeenAt,
                           LocalDateTime createdAt, LocalDateTime updatedAt) {
}
