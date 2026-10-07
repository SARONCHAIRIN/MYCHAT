package com.rindev.chat.notification;

public record PushNotificationResult(
        boolean success,
        boolean invalidToken) {

    public static PushNotificationResult sent() {
        return new PushNotificationResult(
                true,
                false);
    }

    public static PushNotificationResult failed() {
        return new PushNotificationResult(
                false,
                false);
    }

    public static PushNotificationResult invalidDeviceToken() {
        return new PushNotificationResult(
                false,
                true);
    }
}