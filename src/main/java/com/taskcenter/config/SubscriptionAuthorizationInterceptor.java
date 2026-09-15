package com.taskcenter.config;

import com.taskcenter.service.WorkspaceAuthorizationService;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.channel.ChannelInterceptor;
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
                Map<String, Object> sessionAttributes = accessor.getSessionAttributes();
                String userId = (String) sessionAttributes.get("userId");
                if (userId == null || !workspaceAuthorizationService.hasRole(workspaceId, userId)) {
                    throw new IllegalArgumentException("User does not have access to workspace " + workspaceId);
                }
            }
        }
        return message;
    }
}