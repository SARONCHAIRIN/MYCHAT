package com.rindev.chat.dto.request;

import com.rindev.chat.enums.DevicePlatform;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record RegisterDeviceRequest(

        @Size(max = 100) String deviceName,

        @NotNull DevicePlatform platform,

        @NotBlank String fcmToken

) {
}