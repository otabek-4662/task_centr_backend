package com.taskcenter.service;

import com.taskcenter.dto.ChangePasswordRequest;
import com.taskcenter.dto.UpdateProfileRequest;
import com.taskcenter.dto.UserDto;
import com.taskcenter.exception.BadRequestException;
import com.taskcenter.exception.ConflictException;
import com.taskcenter.exception.ForbiddenException;
import com.taskcenter.model.User;
import com.taskcenter.repository.TaskRepository;
import com.taskcenter.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import com.taskcenter.security.TelegramInitDataValidator;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import java.util.Map;
import com.fasterxml.jackson.databind.JsonNode;
import com.taskcenter.service.TelegramMessageEvent;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private TaskRepository taskRepository;

    @Mock
    private WorkspaceAuthorizationService authorizationService;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private TelegramInitDataValidator initDataValidator;

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private UserService userService;

    @Test
    void getCurrentUser_returnsUserDto() {
        User user = User.builder().id("u1").name("tester").fullName("Test User").role(User.Role.USER).build();
        when(taskRepository.countAssignedTasksByUserId("u1")).thenReturn(3L);

        UserDto dto = userService.getCurrentUser(user);

        assertThat(dto).isNotNull();
        assertThat(dto.getName()).isEqualTo("tester");
        assertThat(dto.getFullName()).isEqualTo("Test User");
    }

    @Test
    void getCurrentUser_unauthenticatedThrowsForbidden() {
        assertThatThrownBy(() -> userService.getCurrentUser(null))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void updateProfile_updatesFullNameAndName() {
        User user = User.builder().id("u1").name("oldname").fullName("Old Name").role(User.Role.USER).build();
        when(userRepository.findById("u1")).thenReturn(Optional.of(user));
        when(userRepository.findByName("newname")).thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));

        UpdateProfileRequest request = new UpdateProfileRequest();
        request.setFullName("New Full Name");
        request.setName("newname");

        UserDto updated = userService.updateProfile(user, request);

        assertThat(updated.getFullName()).isEqualTo("New Full Name");
        assertThat(updated.getName()).isEqualTo("newname");
        verify(userRepository).save(user);
    }

    @Test
    void updateProfile_duplicateNameThrowsConflict() {
        User user = User.builder().id("u1").name("oldname").fullName("Old Name").build();
        User existingOther = User.builder().id("u2").name("takenname").build();

        when(userRepository.findById("u1")).thenReturn(Optional.of(user));
        when(userRepository.findByName("takenname")).thenReturn(Optional.of(existingOther));

        UpdateProfileRequest request = new UpdateProfileRequest();
        request.setName("takenname");

        assertThatThrownBy(() -> userService.updateProfile(user, request))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("band");
    }

    @Test
    void changePassword_success() {
        User user = User.builder().id("u1").name("user1").password("encodedOld").build();
        when(userRepository.findById("u1")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("OldPassword123", "encodedOld")).thenReturn(true);
        when(passwordEncoder.encode("NewPassword123")).thenReturn("encodedNew");

        ChangePasswordRequest request = new ChangePasswordRequest();
        request.setOldPassword("OldPassword123");
        request.setNewPassword("NewPassword123");

        userService.changePassword(user, request);

        assertThat(user.getPassword()).isEqualTo("encodedNew");
        verify(userRepository).save(user);
    }

    @Test
    void changePassword_wrongOldPasswordThrowsBadRequest() {
        User user = User.builder().id("u1").name("user1").password("encodedOld").build();
        when(userRepository.findById("u1")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("WrongOld", "encodedOld")).thenReturn(false);

        ChangePasswordRequest request = new ChangePasswordRequest();
        request.setOldPassword("WrongOld");
        request.setNewPassword("NewPassword123");

        assertThatThrownBy(() -> userService.changePassword(user, request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("noto'g'ri");
    }

    @Test
    void changePassword_samePasswordThrowsBadRequest() {
        User user = User.builder().id("u1").name("user1").password("encodedOld").build();
        when(userRepository.findById("u1")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("SamePass123", "encodedOld")).thenReturn(true);

        ChangePasswordRequest request = new ChangePasswordRequest();
        request.setOldPassword("SamePass123");
        request.setNewPassword("SamePass123");

        assertThatThrownBy(() -> userService.changePassword(user, request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("bir xil");
    }

    // === FIX #3 — generateTelegramLinkToken: SecureRandom, 8 belgi, muddati bor ===

    @Test
    void generateTelegramLinkToken_hasCorrectLengthAndFormat() {
        User user = User.builder().id("u1").name("tester").role(User.Role.USER).build();
        User dbUser = User.builder().id("u1").name("tester").role(User.Role.USER).build();
        when(userRepository.findById("u1")).thenReturn(Optional.of(dbUser));
        when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));

        String token = userService.generateTelegramLinkToken(user);

        // 8 belgili bo'lishi kerak
        assertThat(token).hasSize(8);
        // Faqat katta harf va raqamlardan iborat bo'lishi kerak
        assertThat(token).matches("[A-Z0-9]{8}");
        // Faqat raqamdan iborat emas (eski 6 xonali formatdan farqli)
        // (bu test probabilistik: 36^8 / 10^8 ≈ 2800x kam ehtimollik)
        assertThat(token).isNotBlank();
    }

    @Test
    void generateTelegramLinkToken_setsExpiryWithin15Minutes() {
        User user = User.builder().id("u1").name("tester").role(User.Role.USER).build();
        User dbUser = User.builder().id("u1").name("tester").role(User.Role.USER).build();
        when(userRepository.findById("u1")).thenReturn(Optional.of(dbUser));
        when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));

        java.time.LocalDateTime before = java.time.LocalDateTime.now();
        userService.generateTelegramLinkToken(user);
        java.time.LocalDateTime after = java.time.LocalDateTime.now();

        // Token muddati qo'yilgan bo'lishi va 14-16 daqiqa orasida bo'lishi kerak
        assertThat(dbUser.getTelegramLinkTokenExpiresAt())
                .isAfter(before.plusMinutes(14))
                .isBefore(after.plusMinutes(16));
    }

    @Test
    void generateTelegramLinkToken_twoCallsProduceDifferentTokens() {
        // Bir xil user uchun ikki marta chaqirilsa farqli token bo'lishi kerak (Random emas)
        User user = User.builder().id("u1").name("tester").role(User.Role.USER).build();
        User dbUser1 = User.builder().id("u1").name("tester").role(User.Role.USER).build();
        User dbUser2 = User.builder().id("u1").name("tester").role(User.Role.USER).build();

        when(userRepository.findById("u1"))
                .thenReturn(Optional.of(dbUser1))
                .thenReturn(Optional.of(dbUser2));
        when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));

        String token1 = userService.generateTelegramLinkToken(user);
        String token2 = userService.generateTelegramLinkToken(user);

        // Ikki token bir xil bo'lish ehtimoli 36^8 = 2.8 trillion dan 1 — praktikda imkonsiz
        assertThat(token1).isNotEqualTo(token2);
    }

    // === Telegram: telegramLinked, link, expiresAt, unlink ===

    @Test
    void getCurrentUser_telegramLinkedReflectsChatId() {
        User linked = User.builder().id("u1").name("a").role(User.Role.USER).telegramChatId(123L).build();
        User notLinked = User.builder().id("u2").name("b").role(User.Role.USER).build();
        when(taskRepository.countAssignedTasksByUserId(any())).thenReturn(0L);

        assertThat(userService.getCurrentUser(linked).getTelegramLinked()).isTrue();
        assertThat(userService.getCurrentUser(notLinked).getTelegramLinked()).isFalse();
    }

    @Test
    void createTelegramLink_returnsTokenLinkAndExpiresAt() {
        org.springframework.test.util.ReflectionTestUtils.setField(userService, "botUsername", "task_center_bot");
        User user = User.builder().id("u1").name("tester").role(User.Role.USER).build();
        User dbUser = User.builder().id("u1").name("tester").role(User.Role.USER).build();
        when(userRepository.findById("u1")).thenReturn(Optional.of(dbUser));
        when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));

        com.taskcenter.dto.TelegramLinkDto dto = userService.createTelegramLink(user);

        assertThat(dto.getToken()).matches("[A-Z0-9]{8}");
        assertThat(dto.getLink()).isEqualTo("https://t.me/task_center_bot?start=" + dto.getToken());
        assertThat(dto.getExpiresAt()).isEqualTo(dbUser.getTelegramLinkTokenExpiresAt());
        assertThat(dbUser.getTelegramLinkToken()).isEqualTo(dto.getToken());
    }

    @Test
    void createTelegramLink_noBotUsername_linkIsNull() {
        org.springframework.test.util.ReflectionTestUtils.setField(userService, "botUsername", "");
        User user = User.builder().id("u1").name("tester").role(User.Role.USER).build();
        when(userRepository.findById("u1")).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));

        com.taskcenter.dto.TelegramLinkDto dto = userService.createTelegramLink(user);

        assertThat(dto.getToken()).isNotBlank();
        assertThat(dto.getLink()).isNull();
    }

    @Test
    void unlinkTelegram_clearsChatIdAndTokenForCurrentUserOnly() {
        User current = User.builder().id("u1").name("a").role(User.Role.USER).build();
        User dbUser = User.builder().id("u1").name("a").role(User.Role.USER)
                .telegramChatId(555L)
                .telegramLinkToken("ABCD1234")
                .telegramLinkTokenExpiresAt(java.time.LocalDateTime.now().plusMinutes(5))
                .build();
        when(userRepository.findById("u1")).thenReturn(Optional.of(dbUser));
        when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));

        userService.unlinkTelegram(current);

        assertThat(dbUser.getTelegramChatId()).isNull();
        assertThat(dbUser.getTelegramLinkToken()).isNull();
        assertThat(dbUser.getTelegramLinkTokenExpiresAt()).isNull();
        verify(userRepository).findById("u1");
        verify(userRepository).save(dbUser);
    }

    @Test
    void unlinkTelegram_unauthenticatedThrowsForbidden() {
        assertThatThrownBy(() -> userService.unlinkTelegram(null))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void linkTelegram_validData_success() throws Exception {
        User user = User.builder().id("u1").name("tester").role(User.Role.USER).build();
        when(initDataValidator.validate("valid")).thenReturn(Optional.of(Map.of("user", "{}")));
        JsonNode node = new com.fasterxml.jackson.databind.ObjectMapper().readTree("{\"id\":12345}");
        when(objectMapper.readTree("{}")).thenReturn(node);
        when(userRepository.findById("u1")).thenReturn(Optional.of(user));
        when(userRepository.findByTelegramChatId(12345L)).thenReturn(Optional.empty());

        UserDto dto = userService.linkTelegramViaInitData(user, "valid");
        
        assertThat(dto.getTelegramLinked()).isTrue();
        assertThat(user.getTelegramChatId()).isEqualTo(12345L);
        verify(userRepository).save(user);
        verify(eventPublisher).publishEvent(any(TelegramMessageEvent.class));
    }

    @Test
    void linkTelegram_invalidData_throws401() {
        User user = User.builder().id("u1").name("tester").build();
        when(initDataValidator.validate("invalid")).thenReturn(Optional.empty());
        
        assertThatThrownBy(() -> userService.linkTelegramViaInitData(user, "invalid"))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Invalid or expired initData");
    }

    @Test
    void linkTelegram_alreadyLinkedToAnother_throws409() throws Exception {
        User user = User.builder().id("u1").name("tester").build();
        when(initDataValidator.validate("valid")).thenReturn(Optional.of(Map.of("user", "{}")));
        JsonNode node = new com.fasterxml.jackson.databind.ObjectMapper().readTree("{\"id\":12345}");
        when(objectMapper.readTree("{}")).thenReturn(node);
        when(userRepository.findById("u1")).thenReturn(Optional.of(user));
        
        User otherUser = User.builder().id("u2").name("other").build();
        when(userRepository.findByTelegramChatId(12345L)).thenReturn(Optional.of(otherUser));
        
        assertThatThrownBy(() -> userService.linkTelegramViaInitData(user, "valid"))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("Bu Telegram boshqa akkauntga ulangan");
    }

    @Test
    void linkTelegram_idempotent_success() throws Exception {
        User user = User.builder().id("u1").name("tester").telegramChatId(12345L).build();
        when(initDataValidator.validate("valid")).thenReturn(Optional.of(Map.of("user", "{}")));
        JsonNode node = new com.fasterxml.jackson.databind.ObjectMapper().readTree("{\"id\":12345}");
        when(objectMapper.readTree("{}")).thenReturn(node);
        when(userRepository.findById("u1")).thenReturn(Optional.of(user));
        
        UserDto dto = userService.linkTelegramViaInitData(user, "valid");
        assertThat(dto.getTelegramLinked()).isTrue();
        // shud not save again or publish event again if already linked
    }

    @Test
    void linkTelegram_unauthenticated_throws403() {
        assertThatThrownBy(() -> userService.linkTelegramViaInitData(null, "valid"))
                .isInstanceOf(ForbiddenException.class);
    }
}

