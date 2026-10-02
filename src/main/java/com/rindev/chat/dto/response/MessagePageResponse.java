package com.rindev.chat.dto.response;

import java.util.List;

public record MessagePageResponse(
        List<MessageResponse> messages,
        Long nextCursor,
        boolean hasMore) {
}