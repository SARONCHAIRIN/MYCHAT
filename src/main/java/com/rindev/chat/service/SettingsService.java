package com.rindev.chat.service;

import com.rindev.chat.dto.request.UpdateSettingsRequest;
import com.rindev.chat.dto.response.UserSettingResponse;

public interface SettingsService {

    UserSettingResponse getSettings(Long authenticatedUserId);

    UserSettingResponse updateSettings(
            Long authenticatedUserId,
            UpdateSettingsRequest request);
}