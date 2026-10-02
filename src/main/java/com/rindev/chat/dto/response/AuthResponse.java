package com.rindev.chat.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;

public record AuthResponse(
        String accessToken,
        String refreshToken,
        @Schema(example = "Bearer") String tokenType,
        @Schema(description = "Access-token lifetime in seconds", example = "900") long expiresIn,
        Instant refreshExpiresAt,
        UserResponse user) {

    @Override
    public String toString() {
        return "AuthResponse[redacted]";
    }
}
