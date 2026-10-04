package com.rindev.chat.websocket;

import com.rindev.chat.security.ChatUserDetails;
import java.security.Principal;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;

@Controller
public class TypingController {

    private final WebSocketAuthorizationService authorizationService;
    private final SimpMessagingTemplate messagingTemplate;

    public TypingController(
            WebSocketAuthorizationService authorizationService,
            SimpMessagingTemplate messagingTemplate) {

        this.authorizationService = authorizationService;
        this.messagingTemplate = messagingTemplate;
    }

    @MessageMapping("/typing/start")
    public void typingStart(
            TypingEventRequest request,
            Principal principal) {

        publishTyping(
                request,
                principal,
                WebSocketEventType.TYPING_START);
    }

    @MessageMapping("/typing/stop")
    public void typingStop(
            TypingEventRequest request,
            Principal principal) {

        publishTyping(
                request,
                principal,
                WebSocketEventType.TYPING_STOP);
    }

    private void publishTyping(
            TypingEventRequest request,
            Principal principal,
            WebSocketEventType type) {

        // 1. Validate request
        if (request == null
                || request.conversationId() == null
                || request.conversationId() <= 0) {

            throw new IllegalArgumentException(
                    "Valid conversationId is required");
        }

        // 2. Extract authenticated ChatUserDetails
        ChatUserDetails userDetails = requireUserDetails(principal);

        Long conversationId = request.conversationId();

        // 3. Check conversation membership
        if (!authorizationService.canAccessConversation(
                userDetails.getId(),
                conversationId)) {

            throw new IllegalArgumentException(
                    "Conversation access is not allowed");
        }

        // 4. Build typing payload
        TypingEventPayload payload = new TypingEventPayload(
                userDetails.getId(),
                userDetails.getUsername());

        // 5. Build WebSocket event
        WebSocketEvent<TypingEventPayload> event = WebSocketEvent.of(
                type,
                conversationId,
                payload);

        // 6. Broadcast directly
        // Typing is ephemeral and does not use DB transaction.
        messagingTemplate.convertAndSend(
                "/topic/conversations/" + conversationId,
                event);
    }

    private ChatUserDetails requireUserDetails(
            Principal principal) {

        if (!(principal instanceof Authentication authentication)) {
            throw new IllegalArgumentException(
                    "WebSocket authentication required");
        }

        Object authenticatedPrincipal = authentication.getPrincipal();

        if (!(authenticatedPrincipal instanceof ChatUserDetails userDetails)) {

            throw new IllegalArgumentException(
                    "WebSocket authentication required");
        }

        return userDetails;
    }
}