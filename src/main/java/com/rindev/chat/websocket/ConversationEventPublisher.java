package com.rindev.chat.websocket;

import com.rindev.chat.websocket.event.ConversationRealtimeEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

@Component
public class ConversationEventPublisher {

    private final ApplicationEventPublisher publisher;

    public ConversationEventPublisher(
            ApplicationEventPublisher publisher) {
        this.publisher = publisher;
    }

    public void publish(
            WebSocketEventType type,
            Long conversationId,
            Object data) {

        publisher.publishEvent(
                new ConversationRealtimeEvent(
                        type,
                        conversationId,
                        data));
    }
}