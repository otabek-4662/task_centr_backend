package com.taskcenter.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.taskcenter.dto.BoardEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class WebSocketEventPublisherTest {

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    private WebSocketEventPublisher publisher;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        publisher = new WebSocketEventPublisher(messagingTemplate, objectMapper);
    }

    @Test
    void publishBoardEvent_sendsJsonToWorkspaceTopic() throws Exception {
        publisher.publishBoardEvent("ws-1", new BoardEvent(BoardEvent.Type.TASK_CREATED, Map.of("id", "t-1")));

        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(messagingTemplate).convertAndSend(eq("/topic/board/ws-1"), captor.capture());

        var json = objectMapper.readTree(captor.getValue());
        assertThat(json.get("type").asText()).isEqualTo("TASK_CREATED");
        assertThat(json.get("data").get("id").asText()).isEqualTo("t-1");
    }

    @Test
    void publishBoardEvent_columnDeleted_targetsSameTopicFormat() {
        publisher.publishBoardEvent("ws-9", new BoardEvent(BoardEvent.Type.COLUMN_DELETED, Map.of("id", "c-1")));

        verify(messagingTemplate).convertAndSend(eq("/topic/board/ws-9"), (Object) org.mockito.ArgumentMatchers.anyString());
    }
}
