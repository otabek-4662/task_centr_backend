package com.taskcenter.controller;

import com.taskcenter.dto.*;
import com.taskcenter.model.User;
import com.taskcenter.service.ChatPresenceService;
import com.taskcenter.service.ChatService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@CrossOrigin(origins = "*")
public class ChatController {

    private final ChatService chatService;
    private final ChatPresenceService presenceService;

    public ChatController(ChatService chatService, ChatPresenceService presenceService) {
        this.chatService = chatService;
        this.presenceService = presenceService;
    }

    // ==================== XABAR YUBORISH ====================

    @Operation(summary = "Umumiy chatga xabar yuborish")
    @PostMapping("/public")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<ChatMessageDto> sendPublicMessage(
            @Valid @RequestBody SendPublicMessageRequest request,
            @AuthenticationPrincipal User currentUser) {
        ChatMessageDto message = chatService.sendPublicMessage(currentUser.getId(), request);
        return ApiResponse.success("Xabar muvaffaqiyatli yuborildi", message);
    }

    @Operation(summary = "Shaxsiy (DM) xabar yuborish")
    @PostMapping("/direct")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<ChatMessageDto> sendDirectMessage(
            @Valid @RequestBody SendDirectMessageRequest request,
            @AuthenticationPrincipal User currentUser) {
        ChatMessageDto message = chatService.sendDirectMessage(currentUser.getId(), request);
        return ApiResponse.success("Shaxsiy xabar muvaffaqiyatli yuborildi", message);
    }

    // ==================== XABAR TAHRIRLASH / O'CHIRISH ====================

    @Operation(summary = "Xabarni tahrirlash")
    @PutMapping("/messages/{messageId}")
    public ApiResponse<ChatMessageDto> editMessage(
            @PathVariable String messageId,
            @Valid @RequestBody EditMessageRequest request,
            @AuthenticationPrincipal User currentUser) {
        ChatMessageDto message = chatService.editMessage(messageId, currentUser.getId(), request);
        return ApiResponse.success("Xabar muvaffaqiyatli tahrirlandi", message);
    }

    @Operation(summary = "Xabarni o'chirish (hammadan)")
    @DeleteMapping("/messages/{messageId}")
    public ApiResponse<Void> deleteMessage(
            @PathVariable String messageId,
            @AuthenticationPrincipal User currentUser) {
        chatService.deleteMessage(messageId, currentUser.getId());
        return ApiResponse.success("Xabar muvaffaqiyatli o'chirildi", null);
    }

    // ==================== XABARLAR TARIXINI O'QISH ====================

    @Operation(summary = "Umumiy chat xabarlarini sahifalab olish")
    @GetMapping("/public")
    public ApiResponse<Page<ChatMessageDto>> getPublicMessages(
            @PageableDefault(size = 30, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        Page<ChatMessageDto> messages = chatService.getPublicMessages(pageable);
        return ApiResponse.success("ok", messages);
    }

    @Operation(summary = "Foydalanuvchi bilan shaxsiy yozishmalar tarixini olish")
    @GetMapping("/direct/{otherUserId}")
    public ApiResponse<Page<ChatMessageDto>> getDirectMessages(
            @PathVariable String otherUserId,
            @PageableDefault(size = 30, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable,
            @AuthenticationPrincipal User currentUser) {
        Page<ChatMessageDto> messages = chatService.getDirectMessages(currentUser.getId(), otherUserId, pageable);
        return ApiResponse.success("ok", messages);
    }

    // ==================== O'QILGANLIK (READ RECEIPTS) ====================

    @Operation(summary = "Xabarni o'qilgan deb belgilash")
    @PostMapping("/messages/{messageId}/read")
    public ApiResponse<Void> markAsRead(
            @PathVariable String messageId,
            @AuthenticationPrincipal User currentUser) {
        chatService.markAsRead(messageId, currentUser.getId());
        return ApiResponse.success("O'qildi deb belgilandi", null);
    }

    @Operation(summary = "Suhbatdagi barcha xabarlarni o'qilgan deb belgilash")
    @PostMapping("/direct/{otherUserId}/read-all")
    public ApiResponse<Integer> markAllAsRead(
            @PathVariable String otherUserId,
            @AuthenticationPrincipal User currentUser) {
        int count = chatService.markAllAsRead(currentUser.getId(), otherUserId);
        return ApiResponse.success(count + " ta xabar o'qildi deb belgilandi", count);
    }

    // ==================== SUHBATLAR RO'YXATI ====================

    @Operation(summary = "DM suhbatlar ro'yxati (oxirgi xabar + unread count)")
    @GetMapping("/conversations")
    public ApiResponse<List<ConversationDto>> getConversations(
            @AuthenticationPrincipal User currentUser) {
        List<ConversationDto> conversations = chatService.getConversations(currentUser.getId());
        return ApiResponse.success("ok", conversations);
    }

    // ==================== FOYDALANUVCHILAR ====================

    @Operation(summary = "Chat uchun foydalanuvchilar ro'yxati (online holati bilan)")
    @GetMapping("/users")
    public ApiResponse<List<ChatUserDto>> getChatUsers(@AuthenticationPrincipal User currentUser) {
        List<ChatUserDto> users = chatService.getChatUsers(currentUser.getId());
        return ApiResponse.success("ok", users);
    }

    @Operation(summary = "Hozir online foydalanuvchilar ro'yxati")
    @GetMapping("/online")
    public ApiResponse<Set<String>> getOnlineUsers() {
        Set<String> online = presenceService.getOnlineUsers();
        return ApiResponse.success("ok", online);
    }

    // ==================== REACTION ====================

    @Operation(summary = "Xabarga reaksiya qo'shish yoki olib tashlash (toggle)")
    @PostMapping("/{messageId}/reactions")
    public ApiResponse<ChatMessageDto> toggleReaction(
            @PathVariable String messageId,
            @Valid @RequestBody ToggleReactionRequest request,
            @AuthenticationPrincipal User currentUser) {
        ChatMessageDto dto = chatService.toggleReaction(messageId, currentUser.getId(), request.getEmoji());
        return ApiResponse.success("ok", dto);
    }

    // ==================== QIDIRUV ====================

    @Operation(summary = "Barcha ko'rinadigan xabarlar orasidan qidirish (Public + DM)")
    @GetMapping("/search")
    public ApiResponse<Page<ChatMessageDto>> searchAllMessages(
            @RequestParam String query,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable,
            @AuthenticationPrincipal User currentUser) {
        Page<ChatMessageDto> results = chatService.searchAllMessages(currentUser.getId(), query, pageable);
        return ApiResponse.success("ok", results);
    }

    @Operation(summary = "Umumiy chatdan xabar qidirish")
    @GetMapping("/public/search")
    public ApiResponse<Page<ChatMessageDto>> searchPublicMessages(
            @RequestParam String q,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        Page<ChatMessageDto> results = chatService.searchPublicMessages(q, pageable);
        return ApiResponse.success("ok", results);
    }

    @Operation(summary = "DM suhbatdan xabar qidirish")
    @GetMapping("/direct/{otherUserId}/search")
    public ApiResponse<Page<ChatMessageDto>> searchDirectMessages(
            @PathVariable String otherUserId,
            @RequestParam String q,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable,
            @AuthenticationPrincipal User currentUser) {
        Page<ChatMessageDto> results = chatService.searchDirectMessages(
                currentUser.getId(), otherUserId, q, pageable);
        return ApiResponse.success("ok", results);
    }
}
