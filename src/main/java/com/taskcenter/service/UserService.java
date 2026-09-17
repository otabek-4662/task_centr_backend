package com.taskcenter.service;

import com.taskcenter.dto.UserDto;
import com.taskcenter.exception.BadRequestException;
import com.taskcenter.exception.ForbiddenException;
import com.taskcenter.model.User;
import com.taskcenter.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final WorkspaceAuthorizationService authorizationService;

    public UserService(UserRepository userRepository,
                       WorkspaceAuthorizationService authorizationService) {
        this.userRepository = userRepository;
        this.authorizationService = authorizationService;
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
}
