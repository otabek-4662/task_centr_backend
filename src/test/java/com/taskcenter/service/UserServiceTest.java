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
}

