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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ContactServiceTest {

    @Mock
    private UserContactRepository contactRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ChatPresenceService presenceService;

    @InjectMocks
    private ContactService contactService;

    private User currentUser;
    private User contactUser;

    @BeforeEach
    void setUp() {
        currentUser = User.builder()
                .id("u-me")
                .name("bekmurod")
                .fullName("Bekmurod")
                .email("bek@example.com")
                .build();

        contactUser = User.builder()
                .id("u-friend")
                .name("akmal")
                .fullName("Akmal")
                .email("akmal@example.com")
                .lastSeenAt(LocalDateTime.now().minusMinutes(2))
                .build();
    }

    @Test
    @DisplayName("getContacts: shaxsiy kontaktlar va ularning online statusini to'g'ri qaytaradi")
    void getContacts_success() {
        UserContact uc = UserContact.builder()
                .userId(currentUser.getId())
                .contactUserId(contactUser.getId())
                .contactUser(contactUser)
                .build();

        when(contactRepository.findByUserIdWithContactUser("u-me")).thenReturn(List.of(uc));
        when(presenceService.getOnlineUsers()).thenReturn(Set.of("akmal"));

        List<ChatUserDto> contacts = contactService.getContacts("u-me");

        assertThat(contacts).hasSize(1);
        ChatUserDto dto = contacts.get(0);
        assertThat(dto.getId()).isEqualTo("u-friend");
        assertThat(dto.getName()).isEqualTo("akmal");
        assertThat(dto.isOnline()).isTrue();
        assertThat(dto.getLastSeenAt()).isNotNull();
    }

    @Test
    @DisplayName("addContact: yangi kontakt muvaffaqiyatli qo'shiladi")
    void addContact_success() {
        AddContactRequest req = new AddContactRequest();
        req.setUsernameOrEmail("akmal");

        when(userRepository.findByNameOrEmail("akmal")).thenReturn(Optional.of(contactUser));
        when(contactRepository.existsByUserIdAndContactUserId("u-me", "u-friend")).thenReturn(false);
        when(presenceService.isOnline("akmal")).thenReturn(false);

        ChatUserDto result = contactService.addContact(currentUser, req);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo("u-friend");
        assertThat(result.isOnline()).isFalse();
        verify(contactRepository).save(any(UserContact.class));
    }

    @Test
    @DisplayName("addContact: o'zini qo'shishga uringanda BadRequestException beradi")
    void addContact_self_throwsBadRequest() {
        AddContactRequest req = new AddContactRequest();
        req.setUsernameOrEmail("bekmurod");

        when(userRepository.findByNameOrEmail("bekmurod")).thenReturn(Optional.of(currentUser));

        assertThatThrownBy(() -> contactService.addContact(currentUser, req))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("O'zingizni");
        verify(contactRepository, never()).save(any());
    }

    @Test
    @DisplayName("addContact: mavjud bo'lmagan foydalanuvchida ResourceNotFoundException beradi")
    void addContact_notFound_throwsException() {
        AddContactRequest req = new AddContactRequest();
        req.setUsernameOrEmail("notfound");

        when(userRepository.findByNameOrEmail("notfound")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> contactService.addContact(currentUser, req))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(contactRepository, never()).save(any());
    }

    @Test
    @DisplayName("addContact: allaqachon mavjud kontaktda ConflictException beradi")
    void addContact_duplicate_throwsConflict() {
        AddContactRequest req = new AddContactRequest();
        req.setUsernameOrEmail("akmal");

        when(userRepository.findByNameOrEmail("akmal")).thenReturn(Optional.of(contactUser));
        when(contactRepository.existsByUserIdAndContactUserId("u-me", "u-friend")).thenReturn(true);

        assertThatThrownBy(() -> contactService.addContact(currentUser, req))
                .isInstanceOf(ConflictException.class);
        verify(contactRepository, never()).save(any());
    }

    @Test
    @DisplayName("deleteContact: repository orqali o'chiradi")
    void deleteContact_success() {
        contactService.deleteContact(currentUser, "u-friend");
        verify(contactRepository).deleteByUserIdAndContactUserId("u-me", "u-friend");
    }
}
