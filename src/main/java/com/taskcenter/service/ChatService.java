package com.taskcenter.service;

import com.taskcenter.dto.*;
import com.taskcenter.exception.BadRequestException;
import com.taskcenter.exception.ForbiddenException;
import com.taskcenter.exception.ResourceNotFoundException;
import com.taskcenter.model.ChatMessage;
import com.taskcenter.model.ChatMessageType;
import com.taskcenter.model.MessageReadStatus;
import com.taskcenter.model.User;
import com.taskcenter.repository.ChatMessageRepository;
import com.taskcenter.repository.MessageReadStatusRepository;
import com.taskcenter.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ChatService {

    private final ChatMessageRepository chatMessageRepository;
    private final MessageReadStatusRepository readStatusRepository;
    private final UserRepository userRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final ChatPresenceService presenceService;

    public ChatService(ChatMessageRepository chatMessageRepository,
                       MessageReadStatusRepository readStatusRepository,
                       UserRepository userRepository,
                       SimpMessagingTemplate messagingTemplate,
                       ChatPresenceService presenceService) {
        this.chatMessageRepository = chatMessageRepository;
        this.readStatusRepository = readStatusRepository;
        this.userRepository = userRepository;
        this.messagingTemplate = messagingTemplate;
        this.presenceService = presenceService;
    }

    // ==================== XABAR YUBORISH ====================

    @Transactional
    public ChatMessageDto sendPublicMessage(String senderId, SendPublicMessageRequest req) {
        User sender = getUser(senderId);

        ChatMessage message = ChatMessage.builder()
                .senderId(sender.getId())
                .sender(sender)
                .content(req.getContent().trim())
                .type(ChatMessageType.PUBLIC)
                .replyToId(req.getReplyToId())
                .build();

        // reply_to_id borligini tekshirish
        if (req.getReplyToId() != null) {
            ChatMessage replyTo = getMessage(req.getReplyToId());
            message.setReplyTo(replyTo);
        }

        ChatMessage saved = chatMessageRepository.save(message);
        ChatMessageDto dto = toDto(saved);

        messagingTemplate.convertAndSend("/topic/public-chat", dto);

        return dto;
    }

    @Transactional
    public ChatMessageDto sendDirectMessage(String senderId, SendDirectMessageRequest req) {
        if (senderId.equals(req.getRecipientId())) {
            throw new BadRequestException("O'zingizga shaxsiy xabar yubora olmaysiz");
        }

        User sender = getUser(senderId);
        User recipient = getUser(req.getRecipientId());

        ChatMessage message = ChatMessage.builder()
                .senderId(sender.getId())
                .sender(sender)
                .recipientId(recipient.getId())
                .recipient(recipient)
                .content(req.getContent().trim())
                .type(ChatMessageType.DIRECT)
                .replyToId(req.getReplyToId())
                .build();

        if (req.getReplyToId() != null) {
            ChatMessage replyTo = getMessage(req.getReplyToId());
            message.setReplyTo(replyTo);
        }

        ChatMessage saved = chatMessageRepository.save(message);
        ChatMessageDto dto = toDto(saved);

        messagingTemplate.convertAndSendToUser(recipient.getUsername(), "/queue/private-chat", dto);
        messagingTemplate.convertAndSendToUser(sender.getUsername(), "/queue/private-chat", dto);

        return dto;
    }

    // ==================== XABARNI TAHRIRLASH ====================

    @Transactional
    public ChatMessageDto editMessage(String messageId, String currentUserId, EditMessageRequest req) {
        ChatMessage message = getMessage(messageId);

        // Faqat xabar egasi tahrirlashi mumkin
        if (!message.getSenderId().equals(currentUserId)) {
            throw new ForbiddenException("Faqat o'z xabaringizni tahrirlashingiz mumkin");
        }

        message.setContent(req.getContent().trim());
        message.setEditedAt(LocalDateTime.now());

        ChatMessage saved = chatMessageRepository.save(message);
        // EntityGraph bilan qayta yuklab olish (sender, recipient, replyTo)
        ChatMessage loaded = chatMessageRepository.findWithDetailsById(saved.getId())
                .orElse(saved);
        ChatMessageDto dto = toDto(loaded);

        // Real-time event yuborish
        ChatEvent<ChatMessageDto> event = ChatEvent.of("MESSAGE_EDITED", dto);

        if (message.getType() == ChatMessageType.PUBLIC) {
            messagingTemplate.convertAndSend("/topic/public-chat-events", event);
        } else {
            messagingTemplate.convertAndSendToUser(
                    getUser(message.getSenderId()).getUsername(), "/queue/chat-events", event);
            if (message.getRecipientId() != null) {
                messagingTemplate.convertAndSendToUser(
                        getUser(message.getRecipientId()).getUsername(), "/queue/chat-events", event);
            }
        }

        return dto;
    }

    // ==================== XABARNI O'CHIRISH ====================

    @Transactional
    public void deleteMessage(String messageId, String currentUserId) {
        ChatMessage message = getMessage(messageId);

        if (!message.getSenderId().equals(currentUserId)) {
            throw new ForbiddenException("Faqat o'z xabaringizni o'chirishingiz mumkin");
        }

        // Soft delete (SQLDelete annotatsiyasi ishlaydi)
        chatMessageRepository.delete(message);

        // Real-time event
        Map<String, String> deletePayload = Map.of("messageId", messageId);
        ChatEvent<Map<String, String>> event = ChatEvent.of("MESSAGE_DELETED", deletePayload);

        if (message.getType() == ChatMessageType.PUBLIC) {
            messagingTemplate.convertAndSend("/topic/public-chat-events", event);
        } else {
            messagingTemplate.convertAndSendToUser(
                    getUser(message.getSenderId()).getUsername(), "/queue/chat-events", event);
            if (message.getRecipientId() != null) {
                messagingTemplate.convertAndSendToUser(
                        getUser(message.getRecipientId()).getUsername(), "/queue/chat-events", event);
            }
        }
    }

    // ==================== XABARLARNI O'QISH ====================

    @Transactional(readOnly = true)
    public Page<ChatMessageDto> getPublicMessages(Pageable pageable) {
        return chatMessageRepository.findByTypeOrderByCreatedAtDesc(ChatMessageType.PUBLIC, pageable)
                .map(this::toDto);
    }

    @Transactional(readOnly = true)
    public Page<ChatMessageDto> getDirectMessages(String currentUserId, String otherUserId, Pageable pageable) {
        getUser(otherUserId);
        return chatMessageRepository.findDirectMessages(currentUserId, otherUserId, ChatMessageType.DIRECT, pageable)
                .map(this::toDto);
    }

    // ==================== O'QILGANLIK BELGISI (READ RECEIPTS) ====================

    @Transactional
    public void markAsRead(String messageId, String userId) {
        ChatMessage message = getMessage(messageId);

        // O'zining xabarini "o'qigan" deb belgilashga hojat yo'q
        if (message.getSenderId().equals(userId)) {
            return;
        }

        if (!readStatusRepository.existsByMessageIdAndUserId(messageId, userId)) {
            MessageReadStatus status = MessageReadStatus.builder()
                    .messageId(messageId)
                    .userId(userId)
                    .build();
            readStatusRepository.save(status);

            // Yuboruvchiga "o'qildi" eventini jo'natish
            User sender = getUser(message.getSenderId());
            Map<String, String> readPayload = Map.of(
                    "messageId", messageId,
                    "readBy", userId
            );
            messagingTemplate.convertAndSendToUser(
                    sender.getUsername(), "/queue/chat-events",
                    ChatEvent.of("MESSAGE_READ", readPayload));
        }
    }

    @Transactional
    public int markAllAsRead(String currentUserId, String otherUserId) {
        int count = readStatusRepository.markAllAsRead(currentUserId, otherUserId);

        if (count > 0) {
            // Xabar yuboruvchiga "barchasi o'qildi" eventini jo'natish
            User otherUser = getUser(otherUserId);
            Map<String, String> readPayload = Map.of(
                    "readBy", currentUserId,
                    "conversationWith", otherUserId
            );
            messagingTemplate.convertAndSendToUser(
                    otherUser.getUsername(), "/queue/chat-events",
                    ChatEvent.of("ALL_MESSAGES_READ", readPayload));
        }

        return count;
    }

    // ==================== SUHBATLAR RO'YXATI ====================

    @Transactional(readOnly = true)
    public List<ConversationDto> getConversations(String currentUserId) {
        List<ChatMessage> allMessages = chatMessageRepository
                .findAllDirectMessagesForUser(currentUserId);

        // Har bir suhbatdosh bilan faqat ENG OXIRGI xabarni olish
        // LinkedHashMap tartibni saqlaydi (createdAt DESC bo'yicha allaqachon saralangan)
        Map<String, ChatMessage> latestPerPartner = new LinkedHashMap<>();
        for (ChatMessage msg : allMessages) {
            String partnerId = msg.getSenderId().equals(currentUserId)
                    ? msg.getRecipientId()
                    : msg.getSenderId();
            latestPerPartner.putIfAbsent(partnerId, msg); // faqat birinchi (eng yangi) ni olish
        }

        return latestPerPartner.values().stream().map(msg -> {
            String otherUserId = msg.getSenderId().equals(currentUserId)
                    ? msg.getRecipientId()
                    : msg.getSenderId();
            User otherUser = msg.getSenderId().equals(currentUserId)
                    ? msg.getRecipient()
                    : msg.getSender();

            if (otherUser == null) {
                otherUser = getUser(otherUserId);
            }

            long unread = chatMessageRepository.countUnreadDirectMessages(currentUserId, otherUserId);

            return ConversationDto.builder()
                    .user(toUserDto(otherUser))
                    .lastMessage(toDto(msg))
                    .unreadCount(unread)
                    .lastMessageAt(msg.getCreatedAt())
                    .build();
        }).collect(Collectors.toList());
    }

    // ==================== FOYDALANUVCHILAR RO'YXATI ====================

    @Transactional(readOnly = true)
    public List<ChatUserDto> getChatUsers(String currentUserId) {
        return userRepository.findAll().stream()
                .filter(u -> !u.getId().equals(currentUserId))
                .map(this::toUserDto)
                .collect(Collectors.toList());
    }

    // ==================== QIDIRUV ====================

    @Transactional(readOnly = true)
    public Page<ChatMessageDto> searchPublicMessages(String query, Pageable pageable) {
        return chatMessageRepository.searchPublicMessages(query, ChatMessageType.PUBLIC, pageable)
                .map(this::toDto);
    }

    @Transactional(readOnly = true)
    public Page<ChatMessageDto> searchDirectMessages(String currentUserId, String otherUserId,
                                                      String query, Pageable pageable) {
        getUser(otherUserId);
        return chatMessageRepository.searchDirectMessages(
                currentUserId, otherUserId, query, ChatMessageType.DIRECT, pageable)
                .map(this::toDto);
    }

    // ==================== TYPING INDICATOR ====================

    public void sendTypingEvent(String senderUsername, String recipientId, boolean isPublic) {
        Map<String, String> payload = Map.of(
                "username", senderUsername,
                "typing", "true"
        );
        ChatEvent<Map<String, String>> event = ChatEvent.of("TYPING", payload);

        if (isPublic) {
            messagingTemplate.convertAndSend("/topic/public-chat-events", event);
        } else if (recipientId != null) {
            User recipient = getUser(recipientId);
            messagingTemplate.convertAndSendToUser(
                    recipient.getUsername(), "/queue/chat-events", event);
        }
    }

    // ==================== ONLINE STATUS ====================

    @Transactional
    public void updateLastSeen(String userId) {
        userRepository.findById(userId).ifPresent(user -> {
            user.setLastSeenAt(LocalDateTime.now());
            userRepository.save(user);
        });
    }

    public void broadcastOnlineStatus(String username, boolean online) {
        Map<String, Object> payload = Map.of(
                "username", username,
                "online", online
        );
        messagingTemplate.convertAndSend("/topic/presence",
                ChatEvent.of("PRESENCE", payload));
    }

    // ==================== YORDAMCHI METODLAR ====================

    private User getUser(String userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Foydalanuvchi topilmadi: " + userId));
    }

    private ChatMessage getMessage(String messageId) {
        return chatMessageRepository.findById(messageId)
                .orElseThrow(() -> new ResourceNotFoundException("Xabar topilmadi: " + messageId));
    }

    private ChatUserDto toUserDto(User user) {
        return ChatUserDto.builder()
                .id(user.getId())
                .name(user.getName())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .online(presenceService.isOnline(user.getName()))
                .lastSeenAt(user.getLastSeenAt())
                .build();
    }

    private ChatMessageDto toDto(ChatMessage message) {
        ChatMessageDto.ChatMessageDtoBuilder builder = ChatMessageDto.builder()
                .id(message.getId())
                .senderId(message.getSenderId())
                .senderName(message.getSender() != null ? message.getSender().getName() : null)
                .senderFullName(message.getSender() != null ? message.getSender().getFullName() : null)
                .recipientId(message.getRecipientId())
                .recipientName(message.getRecipient() != null ? message.getRecipient().getName() : null)
                .content(message.getContent())
                .type(message.getType())
                .createdAt(message.getCreatedAt())
                .edited(message.getEditedAt() != null)
                .editedAt(message.getEditedAt());

        // Reply ma'lumotini qo'shish
        if (message.getReplyTo() != null) {
            ChatMessage reply = message.getReplyTo();
            String replyContent = reply.getContent();
            if (replyContent != null && replyContent.length() > 100) {
                replyContent = replyContent.substring(0, 100) + "...";
            }
            builder.replyTo(ChatMessageDto.ReplyInfo.builder()
                    .id(reply.getId())
                    .content(replyContent)
                    .senderName(reply.getSender() != null ? reply.getSender().getName() : null)
                    .build());
        }

        // Read count
        try {
            long readCount = readStatusRepository.countByMessageId(message.getId());
            builder.readCount((int) readCount);
        } catch (Exception e) {
            builder.readCount(0);
        }

        return builder.build();
    }
}
