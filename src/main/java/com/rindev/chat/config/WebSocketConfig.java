package com.rindev.chat.config;

import com.rindev.chat.websocket.WebSocketAuthInterceptor;
import com.rindev.chat.websocket.SafeStompErrorHandler;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig
        implements WebSocketMessageBrokerConfigurer {

    private final WebSocketAuthInterceptor authInterceptor;
    private final OriginPolicy origins;

    public WebSocketConfig(
            WebSocketAuthInterceptor authInterceptor, OriginPolicy origins) {
        this.authInterceptor = authInterceptor;
        this.origins = origins;
    }

    @Override
    public void registerStompEndpoints(
            StompEndpointRegistry registry) {

        registry.setErrorHandler(new SafeStompErrorHandler());
        registry
                .addEndpoint("/ws")
                .setAllowedOriginPatterns(origins.websocket().toArray(String[]::new));
    }

    @Override
    public void configureMessageBroker(
            MessageBrokerRegistry registry) {

        registry.enableSimpleBroker(
                "/topic",
                "/queue");

        registry.setApplicationDestinationPrefixes("/app");

        registry.setUserDestinationPrefix("/user");
    }

    @Override
    public void configureClientInboundChannel(
            ChannelRegistration registration) {

        registration.interceptors(authInterceptor);
    }
}
