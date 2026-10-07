package com.rindev.chat.dto.response;

import com.rindev.chat.enums.DevicePlatform;
import java.time.LocalDateTime;

public record DeviceResponse(
        Long id,
        String deviceName,
        DevicePlatform platform,
        LocalDateTime lastActiveAt,
        LocalDateTime createdAt) {
}