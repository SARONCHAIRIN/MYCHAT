package com.rindev.chat.websocket;

import com.rindev.chat.security.ChatUserDetails;
import com.rindev.chat.security.CustomUserDetailsService;
import com.rindev.chat.security.JwtService;
import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class WebSocketAuthInterceptorTest {

    private final JwtService jwtService = mock(JwtService.class);
    private final CustomUserDetailsService userDetailsService = mock(CustomUserDetailsService.class);
    private final WebSocketAuthorizationService authorization = mock(WebSocketAuthorizationService.class);
    private final WebSocketAuthInterceptor interceptor = new WebSocketAuthInterceptor(
            jwtService, userDetailsService, authorization);

    @ParameterizedTest
    @ValueSource(strings = { "/app/typing/start", "/app/typing/stop" })
    void authenticatedClientsKeepAccessToExistingTypingHandlers(String destination) {
        var headers = send(destination, true);
        var message = MessageBuilder.createMessage(new byte[0], headers.getMessageHeaders());

        assertThat(interceptor.preSend(message, null)).isSameAs(message);
        // Membership remains enforced by the existing typing application handlers.
        verifyNoInteractions(authorization);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = { "/topic/conversations/7", "/queue/private", "/user/7/queue/private",
            "/app/unknown", "/app/typing/start/extra" })
    void clientsCannotPublishServerEventsOrUnknownDestinations(String destination) {
        var headers = send(destination, true);
        var message = MessageBuilder.createMessage(new byte[0], headers.getMessageHeaders());

        assertThatThrownBy(() -> interceptor.preSend(message, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Message destination is not allowed");
        verifyNoInteractions(authorization);
    }

    @Test
    void unauthenticatedClientsCannotSendToTypingHandlers() {
        var headers = send("/app/typing/start", false);
        var message = MessageBuilder.createMessage(new byte[0], headers.getMessageHeaders());

        assertThatThrownBy(() -> interceptor.preSend(message, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("WebSocket authentication required");
    }

    @Test
    void unauthenticatedAuthenticationObjectsCannotSendToTypingHandlers() {
        var headers = send("/app/typing/start", false);
        headers.setUser(UsernamePasswordAuthenticationToken.unauthenticated(principal(), null));
        var message = MessageBuilder.createMessage(new byte[0], headers.getMessageHeaders());

        assertThatThrownBy(() -> interceptor.preSend(message, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("WebSocket authentication required");
    }

    @ParameterizedTest
    @EnumSource(value = StompCommand.class, names = { "CONNECT", "STOMP" })
    void bothConnectCommandsAuthenticateAndDiscardCredentials(StompCommand command) {
        var headers = StompHeaderAccessor.create(command);
        headers.setNativeHeader("Authorization", "Bearer test-credential-marker");
        headers.setLeaveMutable(true);
        var user = principal();
        when(jwtService.parseUserId("test-credential-marker")).thenReturn(7L);
        when(userDetailsService.loadUserById(7L)).thenReturn(user);
        var message = MessageBuilder.createMessage(new byte[0], headers.getMessageHeaders());

        interceptor.preSend(message, null);

        assertThat(headers.getUser()).isInstanceOf(UsernamePasswordAuthenticationToken.class);
        assertThat(headers.getFirstNativeHeader("Authorization")).isNull();
        assertThat(headers.getFirstNativeHeader("authorization")).isNull();
        assertThat(user.getPassword()).isNull();
    }

    @Test
    void failedAuthenticationAlsoDiscardsTheNativeCredential() {
        var headers = StompHeaderAccessor.create(StompCommand.CONNECT);
        headers.setNativeHeader("authorization", "Bearer test-invalid-marker");
        headers.setLeaveMutable(true);
        when(jwtService.parseUserId("test-invalid-marker")).thenThrow(new JwtException("Invalid access token"));
        var message = MessageBuilder.createMessage(new byte[0], headers.getMessageHeaders());

        assertThatThrownBy(() -> interceptor.preSend(message, null)).isInstanceOf(JwtException.class);
        assertThat(headers.getFirstNativeHeader("authorization")).isNull();
    }

    private StompHeaderAccessor send(String destination, boolean authenticated) {
        var headers = StompHeaderAccessor.create(StompCommand.SEND);
        if (destination != null) {
            headers.setDestination(destination);
        }
        if (authenticated) {
            var user = principal();
            headers.setUser(UsernamePasswordAuthenticationToken.authenticated(user, null, user.getAuthorities()));
        }
        return headers;
    }

    private ChatUserDetails principal() {
        return new ChatUserDetails(7L, "test-user", "unused-hash-marker");
    }
}
