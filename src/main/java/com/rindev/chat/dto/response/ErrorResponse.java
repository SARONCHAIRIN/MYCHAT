package com.rindev.chat.dto.response;

import java.time.Instant;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/** Error details contain field names and safe messages, never rejected values. */
public record ErrorResponse(boolean success, String code, String message,
                            Instant timestamp, Map<String, String> errors) {

    public ErrorResponse {
        errors = errors == null ? Map.of() : Collections.unmodifiableMap(new LinkedHashMap<>(errors));
    }

    public static ErrorResponse of(String code, String message) {
        return of(code, message, Map.of());
    }

    public static ErrorResponse of(String code, String message, Map<String, String> errors) {
        return new ErrorResponse(false, code, message, Instant.now(), errors);
    }
}
