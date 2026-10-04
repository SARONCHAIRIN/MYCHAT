package com.rindev.chat.dto.response;

import java.time.Instant;
import java.util.Map;

public record ErrorResponse(
        boolean success,
        String code,
        String message,
        Instant timestamp,
        Map<String, String> errors) {

    public static ErrorResponse of(
            String code,
            String message) {
        return new ErrorResponse(
                false,
                code,
                message,
                Instant.now(),
                Map.of());
    }

    public static ErrorResponse of(
            String code,
            String message,
            Map<String, String> errors) {
        return new ErrorResponse(
                false,
                code,
                message,
                Instant.now(),
                errors == null ? Map.of() : Map.copyOf(errors));
    }
}