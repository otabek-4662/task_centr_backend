package com.taskcenter.service;

import com.taskcenter.dto.BoardEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class WebSocketEventPublisher {

    private final SimpMessagingTemplate messagingTemplate;
    private final ObjectMapper objectMapper;

    public void publishBoardEvent(String workspaceId, BoardEvent event) {
        String destination = "/topic/board/" + workspaceId;
        try {
            String payload = objectMapper.writeValueAsString(event);
            messagingTemplate.convertAndSend(destination, payload);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize board event", e);
        }
    }
}