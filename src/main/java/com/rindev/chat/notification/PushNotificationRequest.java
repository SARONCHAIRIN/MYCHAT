package com.rindev.chat.notification;

import java.util.Map;

public record PushNotificationRequest(
        String token,
        String title,
        String body,
        Map<String, String> data) {
}