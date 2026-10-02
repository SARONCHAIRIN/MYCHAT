package com.rindev.chat.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RefreshTokenRequest(
        @NotBlank @Size(max = 512)
        @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
        @Schema(accessMode = Schema.AccessMode.WRITE_ONLY,
                description = "Opaque refresh token returned at registration, login, or the last refresh")
        String refreshToken) {

    @Override
    public String toString() {
        return "RefreshTokenRequest[redacted]";
    }
}
