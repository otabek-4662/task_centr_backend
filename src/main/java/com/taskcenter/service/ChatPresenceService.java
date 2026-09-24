package com.taskcenter.service;

import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Online foydalanuvchilarni kuzatuvchi servis.
 * WebSocket session connect/disconnect paytida chaqiriladi.
 * Ma'lumot faqat xotirada (in-memory) saqlanadi — restart da tozalanadi.
 */
@Service
public class ChatPresenceService {

    // Thread-safe set: online foydalanuvchilar username lari
    private final Set<String> onlineUsers = ConcurrentHashMap.newKeySet();

    public void userConnected(String username) {
        onlineUsers.add(username);
    }

    public void userDisconnected(String username) {
        onlineUsers.remove(username);
    }

    public boolean isOnline(String username) {
        return onlineUsers.contains(username);
    }

    public Set<String> getOnlineUsers() {
        return Set.copyOf(onlineUsers);
    }
}
