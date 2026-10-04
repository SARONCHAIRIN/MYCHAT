package com.rindev.chat.dto.request;

import com.rindev.chat.enums.PrivacyLevel;
import com.rindev.chat.enums.Theme;

public record UpdateSettingsRequest(
                Theme theme,
                Boolean messageNotifications,
                Boolean groupNotifications,
                Boolean reactionNotifications,
                Boolean readReceipts,
                PrivacyLevel lastSeenPrivacy,
                PrivacyLevel profilePhotoPrivacy,
                PrivacyLevel groupAddPrivacy) {
}