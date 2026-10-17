package com.taskcenter.service;

import com.taskcenter.dto.AddContactRequest;
import com.taskcenter.dto.ChatUserDto;
import com.taskcenter.exception.BadRequestException;
import com.taskcenter.exception.ConflictException;
import com.taskcenter.exception.ResourceNotFoundException;
import com.taskcenter.model.User;
import com.taskcenter.model.UserContact;
import com.taskcenter.repository.UserContactRepository;
import com.taskcenter.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class ContactService {

    private final UserContactRepository contactRepository;
    private final UserRepository userRepository;
    private final ChatPresenceService presenceService;

    public ContactService(UserContactRepository contactRepository,
                          UserRepository userRepository,
                          ChatPresenceService presenceService) {
        this.contactRepository = contactRepository;
        this.userRepository = userRepository;
        this.presenceService = presenceService;
    }

    @Transactional(readOnly = true)
    public List<ChatUserDto> getContacts(String currentUserId) {
        List<UserContact> contacts = contactRepository.findByUserIdWithContactUser(currentUserId);
        Set<String> onlineUsernames = safeGetOnlineUsers();

        return contacts.stream()
                .map(uc -> {
                    User u = uc.getContactUser();
                    boolean online = onlineUsernames != null
                            ? onlineUsernames.contains(u.getName())
                            : presenceService.isOnline(u.getName());
                    return toDto(u, online);
                })
                .collect(Collectors.toList());
    }

    @Transactional
    public ChatUserDto addContact(User currentUser, AddContactRequest request) {
        String query = request.getUsernameOrEmail().trim();
        User targetUser = userRepository.findByNameOrEmail(query)
                .orElseThrow(() -> new ResourceNotFoundException("Foydalanuvchi topilmadi: " + query));

        if (targetUser.getId().equals(currentUser.getId())) {
            throw new BadRequestException("O'zingizni kontakt sifatida qo'sha olmaysiz");
        }

        if (contactRepository.existsByUserIdAndContactUserId(currentUser.getId(), targetUser.getId())) {
            throw new ConflictException("Ushbu foydalanuvchi allaqachon kontaktlaringiz ro'yxatida mavjud");
        }

        UserContact contact = UserContact.builder()
                .userId(currentUser.getId())
                .contactUserId(targetUser.getId())
                .build();
        contactRepository.save(contact);

        boolean online = presenceService.isOnline(targetUser.getName());
        return toDto(targetUser, online);
    }

    @Transactional
    public void deleteContact(User currentUser, String contactUserId) {
        contactRepository.deleteByUserIdAndContactUserId(currentUser.getId(), contactUserId);
    }

    private Set<String> safeGetOnlineUsers() {
        try {
            return presenceService.getOnlineUsers();
        } catch (Exception e) {
            return null;
        }
    }

    private ChatUserDto toDto(User user, boolean online) {
        return ChatUserDto.builder()
                .id(user.getId())
                .name(user.getName())
                .fullName(user.getFullName() != null ? user.getFullName() : user.getName())
                .email(user.getEmail())
                .online(online)
                .lastSeenAt(user.getLastSeenAt())
                .build();
    }
}
