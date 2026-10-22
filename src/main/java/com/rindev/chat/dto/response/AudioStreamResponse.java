package com.rindev.chat.dto.response;

public record AudioStreamResponse(
        byte[] data,
        String contentType,
        long totalLength,
        long rangeStart,
        long rangeEnd,
        boolean isPartial
) {
}
