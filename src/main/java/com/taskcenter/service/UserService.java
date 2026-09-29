package com.taskcenter.service;

import com.taskcenter.dto.UserDto;
import com.taskcenter.dto.UpdateProfileRequest;
import com.taskcenter.dto.ChangePasswordRequest;
import com.taskcenter.exception.BadRequestException;
import com.taskcenter.exception.ConflictException;
import com.taskcenter.exception.ForbiddenException;
import com.taskcenter.exception.ResourceNotFoundException;
import com.taskcenter.model.User;
import com.taskcenter.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final WorkspaceAuthorizationService authorizationService;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository,
                       WorkspaceAuthorizationService authorizationService,
                       PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.authorizationService = authorizationService;
        this.passwordEncoder = passwordEncoder;
    }

    public UserDto getCurrentUser(User currentUser) {
        if (currentUser == null) {
            throw new ForbiddenException("Foydalanuvchi tizimga kirmagan");
        }
        return UserDto.fromEntity(currentUser);
    }

    @Transactional(readOnly = true)
    public Page<UserDto> getUsers(String workspaceId, User currentUser, Pageable pageable) {
        if (currentUser == null) {
            throw new ForbiddenException("Foydalanuvchi tizimga kirmagan");
        }
        if (workspaceId == null || workspaceId.isBlank()) {
            throw new BadRequestException("workspaceId parametri kiritilishi shart");
        }
        authorizationService.checkAccess(workspaceId, currentUser);
        Page<User> userPage = userRepository.findByWorkspaceId(workspaceId, pageable);
        return userPage.map(UserDto::fromEntity);
    }

    @Transactional
    public String generateTelegramLinkToken(User currentUser) {
        String token = String.format("%06d", new java.util.Random().nextInt(999999));
        User user = userRepository.findById(currentUser.getId()).orElseThrow();
        user.setTelegramLinkToken(token);
        userRepository.save(user);
        return token;
    }

    @Transactional
    public UserDto updateProfile(User currentUser, UpdateProfileRequest request) {
        if (currentUser == null) {
            throw new ForbiddenException("Foydalanuvchi tizimga kirmagan");
        }
        User user = userRepository.findById(currentUser.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Foydalanuvchi topilmadi"));

        if (request.getFullName() != null) {
            user.setFullName(request.getFullName().trim());
        }

        if (request.getName() != null && !request.getName().isBlank()) {
            String newName = request.getName().trim().toLowerCase();
            if (!newName.equals(user.getName())) {
                if (userRepository.findByName(newName).isPresent()) {
                    throw new ConflictException("Ushbu login nomi band");
                }
                user.setName(newName);
            }
        }

        User saved = userRepository.save(user);
        return UserDto.fromEntity(saved);
    }

    @Transactional
    public void changePassword(User currentUser, ChangePasswordRequest request) {
        if (currentUser == null) {
            throw new ForbiddenException("Foydalanuvchi tizimga kirmagan");
        }
        User user = userRepository.findById(currentUser.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Foydalanuvchi topilmadi"));

        if (!passwordEncoder.matches(request.getOldPassword(), user.getPassword())) {
            throw new BadRequestException("Eski parol noto'g'ri kiritildi");
        }

        if (request.getOldPassword().equals(request.getNewPassword())) {
            throw new BadRequestException("Yangi parol eski parol bilan bir xil bo'lishi mumkin emas");
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword().trim()));
        userRepository.save(user);
    }
}
