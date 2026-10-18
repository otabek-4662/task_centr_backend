package com.taskcenter.service;

import com.taskcenter.dto.WebSocketEvent;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Service
public class WebSocketNotifier {

    private final SimpMessagingTemplate messagingTemplate;
    private final com.taskcenter.repository.UserRepository userRepository;

    public WebSocketNotifier(SimpMessagingTemplate messagingTemplate) {
        this(messagingTemplate, null);
    }

    public WebSocketNotifier(SimpMessagingTemplate messagingTemplate, com.taskcenter.repository.UserRepository userRepository) {
        this.messagingTemplate = messagingTemplate;
        this.userRepository = userRepository;
    }

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(WebSocketNotifier.class);

    @org.springframework.scheduling.annotation.Async
    public void notifyWorkspace(String workspaceId, WebSocketEvent<?> event) {
        try {
            messagingTemplate.convertAndSend("/topic/workspace/" + workspaceId, event);
        } catch (Exception e) {
            log.error("WebSocket notification failed for workspace {}: {}", workspaceId, e.getMessage());
        }
    }

    @org.springframework.scheduling.annotation.Async
    public void notifyUser(String userIdentifier, WebSocketEvent<?> event) {
        if (userIdentifier == null || userIdentifier.isBlank()) return;
        try {
            messagingTemplate.convertAndSendToUser(userIdentifier, "/queue/notifications", event);
            if (userRepository != null && userIdentifier.contains("-")) {
                userRepository.findById(userIdentifier).ifPresent(user -> {
                    if (user.getUsername() != null && !user.getUsername().equals(userIdentifier)) {
                        messagingTemplate.convertAndSendToUser(user.getUsername(), "/queue/notifications", event);
                    }
                });
            }
        } catch (Exception e) {
            log.error("WebSocket user notification failed for user {}: {}", userIdentifier, e.getMessage());
        }
    }
}

