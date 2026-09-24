package com.taskcenter.controller;

import com.taskcenter.dto.EditMessageRequest;
import com.taskcenter.dto.SendDirectMessageRequest;
import com.taskcenter.dto.SendPublicMessageRequest;
import com.taskcenter.exception.ResourceNotFoundException;
import com.taskcenter.model.User;
import com.taskcenter.repository.UserRepository;
import com.taskcenter.service.ChatService;
import jakarta.validation.Valid;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Controller;

import org.springframework.messaging.handler.annotation.MessageExceptionHandler;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.handler.annotation.support.MethodArgumentNotValidException;

import java.security.Principal;
import java.util.Map;

@Controller
public class ChatWebSocketController {

    private final ChatService chatService;
    private final UserRepository userRepository;
    private final SimpMessagingTemplate messagingTemplate;

    public ChatWebSocketController(ChatService chatService, UserRepository userRepository, SimpMessagingTemplate messagingTemplate) {
        this.chatService = chatService;
        this.userRepository = userRepository;
        this.messagingTemplate = messagingTemplate;
    }

    /**
     * Umumiy chatga xabar yuborish
     * Frontend: client.publish({ destination: '/app/chat.public', body: ... })
     */
    @MessageMapping("/chat.public")
    public void sendPublicMessage(@Payload @Valid SendPublicMessageRequest request, Principal principal) {
        if (principal == null) return;
        User user = resolveUser(principal);
        chatService.sendPublicMessage(user.getId(), request);
    }

    /**
     * Shaxsiy (DM) xabar yuborish
     * Frontend: client.publish({ destination: '/app/chat.direct', body: ... })
     */
    @MessageMapping("/chat.direct")
    public void sendDirectMessage(@Payload @Valid SendDirectMessageRequest request, Principal principal) {
        if (principal == null) return;
        User user = resolveUser(principal);
        chatService.sendDirectMessage(user.getId(), request);
    }

    /**
     * Xabarni o'qilgan deb belgilash (read receipt)
     * Frontend: client.publish({ destination: '/app/chat.read', body: '{"messageId":"..."}' })
     */
    @MessageMapping("/chat.read")
    public void markAsRead(@Payload Map<String, String> payload, Principal principal) {
        if (principal == null) return;
        User user = resolveUser(principal);
        String messageId = payload.get("messageId");
        if (messageId != null) {
            chatService.markAsRead(messageId, user.getId());
        }
    }

    /**
     * "Yozmoqda..." (Typing indicator)
     * Frontend: client.publish({ destination: '/app/chat.typing', body: '{"recipientId":"...", "public": false}' })
     */
    @MessageMapping("/chat.typing")
    public void typing(@Payload Map<String, Object> payload, Principal principal) {
        if (principal == null) return;
        String recipientId = (String) payload.get("recipientId");
        Object isPublicObj = payload.get("public");
        boolean isPublic = Boolean.TRUE.equals(isPublicObj);
        chatService.sendTypingEvent(principal.getName(), recipientId, isPublic);
    }

    /**
     * Xabarni tahrirlash (WebSocket orqali - PUT o'rniga)
     * Frontend: client.publish({ destination: '/app/chat.edit', body: '{"messageId":"...", "content":"..."}' })
     */
    @MessageMapping("/chat.edit")
    public void editMessage(@Payload Map<String, String> payload, Principal principal) {
        if (principal == null) return;
        User user = resolveUser(principal);
        String messageId = payload.get("messageId");
        String content = payload.get("content");
        if (messageId != null && content != null) {
            EditMessageRequest req = new EditMessageRequest(content);
            chatService.editMessage(messageId, user.getId(), req);
        }
    }

    /**
     * Xabarni o'chirish (WebSocket orqali - DELETE o'rniga)
     * Frontend: client.publish({ destination: '/app/chat.delete', body: '{"messageId":"..."}' })
     */
    @MessageMapping("/chat.delete")
    public void deleteMessage(@Payload Map<String, String> payload, Principal principal) {
        if (principal == null) return;
        User user = resolveUser(principal);
        String messageId = payload.get("messageId");
        if (messageId != null) {
            chatService.deleteMessage(messageId, user.getId());
        }
    }

    private User resolveUser(Principal principal) {
        return userRepository.findByName(principal.getName())
                .orElseThrow(() -> new ResourceNotFoundException("Foydalanuvchi topilmadi: " + principal.getName()));
    }

    @MessageExceptionHandler
    public void handleValidationExceptions(MethodArgumentNotValidException ex, Principal principal) {
        if (principal == null) return;
        String errorMsg = "Xato ma'lumot jo'natildi";
        if (ex.getBindingResult().hasErrors()) {
            errorMsg = ex.getBindingResult().getAllErrors().get(0).getDefaultMessage();
        }
        messagingTemplate.convertAndSendToUser(principal.getName(), "/queue/errors", errorMsg);
    }
}
