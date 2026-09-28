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

@Service
public class NotificationService {
    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final WebSocketNotifier webSocketNotifier;
    private final TelegramBotService telegramBotService;

    public NotificationService(NotificationRepository notificationRepository, UserRepository userRepository, WebSocketNotifier webSocketNotifier, @org.springframework.lang.Nullable TelegramBotService telegramBotService) {
        this.notificationRepository = notificationRepository;
        this.userRepository = userRepository;
        this.webSocketNotifier = webSocketNotifier;
        this.telegramBotService = telegramBotService;
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
                    
            if (recipient.getTelegramChatId() != null && telegramBotService != null) {
                telegramBotService.sendMessage(recipient.getTelegramChatId(), "🔔 " + saved.getTitle() + "\n" + saved.getMessage());
            }
        });
    }

    @Transactional
    public void notifyWatchers(java.util.Set<User> watchers, String actorId, String title, String message, String refId) {
        if (watchers == null) return;
        for (User watcher : watchers) {
            if (watcher.getId().equals(actorId)) continue;
            
            Notification notification = Notification.builder()
                    .userId(watcher.getId())
                    .title(title)
                    .message(message)
                    .type("WATCH")
                    .referenceId(refId)
                    .build();
            Notification saved = notificationRepository.save(notification);
            
            webSocketNotifier.notifyUser(watcher.getId(), WebSocketEvent.builder()
                    .type("NEW_NOTIFICATION")
                    .data(toDto(saved))
                    .build());
                    
            if (watcher.getTelegramChatId() != null && telegramBotService != null) {
                telegramBotService.sendMessage(watcher.getTelegramChatId(), "🔔 " + saved.getTitle() + "\n" + saved.getMessage());
            }
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

            if (user.getTelegramChatId() != null && telegramBotService != null) {
                telegramBotService.sendMessage(user.getTelegramChatId(), "🔔 " + saved.getTitle() + "\n" + saved.getMessage());
            }
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
