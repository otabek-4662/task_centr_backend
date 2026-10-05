package com.taskcenter.service;

import com.taskcenter.model.User;
import com.taskcenter.model.UserPresence;
import com.taskcenter.repository.UserPresenceRepository;
import com.taskcenter.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.context.event.EventListener;
import org.springframework.boot.context.event.ApplicationReadyEvent;

/**
 * Online foydalanuvchilarni kuzatuvchi servis.
 * WebSocket session connect/disconnect paytida chaqiriladi.
 */
@Service
public class ChatPresenceService {

    private final UserPresenceRepository presenceRepository;
    private final UserRepository userRepository;

    public ChatPresenceService(UserPresenceRepository presenceRepository, UserRepository userRepository) {
        this.presenceRepository = presenceRepository;
        this.userRepository = userRepository;
    }

    private final ConcurrentHashMap<String, Integer> activeConnections = new ConcurrentHashMap<>();

    @Transactional
    public void userConnected(String username) {
        int count = activeConnections.merge(username, 1, Integer::sum);
        if (count == 1) {
            userRepository.findByName(username).ifPresent(user -> {
                UserPresence presence = presenceRepository.findById(user.getId())
                        .orElse(UserPresence.builder().userId(user.getId()).user(user).build());
                presence.setOnline(true);
                presence.setLastSeenAt(LocalDateTime.now());
                presenceRepository.save(presence);
            });
        }
    }

    @Transactional
    public void userDisconnected(String username) {
        Integer newCount = activeConnections.compute(username, (k, v) -> (v == null || v <= 1) ? null : v - 1);
        if (newCount == null) {
            userRepository.findByName(username).ifPresent(user -> {
                presenceRepository.findById(user.getId()).ifPresent(presence -> {
                    presence.setOnline(false);
                    presence.setLastSeenAt(LocalDateTime.now());
                    presenceRepository.save(presence);
                });
            });
        }
    }

    @Transactional(readOnly = true)
    public boolean isOnline(String username) {
        return userRepository.findByName(username)
                .flatMap(user -> presenceRepository.findById(user.getId()))
                .map(UserPresence::isOnline)
                .orElse(false);
    }

    @Transactional(readOnly = true)
    public java.util.Set<String> getOnlineUsers() {
        return new java.util.HashSet<>(presenceRepository.findOnlineUsernames());
    }

    // DIQQAT: Bu yondashuv faqat bitta JVM instance (single node) ishlaganda to'g'ri ishlaydi.
    // Agar dastur bir nechta instanceda (masalan Kubernetes) yugurayotgan bo'lsa,
    // bir instance ishga tushganda boshqa instancelardagi haqiqiy online userlarni ham offline qilib qo'yishi mumkin.
    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void resetAllOnlineStatus() {
        activeConnections.clear();
        for (UserPresence presence : presenceRepository.findAll()) {
            if (presence.isOnline()) {
                presence.setOnline(false);
                presenceRepository.save(presence);
            }
        }
    }
}
