package com.taskcenter.config;

import com.taskcenter.model.User;
import com.taskcenter.repository.UserRepository;
import com.taskcenter.service.ChatPresenceService;
import com.taskcenter.service.ChatService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

/**
 * WebSocket session hodisalarini tinglash: CONNECT va DISCONNECT.
 * Foydalanuvchi ulanganida "online", uzilganida "offline" holatini belgilaydi.
 */
@Component
public class WebSocketEventListener {

    private static final Logger log = LoggerFactory.getLogger(WebSocketEventListener.class);

    private final ChatPresenceService presenceService;
    private final ChatService chatService;
    private final UserRepository userRepository;

    public WebSocketEventListener(ChatPresenceService presenceService,
                                   ChatService chatService,
                                   UserRepository userRepository) {
        this.presenceService = presenceService;
        this.chatService = chatService;
        this.userRepository = userRepository;
    }

    @EventListener
    public void handleWebSocketConnectListener(SessionConnectedEvent event) {
        String username = extractUsername(event.getUser());
        if (username != null) {
            presenceService.userConnected(username);
            chatService.broadcastOnlineStatus(username, true);
            log.info("WebSocket ulandi: {}", username);
        }
    }

    @EventListener
    public void handleWebSocketDisconnectListener(SessionDisconnectEvent event) {
        String username = extractUsername(event.getUser());
        if (username != null) {
            presenceService.userDisconnected(username);

            // last_seen_at ni yangilash
            userRepository.findByName(username).ifPresent(user -> {
                chatService.updateLastSeen(user.getId());
            });

            chatService.broadcastOnlineStatus(username, false);
            log.info("WebSocket uzildi: {}", username);
        }
    }

    private String extractUsername(java.security.Principal principal) {
        if (principal == null) {
            return null;
        }
        if (principal instanceof UsernamePasswordAuthenticationToken auth) {
            Object userObj = auth.getPrincipal();
            if (userObj instanceof User user) {
                return user.getUsername();
            }
        }
        return principal.getName();
    }
}
