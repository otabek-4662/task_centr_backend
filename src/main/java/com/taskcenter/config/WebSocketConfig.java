package com.taskcenter.config;

import com.taskcenter.security.JwtTokenProvider;
import com.taskcenter.service.WorkspaceAuthorizationService;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final JwtTokenProvider jwtTokenProvider;
    private final WorkspaceAuthorizationService workspaceAuthorizationService;

    public WebSocketConfig(JwtTokenProvider jwtTokenProvider, WorkspaceAuthorizationService workspaceAuthorizationService) {
        this.jwtTokenProvider = jwtTokenProvider;
        this.workspaceAuthorizationService = workspaceAuthorizationService;
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        config.enableSimpleBroker("/topic");
        config.setApplicationDestinationPrefixes("/app");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")
                .addInterceptors(new WebSocketAuthInterceptor(jwtTokenProvider, workspaceAuthorizationService))
                .setAllowedOriginPatterns("*")
                .withSockJS();
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(new SubscriptionAuthorizationInterceptor(workspaceAuthorizationService));
    }
}