package com.taskcenter.config;

import com.taskcenter.exception.ForbiddenException;
import com.taskcenter.exception.ResourceNotFoundException;
import com.taskcenter.model.User;
import com.taskcenter.security.JwtTokenProvider;
import com.taskcenter.service.WorkspaceAuthorizationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;

import java.security.Principal;

@Configuration
public class WebSocketAuthInterceptor implements ChannelInterceptor {

    private static final String WORKSPACE_TOPIC_PREFIX = "/topic/workspace/";
    private static final String WORKSPACE_APP_PREFIX = "/app/workspace/";

    private final JwtTokenProvider jwtTokenProvider;
    private final UserDetailsService userDetailsService;
    private final WorkspaceAuthorizationService workspaceAuthorizationService;

    public WebSocketAuthInterceptor(JwtTokenProvider jwtTokenProvider, UserDetailsService userDetailsService) {
        this(jwtTokenProvider, userDetailsService, null);
    }

    @Autowired
    public WebSocketAuthInterceptor(JwtTokenProvider jwtTokenProvider,
                                    UserDetailsService userDetailsService,
                                    @Autowired(required = false) WorkspaceAuthorizationService workspaceAuthorizationService) {
        this.jwtTokenProvider = jwtTokenProvider;
        this.userDetailsService = userDetailsService;
        this.workspaceAuthorizationService = workspaceAuthorizationService;
    }

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        if (accessor != null) {
            StompCommand cmd = accessor.getCommand();
            if (StompCommand.CONNECT.equals(cmd)) {
                String token = extractToken(accessor);

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
                Principal userPrincipal = accessor.getUser();
                if (userPrincipal == null) {
                    throw new IllegalArgumentException("Unauthenticated STOMP session");
                }
                validateWorkspaceDestinationAccess(accessor.getDestination(), userPrincipal);
            }
        }
        return message;
    }

    private String extractToken(StompHeaderAccessor accessor) {
        String authHeader = accessor.getFirstNativeHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }
        if (authHeader != null && !authHeader.isBlank()) {
            return authHeader.trim();
        }
        if (accessor.getFirstNativeHeader("token") != null) {
            return accessor.getFirstNativeHeader("token");
        }
        if (accessor.getPasscode() != null && !accessor.getPasscode().isBlank()) {
            String passcode = accessor.getPasscode();
            return passcode.startsWith("Bearer ") ? passcode.substring(7) : passcode;
        }
        return null;
    }

    private void validateWorkspaceDestinationAccess(String destination, Principal userPrincipal) {
        if (destination == null || workspaceAuthorizationService == null) {
            return;
        }
        String workspaceId = extractWorkspaceId(destination);
        if (workspaceId == null || workspaceId.isBlank()) {
            return;
        }

        User domainUser = resolveDomainUser(userPrincipal);
        if (domainUser == null) {
            return;
        }

        try {
            workspaceAuthorizationService.checkAccess(workspaceId, domainUser);
        } catch (ForbiddenException | ResourceNotFoundException ex) {
            throw new IllegalArgumentException("Workspace WebSocket kanaliga ruxsat yo'q: " + workspaceId);
        }
    }

    private String extractWorkspaceId(String destination) {
        String remainder = null;
        if (destination.startsWith(WORKSPACE_TOPIC_PREFIX)) {
            remainder = destination.substring(WORKSPACE_TOPIC_PREFIX.length());
        } else if (destination.startsWith(WORKSPACE_APP_PREFIX)) {
            remainder = destination.substring(WORKSPACE_APP_PREFIX.length());
        }
        if (remainder == null || remainder.isBlank()) {
            return null;
        }
        int slashIdx = remainder.indexOf('/');
        return slashIdx > 0 ? remainder.substring(0, slashIdx) : remainder;
    }

    private User resolveDomainUser(Principal principal) {
        if (principal instanceof Authentication auth && auth.getPrincipal() instanceof User user) {
            return user;
        }
        if (principal instanceof User user) {
            return user;
        }
        return null;
    }
}
