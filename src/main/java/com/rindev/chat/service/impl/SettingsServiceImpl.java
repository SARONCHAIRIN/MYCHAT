package com.rindev.chat.service.impl;

import com.rindev.chat.dto.request.UpdateSettingsRequest;
import com.rindev.chat.dto.response.UserSettingResponse;
import com.rindev.chat.entity.UserSetting;
import com.rindev.chat.exception.ResourceNotFoundException;
import com.rindev.chat.mapper.UserSettingMapper;
import com.rindev.chat.repository.UserSettingRepository;
import com.rindev.chat.service.SettingsService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class SettingsServiceImpl implements SettingsService {

    private final UserSettingRepository userSettingRepository;
    private final UserSettingMapper userSettingMapper;

    public SettingsServiceImpl(
            UserSettingRepository userSettingRepository,
            UserSettingMapper userSettingMapper) {
        this.userSettingRepository = userSettingRepository;
        this.userSettingMapper = userSettingMapper;
    }

    @Override
    public UserSettingResponse getSettings(Long authenticatedUserId) {
        return userSettingMapper.toResponse(
                findSettings(authenticatedUserId));
    }

    @Override
    @Transactional
    public UserSettingResponse updateSettings(
            Long authenticatedUserId,
            UpdateSettingsRequest request) {
        UserSetting settings = findSettings(authenticatedUserId);

        if (request.theme() != null) {
            settings.setTheme(request.theme());
        }

        if (request.messageNotifications() != null) {
            settings.setMessageNotifications(
                    request.messageNotifications());
        }

        if (request.groupNotifications() != null) {
            settings.setGroupNotifications(
                    request.groupNotifications());
        }

        if (request.reactionNotifications() != null) {
            settings.setReactionNotifications(
                    request.reactionNotifications());
        }

        if (request.readReceipts() != null) {
            settings.setReadReceipts(
                    request.readReceipts());
        }

        if (request.lastSeenPrivacy() != null) {
            settings.setLastSeenPrivacy(
                    request.lastSeenPrivacy());
        }

        if (request.profilePhotoPrivacy() != null) {
            settings.setProfilePhotoPrivacy(
                    request.profilePhotoPrivacy());
        }

        if (request.groupAddPrivacy() != null) {
            settings.setGroupAddPrivacy(
                    request.groupAddPrivacy());
        }

        UserSetting saved = userSettingRepository.save(settings);

        return userSettingMapper.toResponse(saved);
    }

    private UserSetting findSettings(Long userId) {
        return userSettingRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "User settings not found"));
    }
}