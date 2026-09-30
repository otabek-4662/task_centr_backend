package com.taskcenter.service;

import com.taskcenter.dto.WebSocketEvent;
import com.taskcenter.model.Notification;
import com.taskcenter.model.User;
import com.taskcenter.repository.NotificationRepository;
import com.taskcenter.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Set;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private NotificationRepository notificationRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private WebSocketNotifier webSocketNotifier;

    private NotificationService notificationService;

    @BeforeEach
    void setUp() {
        notificationService = new NotificationService(
                notificationRepository,
                userRepository,
                webSocketNotifier
        );
    }

    @Test
    void notifyWatchers_createsWebAndWebSocketNotifications() {
        // Arrange
        User watcher1 = User.builder().id("w1").telegramChatId(111L).build();
        User watcher2 = User.builder().id("w2").telegramChatId(222L).build();
        User actor = User.builder().id("actor").build();

        Set<User> watchers = Set.of(watcher1, watcher2, actor);

        List<Notification> mockSaved = List.of(
                Notification.builder().id("n1").userId("w1").title("T1").message("M1").build(),
                Notification.builder().id("n2").userId("w2").title("T2").message("M2").build()
        );
        when(notificationRepository.saveAll(anyList())).thenReturn(mockSaved);

        // Act
        notificationService.notifyWatchers(watchers, "actor", "Title", "Message", "ref-1");

        // Assert
        verify(notificationRepository).saveAll(anyList());
        verify(webSocketNotifier, times(2)).notifyUser(any(), any(WebSocketEvent.class));
    }
}
