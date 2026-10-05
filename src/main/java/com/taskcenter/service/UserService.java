package com.taskcenter.service;

import com.taskcenter.dto.UserDto;
import com.taskcenter.dto.TelegramLinkDto;
import org.springframework.beans.factory.annotation.Value;
import com.taskcenter.dto.UpdateProfileRequest;
import com.taskcenter.dto.ChangePasswordRequest;
import com.taskcenter.exception.BadRequestException;
import com.taskcenter.exception.ConflictException;
import com.taskcenter.exception.ForbiddenException;
import com.taskcenter.exception.ResourceNotFoundException;
import com.taskcenter.model.User;
import com.taskcenter.repository.TaskRepository;
import com.taskcenter.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.taskcenter.security.TelegramInitDataValidator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Map;
import com.taskcenter.util.TelegramUtil;

@Service
public class UserService {

    // Kriptografik jihatdan xavfsiz random generator (java.util.Random o'rniga)
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    // 8 belgili token: 36^8 ≈ 2.8 trillion kombinatsiya (6 xonali raqam = 1 million)
    private static final String TOKEN_CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    private static final int TOKEN_LENGTH = 8;
    private static final int TOKEN_TTL_MINUTES = 15;

    private final UserRepository userRepository;
    private final TaskRepository taskRepository;
    private final WorkspaceAuthorizationService authorizationService;
    private final PasswordEncoder passwordEncoder;
    private final String botUsername;
    private final TelegramInitDataValidator initDataValidator;
    private final ObjectMapper objectMapper;
    private final ApplicationEventPublisher eventPublisher;

    public UserService(UserRepository userRepository,
                       TaskRepository taskRepository,
                       WorkspaceAuthorizationService authorizationService,
                       PasswordEncoder passwordEncoder,
                       @Value("${telegram.bot.username:}") String botUsername,
                       TelegramInitDataValidator initDataValidator,
                       ObjectMapper objectMapper,
                       ApplicationEventPublisher eventPublisher) {
        this.userRepository = userRepository;
        this.taskRepository = taskRepository;
        this.authorizationService = authorizationService;
        this.passwordEncoder = passwordEncoder;
        this.botUsername = botUsername;
        this.initDataValidator = initDataValidator;
        this.objectMapper = objectMapper;
        this.eventPublisher = eventPublisher;
    }

    public UserDto getCurrentUser(User currentUser) {
        if (currentUser == null) {
            throw new ForbiddenException("Foydalanuvchi tizimga kirmagan");
        }
        long taskCount = taskRepository.countAssignedTasksByUserId(currentUser.getId());
        UserDto dto = UserDto.fromEntity(currentUser, taskCount);
        dto.setTelegramLinked(currentUser.getTelegramChatId() != null);
        return dto;
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
        return createTelegramLink(currentUser).getToken();
    }

    @Transactional
    public TelegramLinkDto createTelegramLink(User currentUser) {
        // SecureRandom bilan kriptografik jihatdan xavfsiz token yaratish
        StringBuilder sb = new StringBuilder(TOKEN_LENGTH);
        for (int i = 0; i < TOKEN_LENGTH; i++) {
            sb.append(TOKEN_CHARS.charAt(SECURE_RANDOM.nextInt(TOKEN_CHARS.length())));
        }
        String token = sb.toString();
        LocalDateTime expiresAt = LocalDateTime.now().plusMinutes(TOKEN_TTL_MINUTES);

        User user = userRepository.findById(currentUser.getId()).orElseThrow();
        user.setTelegramLinkToken(token);
        user.setTelegramLinkTokenExpiresAt(expiresAt);
        userRepository.save(user);

        String link = (botUsername != null && !botUsername.isBlank())
                ? "https://t.me/" + botUsername.trim().replaceFirst("^@", "") + "?start=" + token
                : null;
        return TelegramLinkDto.builder().token(token).link(link).expiresAt(expiresAt).build();
    }

    @Transactional
    public void unlinkTelegram(User currentUser) {
        if (currentUser == null) {
            throw new ForbiddenException("Foydalanuvchi tizimga kirmagan");
        }
        User user = userRepository.findById(currentUser.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Foydalanuvchi topilmadi"));
        user.setTelegramChatId(null);
        user.setTelegramLinkToken(null);
        user.setTelegramLinkTokenExpiresAt(null);
        userRepository.save(user);
    }

    @Transactional
    public UserDto linkTelegramViaInitData(User currentUser, String initData) {
        if (currentUser == null) {
            throw new ForbiddenException("Foydalanuvchi tizimga kirmagan");
        }
        
        Map<String, String> params = initDataValidator.validate(initData)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid or expired initData"));

        long telegramId;
        try {
            JsonNode tgUser = objectMapper.readTree(params.get("user"));
            telegramId = tgUser.get("id").asLong();
        } catch (Exception e) {
            throw new BadRequestException("Noto'g'ri user ma'lumotlari");
        }

        User user = userRepository.findById(currentUser.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Foydalanuvchi topilmadi"));

        if (user.getTelegramChatId() != null && user.getTelegramChatId() == telegramId) {
            return getCurrentUser(user);
        }

        userRepository.findByTelegramChatId(telegramId).ifPresent(u -> {
            if (!u.getId().equals(user.getId())) {
                throw new ConflictException("Bu Telegram boshqa akkauntga ulangan");
            }
        });

        user.setTelegramChatId(telegramId);
        user.setTelegramLinkToken(null);
        user.setTelegramLinkTokenExpiresAt(null);
        userRepository.save(user);

        String welcomeMsg = "🎉 Davraga xush kelibsiz, <b>" + TelegramUtil.escapeHtml(user.getFullName() != null ? user.getFullName() : user.getName()) + "</b>!\n\n"
                + "Akkauntingiz muvaffaqiyatli ulandi ☕️\n"
                + "Endi barcha g'alva va bosh og'riqlardan (vazifalardan) xabardor bo'lib turasiz hamda ularni shu yerdan boshqara olasiz!";
        eventPublisher.publishEvent(new TelegramMessageEvent(telegramId, welcomeMsg));

        return getCurrentUser(user);
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
        UserDto dto = UserDto.fromEntity(saved);
        dto.setTelegramLinked(saved.getTelegramChatId() != null);
        return dto;
    }

    @Transactional
    public void changePassword(User currentUser, ChangePasswordRequest request) {
        if (currentUser == null) {
            throw new ForbiddenException("Foydalanuvchi tizimga kirmagan");
        }
        User user = userRepository.findById(currentUser.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Foydalanuvchi topilmadi"));

        if (!passwordEncoder.matches(request.getOldPassword(), user.getPassword())) {
            throw new BadRequestException("Eski quloqqa aytiladigan so'z noto'g'ri kiritildi");
        }

        if (request.getOldPassword().equals(request.getNewPassword())) {
            throw new BadRequestException("Yangi quloqqa aytiladigan so'z eski so'z bilan bir xil bo'lishi mumkin emas");
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword().trim()));
        userRepository.save(user);
    }
}
