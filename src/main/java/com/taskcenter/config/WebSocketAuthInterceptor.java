package com.taskcenter.config;

import com.taskcenter.security.JwtTokenProvider;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;

@Configuration
public class WebSocketAuthInterceptor implements ChannelInterceptor {

    private final JwtTokenProvider jwtTokenProvider;
    private final UserDetailsService userDetailsService;

    public WebSocketAuthInterceptor(JwtTokenProvider jwtTokenProvider, UserDetailsService userDetailsService) {
        this.jwtTokenProvider = jwtTokenProvider;
        this.userDetailsService = userDetailsService;
    }

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        
        if (accessor != null) {
            StompCommand cmd = accessor.getCommand();
            if (StompCommand.CONNECT.equals(cmd)) {
                String token = null;
                String authHeader = accessor.getFirstNativeHeader("Authorization");
                if (authHeader != null && authHeader.startsWith("Bearer ")) {
                    token = authHeader.substring(7);
                } else if (authHeader != null && !authHeader.isBlank()) {
                    token = authHeader.trim();
                } else if (accessor.getFirstNativeHeader("token") != null) {
                    token = accessor.getFirstNativeHeader("token");
                } else if (accessor.getPasscode() != null && !accessor.getPasscode().isBlank()) {
                    String passcode = accessor.getPasscode();
                    token = passcode.startsWith("Bearer ") ? passcode.substring(7) : passcode;
                }

                if (token != null && jwtTokenProvider.validateToken(token)) {
                    String username = jwtTokenProvider.getUserNameFromJWT(token);
                    UserDetails userDetails = userDetailsService.loadUserByUsername(username);
                    UsernamePasswordAuthenticationToken authentication = 
                            new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
                    
                    if (accessor.isMutable()) {
                        accessor.setUser(authentication);
                    } else {
                        StompHeaderAccessor mutableAccessor = StompHeaderAccessor.wrap(message);
                        mutableAccessor.setUser(authentication);
                        message = MessageBuilder.createMessage(message.getPayload(), mutableAccessor.getMessageHeaders());
                    }
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                } else {
                    throw new IllegalArgumentException("Invalid or missing authentication token");
                }
            } else if (StompCommand.SUBSCRIBE.equals(cmd) || StompCommand.SEND.equals(cmd)) {
                if (accessor.getUser() == null) {
                    throw new IllegalArgumentException("Unauthenticated STOMP session");
                }
            }
        }
        return message;
    }
}
