package com.rindev.chat.websocket;

import com.rindev.chat.security.ChatUserDetails;
import com.rindev.chat.security.CustomUserDetailsService;
import com.rindev.chat.security.JwtService;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Component;

@Component
public class WebSocketAuthInterceptor implements ChannelInterceptor {

    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtService jwtService;
    private final CustomUserDetailsService userDetailsService;
    private final WebSocketAuthorizationService authorizationService;

    public WebSocketAuthInterceptor(
            JwtService jwtService,
            CustomUserDetailsService userDetailsService,
            WebSocketAuthorizationService authorizationService) {

        this.jwtService = jwtService;
        this.userDetailsService = userDetailsService;
        this.authorizationService = authorizationService;
    }

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {

        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(
                message,
                StompHeaderAccessor.class);

        if (accessor == null) {
            return message;
        }

        StompCommand command = accessor.getCommand();

        if (command == null) {
            return message;
        }

        if (StompCommand.CONNECT.equals(command)) {
            authenticate(accessor);
        }

        if (StompCommand.SUBSCRIBE.equals(command)) {
            authorizeSubscription(accessor);
        }

        return message;
    }

    private void authenticate(StompHeaderAccessor accessor) {

        String authorization = accessor.getFirstNativeHeader("Authorization");

        if (authorization == null) {
            authorization = accessor.getFirstNativeHeader("authorization");
        }

        if (authorization == null
                || !authorization.startsWith(BEARER_PREFIX)) {

            throw new IllegalArgumentException(
                    "WebSocket authentication required");
        }

        String token = authorization
                .substring(BEARER_PREFIX.length())
                .trim();

        if (token.isEmpty()) {
            throw new IllegalArgumentException(
                    "WebSocket authentication required");
        }

        Long userId = jwtService.parseUserId(token);

        ChatUserDetails principal = userDetailsService.loadUserById(userId);

        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                principal,
                null,
                principal.getAuthorities());

        accessor.setUser(authentication);
    }

    private void authorizeSubscription(
            StompHeaderAccessor accessor) {

        if (!(accessor.getUser() instanceof UsernamePasswordAuthenticationToken authentication)) {

            throw new IllegalArgumentException(
                    "WebSocket authentication required");
        }

        Object principal = authentication.getPrincipal();

        if (!(principal instanceof ChatUserDetails userDetails)) {
            throw new IllegalArgumentException(
                    "Invalid WebSocket principal");
        }

        String destination = accessor.getDestination();

        if (destination == null) {
            throw new IllegalArgumentException(
                    "Subscription destination is required");
        }

        Long conversationId = extractConversationId(destination);

        if (conversationId == null) {
            throw new IllegalArgumentException(
                    "Subscription destination is not allowed");
        }

        boolean allowed = authorizationService.canAccessConversation(
                userDetails.getId(),
                conversationId);

        if (!allowed) {
            throw new IllegalArgumentException(
                    "Conversation subscription is not allowed");
        }
    }

    private Long extractConversationId(String destination) {

        String prefix = "/topic/conversations/";

        if (!destination.startsWith(prefix)) {
            return null;
        }

        String value = destination.substring(prefix.length());

        if (value.isBlank() || value.contains("/")) {
            return null;
        }

        try {
            long conversationId = Long.parseLong(value);

            return conversationId > 0
                    ? conversationId
                    : null;

        } catch (NumberFormatException exception) {
            return null;
        }
    }
}