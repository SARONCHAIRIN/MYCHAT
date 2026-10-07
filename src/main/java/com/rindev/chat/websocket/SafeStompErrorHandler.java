package com.rindev.chat.websocket;

import org.jspecify.annotations.Nullable;
import org.springframework.messaging.Message;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.web.socket.messaging.StompSubProtocolErrorHandler;

/**
 * Keeps internal exceptions, failed frames, and credentials out of STOMP
 * errors.
 */
public final class SafeStompErrorHandler extends StompSubProtocolErrorHandler {

    @Override
    protected Message<byte[]> handleInternal(StompHeaderAccessor errorHeaderAccessor, byte[] errorPayload,
            @Nullable Throwable cause, @Nullable StompHeaderAccessor clientHeaderAccessor) {
        var safe = StompHeaderAccessor.create(StompCommand.ERROR);
        safe.setMessage("WebSocket request could not be processed");
        if (errorHeaderAccessor.getReceiptId() != null) {
            safe.setReceiptId(errorHeaderAccessor.getReceiptId());
        }
        return MessageBuilder.createMessage(new byte[0], safe.getMessageHeaders());
    }
}
