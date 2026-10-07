package com.rindev.chat.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateAvatarRequest(

        @NotBlank(message = "Avatar URL is required") @Size(max = 2048, message = "Avatar URL must not exceed 2048 characters") String avatarUrl

) {
}