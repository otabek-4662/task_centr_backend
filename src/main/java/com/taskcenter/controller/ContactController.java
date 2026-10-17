package com.taskcenter.controller;

import com.taskcenter.dto.AddContactRequest;
import com.taskcenter.dto.ApiResponse;
import com.taskcenter.dto.ChatUserDto;
import com.taskcenter.model.User;
import com.taskcenter.service.ContactService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Contacts", description = "Shaxsiy kontaktlar boshqaruvi va ularning real vaqt holati (Online / Last seen)")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/contacts")
public class ContactController {

    private final ContactService contactService;

    public ContactController(ContactService contactService) {
        this.contactService = contactService;
    }

    @Operation(
            operationId = "getMyContacts",
            summary = "Foydalanuvchining shaxsiy kontaktlari ro'yxatini olish",
            description = "Foydalanuvchining saqlangan kontaktlarini, ularning real vaqtdagi online/offline holati va oxirgi marta qachon ko'ringani (lastSeenAt) bilan qaytaradi."
    )
    @GetMapping
    public ApiResponse<List<ChatUserDto>> getContacts(@AuthenticationPrincipal User currentUser) {
        List<ChatUserDto> contacts = contactService.getContacts(currentUser.getId());
        return ApiResponse.success("ok", contacts);
    }

    @Operation(
            operationId = "addContact",
            summary = "Login nomi yoki email orqali yangi kontakt qo'shish",
            description = "Boshqa foydalanuvchini login nomi (username) yoki emaili orqali shaxsiy kontaktlar ro'yxatiga qo'shadi."
    )
    @PostMapping
    public ApiResponse<ChatUserDto> addContact(
            @AuthenticationPrincipal User currentUser,
            @Valid @RequestBody AddContactRequest request) {
        ChatUserDto contact = contactService.addContact(currentUser, request);
        return ApiResponse.success("Kontakt muvaffaqiyatli qo'shildi", contact);
    }

    @Operation(
            operationId = "deleteContact",
            summary = "Kontaktni ro'yxatdan o'chirish",
            description = "Foydalanuvchini shaxsiy kontaktlar ro'yxatidan chiqarib tashlaydi."
    )
    @DeleteMapping("/{contactUserId}")
    public ApiResponse<Void> deleteContact(
            @AuthenticationPrincipal User currentUser,
            @Parameter(description = "O'chirilayotgan kontaktning foydalanuvchi ID si", example = "user-123")
            @PathVariable String contactUserId) {
        contactService.deleteContact(currentUser, contactUserId);
        return ApiResponse.success("Kontakt o'chirildi", null);
    }
}
