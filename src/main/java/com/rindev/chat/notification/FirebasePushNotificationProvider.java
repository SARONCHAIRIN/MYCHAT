package com.rindev.chat.notification;

import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.MessagingErrorCode;
import com.google.firebase.messaging.Notification;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class FirebasePushNotificationProvider
                implements PushNotificationProvider {

        private static final Logger log = LoggerFactory.getLogger(
                        FirebasePushNotificationProvider.class);

        private final FirebaseMessaging firebaseMessaging;

        public FirebasePushNotificationProvider(
                        FirebaseMessaging firebaseMessaging) {
                this.firebaseMessaging = firebaseMessaging;
        }

        @Override
        public PushNotificationResult send(
                        PushNotificationRequest request) {

                try {
                        Message.Builder builder =

                                        Message.builder()

                                                        .setToken(request.token())

                                                        .setNotification(

                                                                        Notification.builder()

                                                                                        .setTitle(request.title())

                                                                                        .setBody(request.body())

                                                                                        .build());

                        if (request.data() != null
                                        && !request.data().isEmpty()) {
                                builder.putAllData(request.data());
                        }

                        String messageId = firebaseMessaging.send(
                                        builder.build());

                        log.debug(
                                        "FCM message sent successfully: {}",
                                        messageId);

                        return PushNotificationResult.sent();

                } catch (FirebaseMessagingException ex) {

                        MessagingErrorCode errorCode = ex.getMessagingErrorCode();

                        if (errorCode == MessagingErrorCode.UNREGISTERED) {

                                log.info(
                                                "FCM token is no longer registered");

                                return PushNotificationResult
                                                .invalidDeviceToken();
                        }

                        log.warn(
                                        "FCM send failed. errorCode={}",
                                        errorCode);

                        return PushNotificationResult.failed();
                }
        }
}
