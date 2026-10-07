package com.rindev.chat.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

public record UpdateUserRequest(

        @Size(min = 1, max = 100, message = "Name must be between 1 and 100 characters") String name,

        @Size(min = 3, max = 50, message = "Username must be between 3 and 50 characters") String username,

        @Email(message = "Email must be valid") @Size(max = 255, message = "Email must not exceed 255 characters") String email,

        @Size(max = 30, message = "Phone must not exceed 30 characters") String phone,

        @Size(max = 255, message = "Bio must not exceed 255 characters") String bio) {
}