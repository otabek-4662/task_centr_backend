package com.taskcenter.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final WebSocketAuthInterceptor authInterceptor;

    public WebSocketConfig(WebSocketAuthInterceptor authInterceptor) {
        this.authInterceptor = authInterceptor;
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        // Backend'dan Frontend'ga xabar jo'natish uchun prefix (Topic/Queue)
        config.enableSimpleBroker("/topic", "/queue");
        
        // Frontend'dan Backend'ga keladigan xabarlar uchun prefix
        config.setApplicationDestinationPrefixes("/app");
        
        // Maxsus user larga xabar jo'natish uchun
        config.setUserDestinationPrefix("/user");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // Frontend ulanadigan endpoint: ws://server/ws
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("*")
                .withSockJS(); // Fallback for browsers that don't support WebSocket
    }
    
    @Override
    public void configureClientInboundChannel(org.springframework.messaging.simp.config.ChannelRegistration registration) {
        registration.interceptors(authInterceptor);
    }
}
