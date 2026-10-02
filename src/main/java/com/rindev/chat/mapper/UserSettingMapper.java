package com.rindev.chat.mapper;

import com.rindev.chat.dto.response.UserSettingResponse;
import com.rindev.chat.entity.UserSetting;
import org.springframework.stereotype.Component;

@Component
public class UserSettingMapper {

    public UserSettingResponse toResponse(UserSetting setting) {
        return new UserSettingResponse(
                setting.getId(),
                setting.getTheme(),
                setting.getMessageNotifications(),
                setting.getGroupNotifications(),
                setting.getReactionNotifications(),
                setting.getReadReceipts(),
                setting.getLastSeenPrivacy(),
                setting.getProfilePhotoPrivacy(),
                setting.getGroupAddPrivacy(),
                setting.getCreatedAt(),
                setting.getUpdatedAt());
    }
}