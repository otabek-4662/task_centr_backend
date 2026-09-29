package com.taskcenter.service;

import com.taskcenter.dto.ChangePasswordRequest;
import com.taskcenter.dto.UpdateProfileRequest;
import com.taskcenter.dto.UserDto;
import com.taskcenter.exception.BadRequestException;
import com.taskcenter.exception.ConflictException;
import com.taskcenter.exception.ForbiddenException;
import com.taskcenter.model.User;
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
    private WorkspaceAuthorizationService authorizationService;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserService userService;

    @Test
    void getCurrentUser_returnsUserDto() {
        User user = User.builder().id("u1").name("tester").fullName("Test User").role(User.Role.USER).build();
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
}
