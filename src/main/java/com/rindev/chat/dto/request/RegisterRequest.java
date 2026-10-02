package com.rindev.chat.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.rindev.chat.validation.MaxUtf8Bytes;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank @Size(max = 100) String name,
        @NotBlank @Pattern(regexp = "[A-Za-z0-9_]{3,50}",
                message = "must contain 3 to 50 ASCII letters, digits, or underscores") String username,
        @Email @Size(max = 255) String email,
        @NotBlank @Size(min = 8, max = 72) @MaxUtf8Bytes(72)
        @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
        @Schema(format = "password", accessMode = Schema.AccessMode.WRITE_ONLY,
                description = "At least 8 characters and at most 72 UTF-8 bytes; never trimmed") String password) {

    @Override
    public String toString() {
        return "RegisterRequest[redacted]";
    }
}
