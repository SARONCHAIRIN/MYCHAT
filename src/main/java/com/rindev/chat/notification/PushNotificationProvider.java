package com.rindev.chat.notification;

public interface PushNotificationProvider {

    PushNotificationResult send(
            PushNotificationRequest request);
}