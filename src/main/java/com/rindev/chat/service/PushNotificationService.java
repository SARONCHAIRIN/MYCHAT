package com.rindev.chat.service;

import com.rindev.chat.notification.MessageNotificationEvent;

public interface PushNotificationService {

    void sendNewMessageNotifications(
            MessageNotificationEvent event);
}