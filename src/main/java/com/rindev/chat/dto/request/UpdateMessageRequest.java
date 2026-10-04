package com.rindev.chat.dto.request;

import jakarta.validation.constraints.NotBlank;

public record UpdateMessageRequest(
        @NotBlank(message = "Content is required") String content) {
}