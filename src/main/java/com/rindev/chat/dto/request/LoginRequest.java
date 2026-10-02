package com.rindev.chat.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.rindev.chat.validation.MaxUtf8Bytes;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LoginRequest(
        @NotBlank @Size(max = 50) String username,
        @NotBlank @MaxUtf8Bytes(72)
        @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
        @Schema(format = "password", accessMode = Schema.AccessMode.WRITE_ONLY) String password) {

    @Override
    public String toString() {
        return "LoginRequest[redacted]";
    }
}
