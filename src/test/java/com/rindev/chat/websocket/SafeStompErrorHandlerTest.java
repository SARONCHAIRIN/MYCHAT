package com.rindev.chat.websocket;

import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.Message;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.messaging.support.MessageHeaderAccessor;

import static org.assertj.core.api.Assertions.assertThat;

class SafeStompErrorHandlerTest {

    private final SafeStompErrorHandler handler = new SafeStompErrorHandler();

    @Test
    void clientErrorsDiscardExceptionDetailsAndAuthorizationHeaders() {
        var request = StompHeaderAccessor.create(StompCommand.CONNECT);
        request.setNativeHeader("Authorization", "private-authorization-marker");
        request.setReceipt("request-1");
        Message<byte[]> error = handler.handleClientMessageProcessingError(
                MessageBuilder.createMessage(new byte[0], request.getMessageHeaders()),
                new IllegalStateException("private-exception-marker"));

        var headers = MessageHeaderAccessor.getAccessor(error, StompHeaderAccessor.class);
        assertThat(headers.getCommand()).isEqualTo(StompCommand.ERROR);
        assertThat(headers.getMessage()).isEqualTo("WebSocket request could not be processed");
        assertThat(headers.getReceiptId()).isEqualTo("request-1");
        assertThat(headers.getFirstNativeHeader("Authorization")).isNull();
        assertThat(error.toString()).doesNotContain("private-authorization-marker", "private-exception-marker");
        assertThat(error.getPayload()).isEmpty();
    }

    @Test
    void decodingErrorsWithoutAClientFrameHaveSafeResponses() {
        Message<byte[]> error = handler.handleClientMessageProcessingError(null,
                new IllegalArgumentException("private-decoder-marker"));

        var headers = MessageHeaderAccessor.getAccessor(error, StompHeaderAccessor.class);
        assertThat(headers.getMessage()).isEqualTo("WebSocket request could not be processed");
        assertThat(headers.getReceiptId()).isNull();
        assertThat(error.toString()).doesNotContain("private-decoder-marker");
        assertThat(error.getPayload()).isEmpty();
    }

    @Test
    void outboundErrorsDiscardInternalPayloadAndExtraHeaders() {
        var original = StompHeaderAccessor.create(StompCommand.ERROR);
        original.setMessage("private-error-marker");
        original.setNativeHeader("internal-details", "private-header-marker");
        original.setReceiptId("request-2");
        Message<byte[]> error = handler.handleErrorMessageToClient(MessageBuilder.createMessage(
                "private-payload-marker".getBytes(StandardCharsets.UTF_8), original.getMessageHeaders()));

        var headers = MessageHeaderAccessor.getAccessor(error, StompHeaderAccessor.class);
        assertThat(headers.getMessage()).isEqualTo("WebSocket request could not be processed");
        assertThat(headers.getReceiptId()).isEqualTo("request-2");
        assertThat(headers.getFirstNativeHeader("internal-details")).isNull();
        assertThat(error.toString()).doesNotContain("private-error-marker", "private-header-marker");
        assertThat(error.getPayload()).isEmpty();
    }
}
