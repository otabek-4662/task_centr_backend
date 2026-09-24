package com.taskcenter.service;

import com.taskcenter.dto.*;
import com.taskcenter.exception.BadRequestException;
import com.taskcenter.exception.ForbiddenException;
import com.taskcenter.model.ChatMessage;
import com.taskcenter.model.ChatMessageType;
import com.taskcenter.model.User;
import com.taskcenter.repository.ChatMessageRepository;
import com.taskcenter.repository.MessageReadStatusRepository;
import com.taskcenter.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ChatServiceTest {

    @Mock
    private ChatMessageRepository chatMessageRepository;

    @Mock
    private MessageReadStatusRepository readStatusRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @Mock
    private ChatPresenceService presenceService;

    @InjectMocks
    private ChatService chatService;

    private User sender;
    private User recipient;

    @BeforeEach
    void setUp() {
        sender = User.builder()
                .id("user-1")
                .name("ali")
                .fullName("Ali Valiyev")
                .email("ali@example.com")
                .build();

        recipient = User.builder()
                .id("user-2")
                .name("vali")
                .fullName("Vali Aliyev")
                .email("vali@example.com")
                .build();
    }

    // ==================== XABAR YUBORISH ====================

    @Test
    @DisplayName("Umumiy chatga xabar yuborish va broadcast qilish")
    void testSendPublicMessage() {
        SendPublicMessageRequest request = new SendPublicMessageRequest("Hammaga salom!", null);

        when(userRepository.findById("user-1")).thenReturn(Optional.of(sender));
        when(readStatusRepository.countByMessageId(any())).thenReturn(0L);
        when(chatMessageRepository.save(any(ChatMessage.class))).thenAnswer(invocation -> {
            ChatMessage msg = invocation.getArgument(0);
            msg.setId("msg-1");
            msg.setCreatedAt(LocalDateTime.now());
            return msg;
        });

        ChatMessageDto result = chatService.sendPublicMessage("user-1", request);

        assertThat(result).isNotNull();
        assertThat(result.getContent()).isEqualTo("Hammaga salom!");
        assertThat(result.getType()).isEqualTo(ChatMessageType.PUBLIC);
        assertThat(result.getSenderName()).isEqualTo("ali");

        verify(messagingTemplate).convertAndSend(eq("/topic/public-chat"), any(ChatMessageDto.class));
    }

    @Test
    @DisplayName("Umumiy chatga reply bilan xabar yuborish")
    void testSendPublicMessageWithReply() {
        ChatMessage originalMsg = ChatMessage.builder()
                .id("original-1")
                .senderId("user-2")
                .sender(recipient)
                .content("Asl xabar matni")
                .type(ChatMessageType.PUBLIC)
                .createdAt(LocalDateTime.now())
                .build();

        SendPublicMessageRequest request = new SendPublicMessageRequest("Javobim shu!", "original-1");

        when(userRepository.findById("user-1")).thenReturn(Optional.of(sender));
        when(chatMessageRepository.findById("original-1")).thenReturn(Optional.of(originalMsg));
        when(readStatusRepository.countByMessageId(any())).thenReturn(0L);
        when(chatMessageRepository.save(any(ChatMessage.class))).thenAnswer(invocation -> {
            ChatMessage msg = invocation.getArgument(0);
            msg.setId("msg-reply-1");
            msg.setCreatedAt(LocalDateTime.now());
            return msg;
        });

        ChatMessageDto result = chatService.sendPublicMessage("user-1", request);

        assertThat(result).isNotNull();
        assertThat(result.getContent()).isEqualTo("Javobim shu!");
        assertThat(result.getReplyTo()).isNotNull();
        assertThat(result.getReplyTo().getContent()).isEqualTo("Asl xabar matni");
        assertThat(result.getReplyTo().getSenderName()).isEqualTo("vali");
    }

    @Test
    @DisplayName("Shaxsiy (DM) xabar yuborish")
    void testSendDirectMessage() {
        SendDirectMessageRequest request = new SendDirectMessageRequest("user-2", "Salom Vali!", null);

        when(userRepository.findById("user-1")).thenReturn(Optional.of(sender));
        when(userRepository.findById("user-2")).thenReturn(Optional.of(recipient));
        when(readStatusRepository.countByMessageId(any())).thenReturn(0L);
        when(chatMessageRepository.save(any(ChatMessage.class))).thenAnswer(invocation -> {
            ChatMessage msg = invocation.getArgument(0);
            msg.setId("msg-2");
            msg.setCreatedAt(LocalDateTime.now());
            return msg;
        });

        ChatMessageDto result = chatService.sendDirectMessage("user-1", request);

        assertThat(result).isNotNull();
        assertThat(result.getContent()).isEqualTo("Salom Vali!");
        assertThat(result.getType()).isEqualTo(ChatMessageType.DIRECT);
        assertThat(result.getRecipientId()).isEqualTo("user-2");

        verify(messagingTemplate).convertAndSendToUser(eq("vali"), eq("/queue/private-chat"), any(ChatMessageDto.class));
        verify(messagingTemplate).convertAndSendToUser(eq("ali"), eq("/queue/private-chat"), any(ChatMessageDto.class));
    }

    @Test
    @DisplayName("O'ziga DM yuborilganda xatolik berishi kerak")
    void testSendDirectMessageToSelf() {
        SendDirectMessageRequest request = new SendDirectMessageRequest("user-1", "Salom o'zimga!", null);

        assertThatThrownBy(() -> chatService.sendDirectMessage("user-1", request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("O'zingizga shaxsiy xabar yubora olmaysiz");
    }

    // ==================== TAHRIRLASH ====================

    @Test
    @DisplayName("Xabarni tahrirlash — faqat ega tahrirlashi mumkin")
    void testEditMessage() {
        ChatMessage message = ChatMessage.builder()
                .id("msg-1")
                .senderId("user-1")
                .sender(sender)
                .content("Eski matn")
                .type(ChatMessageType.PUBLIC)
                .createdAt(LocalDateTime.now())
                .build();

        when(chatMessageRepository.findById("msg-1")).thenReturn(Optional.of(message));
        when(readStatusRepository.countByMessageId(any())).thenReturn(0L);
        when(chatMessageRepository.save(any(ChatMessage.class))).thenAnswer(inv -> inv.getArgument(0));
        when(chatMessageRepository.findWithDetailsById("msg-1")).thenReturn(Optional.of(message));

        ChatMessageDto result = chatService.editMessage("msg-1", "user-1",
                new EditMessageRequest("Yangi matn"));

        assertThat(result.getContent()).isEqualTo("Yangi matn");
        assertThat(result.isEdited()).isTrue();
        verify(messagingTemplate).convertAndSend(eq("/topic/public-chat-events"), any(ChatEvent.class));
    }

    @Test
    @DisplayName("Boshqaning xabarini tahrirlash taqiqlangan")
    void testEditMessageForbidden() {
        ChatMessage message = ChatMessage.builder()
                .id("msg-1")
                .senderId("user-2")
                .content("Valining xabari")
                .type(ChatMessageType.PUBLIC)
                .createdAt(LocalDateTime.now())
                .build();

        when(chatMessageRepository.findById("msg-1")).thenReturn(Optional.of(message));

        assertThatThrownBy(() -> chatService.editMessage("msg-1", "user-1",
                new EditMessageRequest("Hack!")))
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("Faqat o'z xabaringizni tahrirlashingiz mumkin");
    }

    // ==================== O'CHIRISH ====================

    @Test
    @DisplayName("O'z xabarini o'chirish")
    void testDeleteMessage() {
        ChatMessage message = ChatMessage.builder()
                .id("msg-1")
                .senderId("user-1")
                .sender(sender)
                .content("O'chiriladi")
                .type(ChatMessageType.PUBLIC)
                .createdAt(LocalDateTime.now())
                .build();

        when(chatMessageRepository.findById("msg-1")).thenReturn(Optional.of(message));

        chatService.deleteMessage("msg-1", "user-1");

        verify(chatMessageRepository).delete(message);
        verify(messagingTemplate).convertAndSend(eq("/topic/public-chat-events"), any(ChatEvent.class));
    }

    @Test
    @DisplayName("Boshqaning xabarini o'chirish taqiqlangan")
    void testDeleteMessageForbidden() {
        ChatMessage message = ChatMessage.builder()
                .id("msg-1")
                .senderId("user-2")
                .content("Valining xabari")
                .build();

        when(chatMessageRepository.findById("msg-1")).thenReturn(Optional.of(message));

        assertThatThrownBy(() -> chatService.deleteMessage("msg-1", "user-1"))
                .isInstanceOf(ForbiddenException.class);
    }

    // ==================== RO'YXAT ====================

    @Test
    @DisplayName("Umumiy chat xabarlarini sahifalab olish")
    void testGetPublicMessages() {
        Pageable pageable = PageRequest.of(0, 10);
        ChatMessage msg = ChatMessage.builder()
                .id("msg-1")
                .senderId("user-1")
                .sender(sender)
                .content("Xabar 1")
                .type(ChatMessageType.PUBLIC)
                .createdAt(LocalDateTime.now())
                .build();

        when(chatMessageRepository.findByTypeOrderByCreatedAtDesc(ChatMessageType.PUBLIC, pageable))
                .thenReturn(new PageImpl<>(List.of(msg)));
        when(readStatusRepository.countByMessageId("msg-1")).thenReturn(0L);

        Page<ChatMessageDto> page = chatService.getPublicMessages(pageable);

        assertThat(page.getContent()).hasSize(1);
        assertThat(page.getContent().get(0).getContent()).isEqualTo("Xabar 1");
    }

    // ==================== FOYDALANUVCHILAR ====================

    @Test
    @DisplayName("Chat foydalanuvchilarini olish (o'zidan tashqari, online holati bilan)")
    void testGetChatUsers() {
        when(userRepository.findAll()).thenReturn(List.of(sender, recipient));
        when(presenceService.isOnline("vali")).thenReturn(true);

        List<ChatUserDto> users = chatService.getChatUsers("user-1");

        assertThat(users).hasSize(1);
        assertThat(users.get(0).getId()).isEqualTo("user-2");
        assertThat(users.get(0).getName()).isEqualTo("vali");
        assertThat(users.get(0).isOnline()).isTrue();
    }

    // ==================== QIDIRUV ====================

    @Test
    @DisplayName("Umumiy chatdan xabar qidirish")
    void testSearchPublicMessages() {
        Pageable pageable = PageRequest.of(0, 10);
        ChatMessage msg = ChatMessage.builder()
                .id("msg-1")
                .senderId("user-1")
                .sender(sender)
                .content("Salom dunyo!")
                .type(ChatMessageType.PUBLIC)
                .createdAt(LocalDateTime.now())
                .build();

        when(chatMessageRepository.searchPublicMessages("salom", ChatMessageType.PUBLIC, pageable))
                .thenReturn(new PageImpl<>(List.of(msg)));
        when(readStatusRepository.countByMessageId("msg-1")).thenReturn(0L);

        Page<ChatMessageDto> results = chatService.searchPublicMessages("salom", pageable);

        assertThat(results.getContent()).hasSize(1);
        assertThat(results.getContent().get(0).getContent()).contains("Salom");
    }
}
