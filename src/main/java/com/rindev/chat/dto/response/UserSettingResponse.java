package com.rindev.chat.dto.response;

import com.rindev.chat.enums.PrivacyLevel;
import com.rindev.chat.enums.Theme;
import java.time.LocalDateTime;

public record UserSettingResponse(
        Long id,
        Theme theme,
        Boolean messageNotifications,
        Boolean groupNotifications,
        Boolean reactionNotifications,
        Boolean readReceipts,
        PrivacyLevel lastSeenPrivacy,
        PrivacyLevel profilePhotoPrivacy,
        PrivacyLevel groupAddPrivacy,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {
}