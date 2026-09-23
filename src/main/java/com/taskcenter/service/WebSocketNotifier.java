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

    public void notifyWorkspace(String workspaceId, WebSocketEvent<?> event) {
        messagingTemplate.convertAndSend("/topic/workspace/" + workspaceId, event);
    }
}
