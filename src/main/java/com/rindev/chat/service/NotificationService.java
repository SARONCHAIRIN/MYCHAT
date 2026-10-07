package com.rindev.chat.service;

import com.rindev.chat.dto.response.NotificationPageResponse;
import com.rindev.chat.dto.response.NotificationResponse;

public interface NotificationService {

    NotificationPageResponse getNotifications(
            Long userId,
            int page,
            int size);

    NotificationResponse markRead(
            Long userId,
            Long notificationId);

    void markAllRead(Long userId);

    void delete(
            Long userId,
            Long notificationId);
}