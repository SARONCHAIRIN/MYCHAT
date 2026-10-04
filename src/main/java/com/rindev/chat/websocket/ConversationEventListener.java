package com.rindev.chat.websocket;

import com.rindev.chat.websocket.event.ConversationRealtimeEvent;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class ConversationEventListener {

    private final SimpMessagingTemplate messagingTemplate;

    public ConversationEventListener(
            SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handle(
            ConversationRealtimeEvent event) {

        String destination = "/topic/conversations/"
                + event.conversationId();

        WebSocketEvent<Object> payload = WebSocketEvent.of(
                event.type(),
                event.conversationId(),
                event.data());

        messagingTemplate.convertAndSend(
                destination,
                payload);
    }
}