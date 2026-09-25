package com.taskcenter.service;

import com.taskcenter.model.User;
import com.taskcenter.model.UserPresence;
import com.taskcenter.repository.UserPresenceRepository;
import com.taskcenter.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

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

    @Transactional
    public void userConnected(String username) {
        userRepository.findByName(username).ifPresent(user -> {
            UserPresence presence = presenceRepository.findById(user.getId())
                    .orElse(UserPresence.builder().userId(user.getId()).user(user).build());
            presence.setOnline(true);
            presence.setLastSeenAt(LocalDateTime.now());
            presenceRepository.save(presence);
        });
    }

    @Transactional
    public void userDisconnected(String username) {
        userRepository.findByName(username).ifPresent(user -> {
            presenceRepository.findById(user.getId()).ifPresent(presence -> {
                presence.setOnline(false);
                presence.setLastSeenAt(LocalDateTime.now());
                presenceRepository.save(presence);
            });
        });
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
}
