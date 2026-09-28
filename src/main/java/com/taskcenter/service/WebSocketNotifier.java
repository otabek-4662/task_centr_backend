package com.taskcenter.service;

import com.taskcenter.dto.WebSocketEvent;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Service
public class WebSocketNotifier {

    private final SimpMessagingTemplate messagingTemplate;

    public WebSocketNotifier(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
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
    public void notifyUser(String userId, WebSocketEvent<?> event) {
        try {
            messagingTemplate.convertAndSendToUser(userId, "/queue/notifications", event);
        } catch (Exception e) {
            log.error("WebSocket user notification failed for user {}: {}", userId, e.getMessage());
        }
    }
}
