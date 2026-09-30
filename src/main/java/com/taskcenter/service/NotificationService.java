package com.taskcenter.service;

import com.taskcenter.dto.NotificationDto;
import com.taskcenter.dto.WebSocketEvent;
import com.taskcenter.model.Notification;
import com.taskcenter.model.User;
import com.taskcenter.repository.NotificationRepository;
import com.taskcenter.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
public class NotificationService {
    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final WebSocketNotifier webSocketNotifier;

    public NotificationService(NotificationRepository notificationRepository, UserRepository userRepository, WebSocketNotifier webSocketNotifier) {
        this.notificationRepository = notificationRepository;
        this.userRepository = userRepository;
        this.webSocketNotifier = webSocketNotifier;
    }

    @Transactional(readOnly = true)
    public Page<NotificationDto> getUserNotifications(User currentUser, Pageable pageable) {
        return notificationRepository.findByUserId(currentUser.getId(), pageable).map(this::toDto);
    }

    @Transactional
    public void markAsRead(String id, User currentUser) {
        Notification notification = notificationRepository.findById(id).orElseThrow();
        if (notification.getUserId().equals(currentUser.getId())) {
            notification.setRead(true);
            notificationRepository.save(notification);
        }
    }

    @Transactional
    public void markAllAsRead(User currentUser) {
        notificationRepository.findByUserIdAndReadFalseOrderByCreatedAtDesc(currentUser.getId())
                .forEach(n -> {
                    n.setRead(true);
                    notificationRepository.save(n);
                });
    }

    @Transactional
    public void createMentionNotification(String actorName, String mentionedUsername, String message, String refId) {
        userRepository.findByName(mentionedUsername).ifPresent(recipient -> {
            Notification notification = Notification.builder()
                    .userId(recipient.getId())
                    .title("Sizni " + actorName + " eslab o'tdi")
                    .message(message)
                    .type("MENTION")
                    .referenceId(refId)
                    .build();
            Notification saved = notificationRepository.save(notification);
            
            webSocketNotifier.notifyUser(recipient.getId(), WebSocketEvent.builder()
                    .type("NEW_NOTIFICATION")
                    .data(toDto(saved))
                    .build());
        });
    }

    @Transactional
    public void notifyWatchers(java.util.Set<User> watchers, String actorId, String title, String message, String refId) {
        if (watchers == null) return;
        
        List<Notification> notifications = new ArrayList<>();
        List<User> targetWatchers = new ArrayList<>();
        
        for (User watcher : watchers) {
            if (watcher.getId().equals(actorId)) continue;
            
            Notification notification = Notification.builder()
                    .userId(watcher.getId())
                    .title(title)
                    .message(message)
                    .type("WATCH")
                    .referenceId(refId)
                    .build();
            notifications.add(notification);
            targetWatchers.add(watcher);
        }
        
        if (notifications.isEmpty()) return;
        
        List<Notification> savedList = notificationRepository.saveAll(notifications);
        
        for (int i = 0; i < savedList.size(); i++) {
            Notification saved = savedList.get(i);
            User watcher = targetWatchers.get(i);
            
            webSocketNotifier.notifyUser(watcher.getId(), WebSocketEvent.builder()
                    .type("NEW_NOTIFICATION")
                    .data(toDto(saved))
                    .build());
        }
    }

    @Transactional
    public void notifyUser(String userId, String title, String message) {
        userRepository.findById(userId).ifPresent(user -> {
            Notification notification = Notification.builder()
                    .userId(user.getId())
                    .title(title)
                    .message(message)
                    .type("SYSTEM")
                    .build();
            Notification saved = notificationRepository.save(notification);

            webSocketNotifier.notifyUser(user.getId(), WebSocketEvent.builder()
                    .type("NEW_NOTIFICATION")
                    .data(toDto(saved))
                    .build());
        });
    }

    private NotificationDto toDto(Notification n) {
        return NotificationDto.builder()
                .id(n.getId())
                .title(n.getTitle())
                .message(n.getMessage())
                .read(n.isRead())
                .type(n.getType())
                .referenceId(n.getReferenceId())
                .createdAt(n.getCreatedAt())
                .build();
    }
}
