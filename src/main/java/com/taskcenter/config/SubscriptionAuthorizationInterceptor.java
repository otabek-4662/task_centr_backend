package com.taskcenter.config;

import com.taskcenter.service.WorkspaceAuthorizationService;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;

import java.util.Map;

@RequiredArgsConstructor
public class SubscriptionAuthorizationInterceptor implements ChannelInterceptor {

    private final WorkspaceAuthorizationService workspaceAuthorizationService;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(message);
        if (StompCommand.SUBSCRIBE.equals(accessor.getCommand())) {
            String destination = accessor.getDestination();
            if (destination != null && destination.startsWith("/topic/board/")) {
                String workspaceId = destination.substring("/topic/board/".length());
                // Noto'g'ri format (bo'sh, path traversal) — default rad etish
                if (workspaceId.isEmpty() || workspaceId.contains("/") || workspaceId.contains(".")) {
                    throw new IllegalArgumentException("Invalid workspace subscription: " + destination);
                }
                Map<String, Object> sessionAttributes = accessor.getSessionAttributes();
                String userId = sessionAttributes != null ? (String) sessionAttributes.get("userId") : null;
                if (userId == null || !workspaceAuthorizationService.hasAccess(workspaceId, userId)) {
                    throw new IllegalArgumentException("User does not have access to workspace " + workspaceId);
                }
            }
        }
        return message;
    }
}