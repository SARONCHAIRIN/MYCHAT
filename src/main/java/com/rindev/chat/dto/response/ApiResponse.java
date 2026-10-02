package com.rindev.chat.dto.response;

import java.time.Instant;

/** The JSON envelope used for successful REST responses. */
public record ApiResponse<T>(boolean success, String code, String message, Instant timestamp, T data) {

    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(true, "OK", "Request successful", Instant.now(), data);
    }

    public static <T> ApiResponse<T> created(T data) {
        return new ApiResponse<>(true, "CREATED", "Resource created", Instant.now(), data);
    }
}
