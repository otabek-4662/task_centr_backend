package com.taskcenter.controller;

import com.taskcenter.dto.*;
import com.taskcenter.model.User;
import com.taskcenter.service.ChatPresenceService;
import com.taskcenter.service.ChatService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springdoc.core.annotations.ParameterObject;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;

@Tag(name = "Chat", description = "Telegram uslubidagi umumiy va shaxsiy (DM) chat API lari")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/chat")
public class ChatController {

    private final ChatService chatService;
    private final ChatPresenceService presenceService;

    public ChatController(ChatService chatService, ChatPresenceService presenceService) {
        this.chatService = chatService;
        this.presenceService = presenceService;
    }

    // ==================== XABAR YUBORISH ====================

    @Operation(
            operationId = "sendPublicChatMessage",
            summary = "Umumiy chatga xabar yuborish",
            description = "Barcha tizim foydalanuvchilari uchun ochiq bo'lgan umumiy chatga matn yoki fayl biriktirmasi bilan xabar yuboradi."
    )
    @PostMapping("/public")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<ChatMessageDto> sendPublicMessage(
            @Valid @RequestBody SendPublicMessageRequest request,
            @AuthenticationPrincipal User currentUser) {
        ChatMessageDto message = chatService.sendPublicMessage(currentUser.getId(), request);
        return ApiResponse.success("Xabar muvaffaqiyatli yuborildi", message);
    }

    @Operation(
            operationId = "sendDirectChatMessage",
            summary = "Shaxsiy (DM) xabar yuborish",
            description = "Aniq bir foydalanuvchiga (recipientId) shaxsiy yopiq xabar yuboradi."
    )
    @PostMapping("/direct")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<ChatMessageDto> sendDirectMessage(
            @Valid @RequestBody SendDirectMessageRequest request,
            @AuthenticationPrincipal User currentUser) {
        ChatMessageDto message = chatService.sendDirectMessage(currentUser.getId(), request);
        return ApiResponse.success("Shaxsiy xabar muvaffaqiyatli yuborildi", message);
    }

    // ==================== XABAR TAHRIRLASH / O'CHIRISH ====================

    @Operation(
            operationId = "editChatMessage",
            summary = "Xabarni tahrirlash",
            description = "Faqat xabar yuboruvchisi o'z xabarining matnini tahrirlashi mumkin."
    )
    @PutMapping("/messages/{messageId}")
    public ApiResponse<ChatMessageDto> editMessage(
            @io.swagger.v3.oas.annotations.Parameter(description = "Xabar ID si", example = "msg-uuid-123")
            @PathVariable String messageId,
            @Valid @RequestBody EditMessageRequest request,
            @AuthenticationPrincipal User currentUser) {
        ChatMessageDto message = chatService.editMessage(messageId, currentUser.getId(), request);
        return ApiResponse.success("Xabar muvaffaqiyatli tahrirlandi", message);
    }

    @Operation(
            operationId = "deleteChatMessage",
            summary = "Xabarni o'chirish (hammadan)",
            description = "Faqat xabar yuboruvchisi o'z xabarini butunlay o'chirishi mumkin."
    )
    @DeleteMapping("/messages/{messageId}")
    public ApiResponse<Void> deleteMessage(
            @io.swagger.v3.oas.annotations.Parameter(description = "Xabar ID si", example = "msg-uuid-123")
            @PathVariable String messageId,
            @AuthenticationPrincipal User currentUser) {
        chatService.deleteMessage(messageId, currentUser.getId());
        return ApiResponse.success("Xabar muvaffaqiyatli o'chirildi", null);
    }

    // ==================== XABARLAR TARIXINI O'QISH ====================

    @Operation(
            operationId = "getPublicChatMessages",
            summary = "Umumiy chat xabarlarini sahifalab olish",
            description = "Umumiy chatdagi xabarlar tarixini sahifalangan holda (eng yangilari birinchi) qaytaradi."
    )
    @GetMapping("/public")
    public ApiResponse<Page<ChatMessageDto>> getPublicMessages(
            @ParameterObject @PageableDefault(size = 30, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        Page<ChatMessageDto> messages = chatService.getPublicMessages(pageable);
        return ApiResponse.success("ok", messages);
    }

    @Operation(
            operationId = "getDirectChatMessages",
            summary = "Foydalanuvchi bilan shaxsiy yozishmalar tarixini olish",
            description = "Joriy foydalanuvchi va boshqa foydalanuvchi (otherUserId) o'rtasidagi shaxsiy yozishmalar tarixini sahifalab qaytaradi."
    )
    @GetMapping("/direct/{otherUserId}")
    public ApiResponse<Page<ChatMessageDto>> getDirectMessages(
            @io.swagger.v3.oas.annotations.Parameter(description = "Suhbatdosh foydalanuvchi ID si", example = "user-uuid-123")
            @PathVariable String otherUserId,
            @ParameterObject @PageableDefault(size = 30, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable,
            @AuthenticationPrincipal User currentUser) {
        Page<ChatMessageDto> messages = chatService.getDirectMessages(currentUser.getId(), otherUserId, pageable);
        return ApiResponse.success("ok", messages);
    }

    // ==================== O'QILGANLIK (READ RECEIPTS) ====================

    @Operation(
            operationId = "markChatMessageAsRead",
            summary = "Xabarni o'qilgan deb belgilash",
            description = "Muayyan xabarni joriy foydalanuvchi tomonidan o'qildi (isRead = true) holatiga o'tkazadi."
    )
    @PostMapping("/messages/{messageId}/read")
    public ApiResponse<Void> markAsRead(
            @io.swagger.v3.oas.annotations.Parameter(description = "Xabar ID si", example = "msg-uuid-123")
            @PathVariable String messageId,
            @AuthenticationPrincipal User currentUser) {
        chatService.markAsRead(messageId, currentUser.getId());
        return ApiResponse.success("O'qildi deb belgilandi", null);
    }

    @Operation(
            operationId = "markAllDirectChatMessagesAsRead",
            summary = "Suhbatdagi barcha xabarlarni o'qilgan deb belgilash",
            description = "Boshqa foydalanuvchi (otherUserId) tomonidan yuborilgan barcha o'qilmagan xabarlarni o'qilgan qiladi va o'qilganlar sonini qaytaradi."
    )
    @PostMapping("/direct/{otherUserId}/read-all")
    public ApiResponse<Integer> markAllAsRead(
            @io.swagger.v3.oas.annotations.Parameter(description = "Suhbatdosh foydalanuvchi ID si", example = "user-uuid-123")
            @PathVariable String otherUserId,
            @AuthenticationPrincipal User currentUser) {
        int count = chatService.markAllAsRead(currentUser.getId(), otherUserId);
        return ApiResponse.success(count + " ta xabar o'qildi deb belgilandi", count);
    }

    // ==================== SUHBATLAR RO'YXATI ====================

    @Operation(
            operationId = "getDirectChatConversations",
            summary = "DM suhbatlar ro'yxati (oxirgi xabar + unread count)",
            description = "Foydalanuvchi qatnashgan barcha shaxsiy suhbatlar ro'yxati, ularning oxirgi xabari va o'qilmagan xabarlar sonini qaytaradi."
    )
    @GetMapping("/conversations")
    public ApiResponse<List<ConversationDto>> getConversations(
            @AuthenticationPrincipal User currentUser) {
        List<ConversationDto> conversations = chatService.getConversations(currentUser.getId());
        return ApiResponse.success("ok", conversations);
    }

    // ==================== FOYDALANUVCHILAR ====================

    @Operation(
            operationId = "getChatUsers",
            summary = "Chat uchun foydalanuvchilar ro'yxati (online holati bilan)",
            description = "Yangi suhbat boshlash uchun tizimdagi barcha foydalanuvchilar va ularning online/offline holatini qaytaradi."
    )
    @GetMapping("/users")
    public ApiResponse<List<ChatUserDto>> getChatUsers(@AuthenticationPrincipal User currentUser) {
        List<ChatUserDto> users = chatService.getChatUsers(currentUser.getId());
        return ApiResponse.success("ok", users);
    }

    @Operation(
            operationId = "getOnlineChatUsers",
            summary = "Hozir online foydalanuvchilar ro'yxati",
            description = "Ayni vaqtda WebSocket yoki tizimda faol (online) bo'lgan foydalanuvchilar ID lari to'plamini beradi."
    )
    @GetMapping("/online")
    public ApiResponse<Set<String>> getOnlineUsers() {
        Set<String> online = presenceService.getOnlineUsers();
        return ApiResponse.success("ok", online);
    }

    // ==================== REACTION ====================

    @Operation(
            operationId = "toggleChatMessageReaction",
            summary = "Xabarga reaksiya qo'shish yoki olib tashlash (toggle)",
            description = "Xabarga emoji reaksiyasini qo'yadi. Agar foydalanuvchi ushbu reaksiyani avval bosgan bo'lsa, olib tashlanadi."
    )
    @PostMapping("/{messageId}/reactions")
    public ApiResponse<ChatMessageDto> toggleReaction(
            @io.swagger.v3.oas.annotations.Parameter(description = "Xabar ID si", example = "msg-uuid-123")
            @PathVariable String messageId,
            @Valid @RequestBody ToggleReactionRequest request,
            @AuthenticationPrincipal User currentUser) {
        ChatMessageDto dto = chatService.toggleReaction(messageId, currentUser.getId(), request.getEmoji());
        return ApiResponse.success("ok", dto);
    }

    // ==================== QIDIRUV ====================

    @Operation(
            operationId = "searchAllChatMessages",
            summary = "Barcha ko'rinadigan xabarlar orasidan qidirish (Public + DM)",
            description = "Foydalanuvchiga ko'rinadigan barcha ommaviy va shaxsiy xabarlar bo'yicha matnli qidiruvni amalga oshiradi."
    )
    @GetMapping("/search")
    public ApiResponse<Page<ChatMessageDto>> searchAllMessages(
            @io.swagger.v3.oas.annotations.Parameter(description = "Qidiruv matni", example = "deploy")
            @RequestParam String query,
            @ParameterObject @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable,
            @AuthenticationPrincipal User currentUser) {
        Page<ChatMessageDto> results = chatService.searchAllMessages(currentUser.getId(), query, pageable);
        return ApiResponse.success("ok", results);
    }

    @Operation(
            operationId = "searchPublicChatMessages",
            summary = "Umumiy chatdan xabar qidirish",
            description = "Faqat umumiy chat xabarlari ichidan kalit so'z (q) bo'yicha qidiradi."
    )
    @GetMapping("/public/search")
    public ApiResponse<Page<ChatMessageDto>> searchPublicMessages(
            @io.swagger.v3.oas.annotations.Parameter(description = "Qidiruv so'zi", example = "yangilik")
            @RequestParam String q,
            @ParameterObject @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        Page<ChatMessageDto> results = chatService.searchPublicMessages(q, pageable);
        return ApiResponse.success("ok", results);
    }

    @Operation(
            operationId = "searchDirectChatMessages",
            summary = "DM suhbatdan xabar qidirish",
            description = "Muayyan foydalanuvchi bilan bo'lgan shaxsiy yozishmalar orasidan qidiruv qiladi."
    )
    @GetMapping("/direct/{otherUserId}/search")
    public ApiResponse<Page<ChatMessageDto>> searchDirectMessages(
            @io.swagger.v3.oas.annotations.Parameter(description = "Suhbatdosh foydalanuvchi ID si", example = "user-uuid-123")
            @PathVariable String otherUserId,
            @io.swagger.v3.oas.annotations.Parameter(description = "Qidiruv so'zi", example = "topshiriq")
            @RequestParam String q,
            @ParameterObject @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable,
            @AuthenticationPrincipal User currentUser) {
        Page<ChatMessageDto> results = chatService.searchDirectMessages(
                currentUser.getId(), otherUserId, q, pageable);
        return ApiResponse.success("ok", results);
    }
}
