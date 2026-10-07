package com.rindev.chat.websocket;

import com.rindev.chat.security.ChatUserDetails;
import org.springframework.context.event.EventListener;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

@Component
public class WebSocketPresenceListener {

    private final PresenceService presenceService;

    public WebSocketPresenceListener(
            PresenceService presenceService) {

        this.presenceService = presenceService;
    }

    @EventListener
    public void handleConnected(
            SessionConnectedEvent event) {

        ChatUserDetails userDetails = extractUserDetails(event.getUser());

        if (userDetails == null) {
            return;
        }

        String sessionId = event.getMessage()
                .getHeaders()
                .get("simpSessionId", String.class);

        if (sessionId == null) {
            return;
        }

        boolean becameOnline = presenceService.connected(
                userDetails.getId(),
                sessionId);

        if (!becameOnline) {
            return;
        }

        PresenceEventPayload payload = new PresenceEventPayload(
                userDetails.getId(),
                userDetails.getUsername(),
                true,
                null);

        presenceService.broadcastPresence(
                userDetails.getId(),
                WebSocketEventType.USER_ONLINE,
                payload);
    }

    @EventListener
    public void handleDisconnected(
            SessionDisconnectEvent event) {

        ChatUserDetails userDetails = extractUserDetails(event.getUser());

        if (userDetails == null) {
            return;
        }

        PresenceEventPayload payload = presenceService.disconnected(
                userDetails.getId(),
                event.getSessionId());

        if (payload == null) {
            return;
        }

        presenceService.broadcastPresence(
                userDetails.getId(),
                WebSocketEventType.USER_OFFLINE,
                payload);
    }

    private ChatUserDetails extractUserDetails(
            java.security.Principal principal) {

        if (!(principal instanceof Authentication authentication)) {
            return null;
        }

        Object authenticatedPrincipal = authentication.getPrincipal();

        if (!(authenticatedPrincipal instanceof ChatUserDetails userDetails)) {
            return null;
        }

        return userDetails;
    }
}