package com.rindev.chat.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AddReactionRequest(

        @NotBlank(message = "Emoji is required") @Size(max = 20, message = "Emoji is too long") String emoji

) {
}