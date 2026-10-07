package com.rindev.chat.notification;

import com.rindev.chat.service.PushNotificationService;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class MessageNotificationListener {

    private final PushNotificationService service;

    public MessageNotificationListener(
            PushNotificationService service) {
        this.service = service;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handle(
            MessageNotificationEvent event) {

        service.sendNewMessageNotifications(event);
    }
}