package com.rindev.chat.notification;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

@Component
public class MessageNotificationPublisher {

    private final ApplicationEventPublisher publisher;

    public MessageNotificationPublisher(
            ApplicationEventPublisher publisher) {
        this.publisher = publisher;
    }

    public void publish(
            Long messageId,
            Long conversationId,
            Long senderId) {

        publisher.publishEvent(
                new MessageNotificationEvent(
                        messageId,
                        conversationId,
                        senderId));
    }
}