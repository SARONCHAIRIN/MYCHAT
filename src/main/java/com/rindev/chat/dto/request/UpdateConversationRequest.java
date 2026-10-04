package com.rindev.chat.dto.request;

import jakarta.validation.constraints.Size;

public record UpdateConversationRequest(

        @Size(max = 150, message = "Name must not exceed 150 characters") String name,

        String description,

        String avatarUrl) {
}