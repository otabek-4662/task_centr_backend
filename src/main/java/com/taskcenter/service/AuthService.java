package com.taskcenter.service;

import com.taskcenter.dto.AuthResponse;
import com.taskcenter.dto.LoginRequest;
import com.taskcenter.dto.RegisterRequest;
import com.taskcenter.exception.BadRequestException;
import com.taskcenter.exception.ConflictException;
import com.taskcenter.exception.ResourceNotFoundException;
import com.taskcenter.exception.RateLimitException;
import com.taskcenter.model.User;
import com.taskcenter.model.RefreshToken;
import com.taskcenter.model.PasswordResetToken;
import com.taskcenter.repository.UserRepository;
import com.taskcenter.repository.RefreshTokenRepository;
import com.taskcenter.repository.PasswordResetTokenRepository;
import com.taskcenter.repository.WorkspaceInvitationRepository;
import com.taskcenter.repository.WorkspaceMemberRepository;
import com.taskcenter.security.JwtTokenProvider;
import com.taskcenter.security.RateLimitingService;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider tokenProvider;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final WorkspaceInvitationRepository invitationRepository;
    private final WorkspaceMemberRepository memberRepository;
    private final EmailService emailService;
    private final RateLimitingService rateLimitingService;

    public AuthService(AuthenticationManager authenticationManager, UserRepository userRepository,
                       PasswordEncoder passwordEncoder, JwtTokenProvider tokenProvider,
                       RefreshTokenRepository refreshTokenRepository,
                       PasswordResetTokenRepository passwordResetTokenRepository,
                       WorkspaceInvitationRepository invitationRepository,
                       WorkspaceMemberRepository memberRepository,
                       EmailService emailService,
                       RateLimitingService rateLimitingService) {
        this.authenticationManager = authenticationManager;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenProvider = tokenProvider;
        this.refreshTokenRepository = refreshTokenRepository;
        this.passwordResetTokenRepository = passwordResetTokenRepository;
        this.invitationRepository = invitationRepository;
        this.memberRepository = memberRepository;
        this.emailService = emailService;
        this.rateLimitingService = rateLimitingService;
    }

    public String createRefreshToken(String userId) {
        RefreshToken refreshToken = RefreshToken.builder()
                .userId(userId)
                .token(UUID.randomUUID().toString())
                .expiryDate(LocalDateTime.now().plusDays(30)) // 30 days
                .build();
        refreshTokenRepository.save(refreshToken);
        return refreshToken.getToken();
    }

    @Transactional
    @CacheEvict(value = "users", key = "#request.name")
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByName(request.getName())) {
            throw new ConflictException("Bu nom allaqachon ishlatilmoqda: " + request.getName());
        }

        User user = User.builder()
                .name(request.getName())
                .fullName(request.getName())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(User.Role.USER)
                .build();

        User savedUser = userRepository.save(user);

        // Auto-link pending invitations
        java.util.List<com.taskcenter.model.WorkspaceInvitation> pendingInvites = invitationRepository.findByReceiverEmailAndStatus(request.getName(), com.taskcenter.model.InvitationStatus.PENDING);
                
        for (com.taskcenter.model.WorkspaceInvitation inv : pendingInvites) {
            if (!memberRepository.existsByWorkspaceIdAndUserId(inv.getWorkspaceId(), savedUser.getId())) {
                com.taskcenter.model.WorkspaceMember member = com.taskcenter.model.WorkspaceMember.builder()
                        .workspaceId(inv.getWorkspaceId())
                        .userId(savedUser.getId())
                        .role(inv.getRole())
                        .build();
                memberRepository.save(member);
            }
            inv.setReceiverId(savedUser.getId());
            inv.setStatus(com.taskcenter.model.InvitationStatus.ACCEPTED);
            invitationRepository.save(inv);
        }

        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getName(), request.getPassword()));
        SecurityContextHolder.getContext().setAuthentication(authentication);

        String jwt = tokenProvider.generateToken(authentication);
        String refreshToken = createRefreshToken(savedUser.getId());

        return new AuthResponse(jwt, refreshToken, AuthResponse.UserDto.fromEntity(savedUser));
    }

    public AuthResponse login(LoginRequest request) {
        long waitTime = rateLimitingService.checkUsernameFailedLimit(request.getName());
        if (waitTime > 0) {
            throw new RateLimitException(waitTime);
        }

        Authentication authentication;
        try {
            authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getName(), request.getPassword()));
        } catch (org.springframework.security.core.AuthenticationException e) {
            rateLimitingService.recordFailedLogin(request.getName());
            throw e;
        }

        rateLimitingService.resetFailedLogin(request.getName());
        SecurityContextHolder.getContext().setAuthentication(authentication);

        String jwt = tokenProvider.generateToken(authentication);
        User user = getUserByNameOrEmail(request.getName());
        String refreshToken = createRefreshToken(user.getId());

        return new AuthResponse(jwt, refreshToken, AuthResponse.UserDto.fromEntity(user));
    }

    @Cacheable(value = "users", key = "#nameOrEmail")
    public User getUserByNameOrEmail(String nameOrEmail) {
        return userRepository.findByNameOrEmail(nameOrEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Foydalanuvchi topilmadi: " + nameOrEmail));
    }

    @Transactional
    public com.taskcenter.dto.TokenRefreshResponse refresh(com.taskcenter.dto.TokenRefreshRequest request) {
        String requestRefreshToken = request.getRefreshToken();

        RefreshToken token = refreshTokenRepository.findByToken(requestRefreshToken)
                .orElseThrow(() -> new ResourceNotFoundException("Refresh token bazada topilmadi"));

        if (Boolean.TRUE.equals(token.getRevoked())) {
            // Token Reuse Detection: bekor qilingan token qayta ishlatildi (token o'g'irlangan bo'lishi mumkin)
            refreshTokenRepository.deleteByUserId(token.getUserId());
            throw new com.taskcenter.exception.ForbiddenException(
                    "Xavfsizlik ogohlantirishi: Ushbu refresh token allaqachon ishlatilgan! " +
                    "Sessiya o'g'irlanishining oldini olish maqsadida barcha sessiyalar bekor qilindi. Qaytadan kiring.");
        }

        if (token.getExpiryDate().isBefore(LocalDateTime.now())) {
            refreshTokenRepository.delete(token);
            throw new com.taskcenter.exception.ForbiddenException("Refresh token muddati tugagan. Iltimos, qaytadan tizimga kiring");
        }

        User user = userRepository.findById(token.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("Foydalanuvchi topilmadi"));

        // Refresh token rotation: eski tokenni bekor qilish va yangi refresh token berish
        String newRefreshTokenStr = UUID.randomUUID().toString();
        token.setRevoked(true);
        token.setReplacedByToken(newRefreshTokenStr);
        refreshTokenRepository.save(token);

        RefreshToken newRefreshToken = RefreshToken.builder()
                .userId(user.getId())
                .token(newRefreshTokenStr)
                .expiryDate(LocalDateTime.now().plusDays(30))
                .revoked(false)
                .createdAt(LocalDateTime.now())
                .build();
        refreshTokenRepository.save(newRefreshToken);

        String newAccessToken = tokenProvider.generateTokenFromUser(user);
        return new com.taskcenter.dto.TokenRefreshResponse(newAccessToken, newRefreshTokenStr);
    }

    @Transactional
    public void logout(String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) {
            return;
        }
        refreshTokenRepository.findByToken(refreshToken.trim())
                .ifPresent(token -> {
                    token.setRevoked(true);
                    refreshTokenRepository.save(token);
                });
    }

    @Transactional
    public void logoutAll(String userId) {
        if (userId != null && !userId.isBlank()) {
            refreshTokenRepository.deleteByUserId(userId);
        }
    }

    @Transactional
    public void forgotPassword(com.taskcenter.dto.ForgotPasswordRequest request) {
        String email = request.getEmail().trim().toLowerCase();
        java.util.Optional<User> userOpt = userRepository.findByEmail(email);
        if (userOpt.isEmpty()) {
            return;
        }
        User user = userOpt.get();
        String token = UUID.randomUUID().toString();
        PasswordResetToken resetToken = PasswordResetToken.builder()
                .user(user)
                .token(token)
                .expiryDate(LocalDateTime.now().plusMinutes(30))
                .used(false)
                .build();
        passwordResetTokenRepository.save(resetToken);
        emailService.sendPasswordResetEmail(user.getEmail(), token);
    }

    @Transactional
    @CacheEvict(value = "users", key = "#result != null ? #result.name : ''", allEntries = true)
    public void resetPassword(com.taskcenter.dto.ResetPasswordRequest request) {
        PasswordResetToken resetToken = passwordResetTokenRepository.findByToken(request.getToken().trim())
                .orElseThrow(() -> new BadRequestException("Parolni tiklash tokeni yaroqsiz yoki topilmadi"));

        if (resetToken.isUsed()) {
            throw new BadRequestException("Ushbu token allaqachon ishlatilgan");
        }
        if (resetToken.isExpired()) {
            throw new BadRequestException("Ushbu tokenning amal qilish muddati tugagan. Iltimos, qaytadan so'rov yuboring");
        }

        User user = resetToken.getUser();
        user.setPassword(passwordEncoder.encode(request.getNewPassword().trim()));
        userRepository.save(user);

        resetToken.setUsed(true);
        passwordResetTokenRepository.save(resetToken);
    }
}
