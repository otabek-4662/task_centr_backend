package com.taskcenter.service;

import com.taskcenter.dto.CreateLinkInviteRequest;
import com.taskcenter.dto.InviteRequestDto;
import com.taskcenter.dto.PublicInvitationDto;
import com.taskcenter.dto.TestEmailResponse;
import com.taskcenter.dto.WorkspaceInvitationDto;
import com.taskcenter.exception.BadRequestException;
import com.taskcenter.exception.ConflictException;
import com.taskcenter.exception.ForbiddenException;
import com.taskcenter.exception.RateLimitException;
import com.taskcenter.exception.ResourceNotFoundException;
import com.taskcenter.model.*;
import com.taskcenter.repository.*;
import com.taskcenter.security.RateLimitingService;
import com.taskcenter.util.InvitationTokenUtil;
import com.taskcenter.util.TelegramUtil;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
public class WorkspaceInvitationService {

    private final WorkspaceInvitationRepository invitationRepository;
    private final WorkspaceMemberRepository memberRepository;
    private final UserRepository userRepository;
    private final WorkspaceRepository workspaceRepository;
    private final WorkspaceAuthorizationService authorizationService;
    private final NotificationService notificationService;
    private final EmailService emailService;
    private final TelegramNotificationService telegramNotificationService;
    private final PendingTelegramChatInviteRepository pendingChatInviteRepository;
    private final RateLimitingService rateLimitingService;

    @Value("${telegram.bot.username:}")
    private String botUsername;

    @Value("${telegram.miniapp.short-name:app}")
    private String miniappShortName;

    @Value("${app.frontend.url:https://task-center-frontend.onrender.com}")
    private String frontendUrl;

    public WorkspaceInvitationService(WorkspaceInvitationRepository invitationRepository,
                                      WorkspaceMemberRepository memberRepository,
                                      UserRepository userRepository,
                                      WorkspaceRepository workspaceRepository,
                                      WorkspaceAuthorizationService authorizationService,
                                      NotificationService notificationService,
                                      EmailService emailService,
                                      TelegramNotificationService telegramNotificationService,
                                      PendingTelegramChatInviteRepository pendingChatInviteRepository,
                                      RateLimitingService rateLimitingService) {
        this.invitationRepository = invitationRepository;
        this.memberRepository = memberRepository;
        this.userRepository = userRepository;
        this.workspaceRepository = workspaceRepository;
        this.authorizationService = authorizationService;
        this.notificationService = notificationService;
        this.emailService = emailService;
        this.telegramNotificationService = telegramNotificationService;
        this.pendingChatInviteRepository = pendingChatInviteRepository;
        this.rateLimitingService = rateLimitingService;
    }

    /**
     * Tizimda hisobi yo'q / Telegram ulanmagan foydalanuvchilar taklifini DB da 24 soat TTL bilan saqlash.
     */
    @Transactional
    public void storePendingChatInvite(Long chatId, String token) {
        if (chatId != null && token != null && !token.isBlank()) {
            PendingTelegramChatInvite invite = PendingTelegramChatInvite.builder()
                    .chatId(chatId)
                    .token(token.trim())
                    .createdAt(LocalDateTime.now(ZoneOffset.UTC))
                    .expiresAt(LocalDateTime.now(ZoneOffset.UTC).plusHours(24))
                    .build();
            pendingChatInviteRepository.save(invite);
        }
    }

    @Transactional(readOnly = true)
    public String getPendingChatInvite(Long chatId) {
        if (chatId == null) {
            return null;
        }
        return pendingChatInviteRepository.findActiveByChatId(chatId, LocalDateTime.now(ZoneOffset.UTC))
                .map(PendingTelegramChatInvite::getToken)
                .orElse(null);
    }

    @Transactional
    public void removePendingChatInvite(Long chatId) {
        if (chatId != null) {
            pendingChatInviteRepository.deleteById(chatId);
        }
    }

    @Scheduled(cron = "0 0 2 * * *")
    @SchedulerLock(name = "WorkspaceInvitationService_cleanupExpiredPendingChatInvites", lockAtLeastFor = "5m", lockAtMostFor = "30m")
    @Transactional
    public void cleanupExpiredPendingChatInvites() {
        int deleted = pendingChatInviteRepository.deleteExpired(LocalDateTime.now(ZoneOffset.UTC));
        if (deleted > 0) {
            log.info("Eskirgan {} ta pending Telegram chat takliflari tozalandi", deleted);
        }
    }

    /**
     * Mini App orqali ulanmagan Telegram user uchun taklifni tekshirish va kutilayotgan taklif sifatida saqlash.
     * @return Telegram botga yo'naltiruvchi havola: https://t.me/<bot>?start=inv_<token>
     */
    @Transactional
    public String registerPendingTelegramChatInvite(Long telegramUserId, String rawToken) {
        if (rawToken == null || rawToken.isBlank()) {
            throw new ResourceNotFoundException("INVITE_NOT_FOUND", "Taklif tokeni kiritilmagan");
        }
        String cleanToken = rawToken.trim();
        if (cleanToken.startsWith("inv_")) {
            cleanToken = cleanToken.substring(4);
        }
        String tokenHash = InvitationTokenUtil.hashToken(cleanToken);
        WorkspaceInvitation invitation = invitationRepository.findByTokenHash(tokenHash)
                .orElseThrow(() -> new ResourceNotFoundException("INVITE_NOT_FOUND", "Bunday taklifnoma topilmadi"));

        if (invitation.getStatus() == InvitationStatus.EXPIRED ||
                (invitation.getExpiresAt() != null && invitation.getExpiresAt().isBefore(LocalDateTime.now(ZoneOffset.UTC)))) {
            throw new BadRequestException("INVITE_EXPIRED", "Ushbu taklifnomaning amal qilish muddati tugagan");
        }
        if (invitation.getStatus() == InvitationStatus.CANCELLED) {
            throw new BadRequestException("INVITE_CANCELLED", "Ushbu taklifnoma administrator tomonidan bekor qilingan");
        }
        if (invitation.getStatus() == InvitationStatus.REJECTED) {
            throw new BadRequestException("INVITE_REJECTED", "Ushbu taklifnoma allaqachon rad etilgan");
        }
        if (invitation.getType() == InvitationType.EMAIL && invitation.getStatus() == InvitationStatus.ACCEPTED) {
            throw new BadRequestException("INVITE_ALREADY_USED", "Ushbu taklifnoma allaqachon ishlatilgan");
        }
        if (invitation.getType() == InvitationType.LINK &&
                (invitation.getStatus() == InvitationStatus.ACCEPTED ||
                 (invitation.getMaxUses() != null && invitation.getUseCount() >= invitation.getMaxUses()))) {
            throw new BadRequestException("INVITE_LIMIT_REACHED", "Ushbu taklif havolasining ruxsat etilgan foydalanish limiti tugagan");
        }

        // Store 24-hour pending chat invite for this Telegram user
        storePendingChatInvite(telegramUserId, cleanToken);

        String cleanBot = (botUsername != null && !botUsername.isBlank())
                ? botUsername.trim().replaceFirst("^@", "")
                : "task_center_bot";
        return "https://t.me/" + cleanBot + "?start=inv_" + cleanToken;
    }

    public WorkspaceInvitationDto toDto(WorkspaceInvitation inv, String workspaceTitle, String senderName) {
        return toDto(inv, workspaceTitle, senderName, null);
    }

    public WorkspaceInvitationDto toDto(WorkspaceInvitation inv, String workspaceTitle, String senderName, String rawToken) {
        WorkspaceInvitationDto dto = WorkspaceInvitationDto.fromEntity(inv);
        dto.setWorkspaceTitle(workspaceTitle);
        dto.setSenderName(senderName);

        if (rawToken != null && !rawToken.isBlank()) {
            dto.setToken(rawToken);
            String baseUrl = (frontendUrl != null && !frontendUrl.isBlank())
                    ? frontendUrl.replaceAll("/+$", "")
                    : "https://task-center-frontend.onrender.com";
            dto.setInviteLink(baseUrl + "/invite/" + rawToken);

            if (botUsername != null && !botUsername.isBlank()) {
                String cleanBot = botUsername.trim().replaceFirst("^@", "");
                dto.setTelegramInviteLink("https://t.me/" + cleanBot + "?start=inv_" + rawToken);
                String cleanApp = (miniappShortName != null && !miniappShortName.isBlank()) ? miniappShortName.trim() : "app";
                dto.setTelegramMiniappLink("https://t.me/" + cleanBot + "/" + cleanApp + "?startapp=inv_" + rawToken);
            }
        }
        return dto;
    }

    public WorkspaceInvitation findInvitationByTokenOrId(String tokenOrId) {
        if (tokenOrId == null || tokenOrId.isBlank()) {
            throw new ResourceNotFoundException("INVITE_NOT_FOUND", "Taklif topilmadi");
        }
        String clean = tokenOrId.trim();
        try {
            String hash = InvitationTokenUtil.hashToken(clean);
            Optional<WorkspaceInvitation> byHash = invitationRepository.findByTokenHash(hash);
            if (byHash.isPresent()) {
                return byHash.get();
            }
        } catch (Exception ignored) {
        }

        // Backward compatibility fallback: ID bo'yicha qidirish
        return invitationRepository.findById(clean)
                .orElseThrow(() -> new ResourceNotFoundException("INVITE_NOT_FOUND", "Taklif topilmadi"));
    }

    /**
     * 1. EMAIL TAKLIF: Aniq email'ga yuboriladi, faqat shu email egasi qabul qila oladi.
     */
    @Transactional
    public WorkspaceInvitationDto inviteUser(String workspaceId, InviteRequestDto request, User currentUser) {
        Workspace workspace = authorizationService.checkAdmin(workspaceId, currentUser);

        WorkspaceRole roleToAssign = request.getRole() != null ? request.getRole() : WorkspaceRole.MEMBER;
        if (roleToAssign == WorkspaceRole.OWNER) {
            throw new BadRequestException("CANNOT_INVITE_OWNER", "A'zoga OWNER rolini berib bo'lmaydi");
        }

        long pendingCount = invitationRepository.countByWorkspaceIdAndStatus(workspaceId, InvitationStatus.PENDING);
        if (pendingCount >= 50) {
            throw new BadRequestException("INVITE_LIMIT_REACHED", "Ushbu g'alvada kutilayotgan takliflar soni 50 tadan oshib ketdi. Eski takliflarni bekor qiling.");
        }

        String rawQuery = request.getUsernameOrEmail().trim();
        String query = rawQuery.startsWith("@") ? rawQuery.substring(1).trim() : rawQuery;
        Optional<User> receiverOpt = userRepository.findByNameOrEmail(query);

        String receiverId = null;
        String receiverEmail = null;

        if (receiverOpt.isPresent()) {
            User targetUser = receiverOpt.get();
            receiverId = targetUser.getId();
            receiverEmail = targetUser.getEmail();

            if (memberRepository.existsByWorkspaceIdAndUserId(workspaceId, targetUser.getId())) {
                throw new ConflictException("ALREADY_MEMBER", "Foydalanuvchi allaqachon ushbu ish maydoni a'zosi");
            }

            Optional<WorkspaceInvitation> existingPending = invitationRepository
                    .findByWorkspaceIdAndReceiverIdAndStatus(workspaceId, targetUser.getId(), InvitationStatus.PENDING);
            if (existingPending.isPresent()) {
                throw new ConflictException("DUPLICATE_PENDING_INVITE", "Ushbu foydalanuvchiga allaqachon kutilayotgan taklif yuborilgan");
            }
        } else {
            if (!query.contains("@")) {
                throw new BadRequestException("VALIDATION_ERROR", "Foydalanuvchi tizimda topilmadi. Taklif yuborish uchun to'liq email manzilini kiriting.");
            }
            receiverEmail = query.toLowerCase();

            Optional<WorkspaceInvitation> existingPendingEmail = invitationRepository
                    .findByWorkspaceIdAndReceiverEmailIgnoreCaseAndStatus(workspaceId, receiverEmail, InvitationStatus.PENDING);
            if (existingPendingEmail.isPresent()) {
                throw new ConflictException("DUPLICATE_PENDING_INVITE", "Ushbu email manziliga allaqachon kutilayotgan taklif yuborilgan");
            }
        }

        String rawToken = InvitationTokenUtil.generateToken();
        String tokenHash = InvitationTokenUtil.hashToken(rawToken);

        WorkspaceInvitation invitation = WorkspaceInvitation.builder()
                .workspaceId(workspaceId)
                .senderId(currentUser.getId())
                .receiverId(receiverId)
                .receiverEmail(receiverEmail)
                .type(InvitationType.EMAIL)
                .tokenHash(tokenHash)
                .role(roleToAssign)
                .status(InvitationStatus.PENDING)
                .maxUses(1)
                .useCount(0)
                .emailDeliveryStatus(EmailDeliveryStatus.PENDING)
                .expiresAt(LocalDateTime.now(ZoneOffset.UTC).plusDays(7))
                .build();

        if (receiverEmail != null && !receiverEmail.endsWith("@taskcenter.local")) {
            EmailService.EmailDeliveryResult deliveryResult = emailService.sendInvitationEmailDirect(
                    receiverEmail, workspace.getTitle(), currentUser.getName(), roleToAssign.name(), rawToken);
            invitation.setEmailDeliveryStatus(deliveryResult.status());
            invitation.setEmailDeliveryError(deliveryResult.errorMessage());
        } else {
            invitation.setEmailDeliveryStatus(EmailDeliveryStatus.NOT_APPLICABLE);
        }

        WorkspaceInvitation saved = invitationRepository.save(invitation);

        if (receiverId != null) {
            notificationService.notifyUser(receiverId, "Yangi taklif", currentUser.getName() + " sizni " + workspace.getTitle() + " g'alvasiga taklif qildi.");
            receiverOpt.ifPresent(receiver -> telegramNotificationService.sendWorkspaceInviteNotification(workspace, receiver, currentUser, saved.getId()));
        }

        return toDto(saved, workspace.getTitle(), currentUser.getName(), rawToken);
    }

    /**
     * 2. HAVOLA TAKLIF (Invite link): email'siz, rol bilan, muddati va max foydalanish soni bor.
     * Mantiq: xom token bazada hash qilinganligi sababli, faqat yangi yaratilganda rawToken qaytariladi.
     * Agar avval yaratilgan faol link bo'lsa, xavfsizlik maqsadida metama'lumotlar qaytariladi (token = null).
     * Yangi xom havola olish uchun 'regenerateLinkInvite' ishlatiladi.
     */
    @Transactional
    public WorkspaceInvitationDto createOrGetLinkInvite(String workspaceId, CreateLinkInviteRequest request, User currentUser) {
        Workspace workspace = authorizationService.checkAdmin(workspaceId, currentUser);

        Optional<WorkspaceInvitation> existingOpt = invitationRepository.findByWorkspaceIdAndTypeAndStatus(
                workspaceId, InvitationType.LINK, InvitationStatus.PENDING
        );
        if (existingOpt.isPresent()) {
            WorkspaceInvitation existing = existingOpt.get();
            boolean notExpired = existing.getExpiresAt() == null || existing.getExpiresAt().isAfter(LocalDateTime.now(ZoneOffset.UTC));
            boolean hasRemainingUses = existing.getMaxUses() == null || existing.getUseCount() < existing.getMaxUses();
            if (notExpired && hasRemainingUses) {
                return toDto(existing, workspace.getTitle(), currentUser.getName(), null);
            }
        }

        WorkspaceRole roleToAssign = request != null && request.getRole() != null ? request.getRole() : WorkspaceRole.MEMBER;
        if (roleToAssign == WorkspaceRole.OWNER) {
            throw new BadRequestException("CANNOT_INVITE_OWNER", "A'zoga OWNER rolini berib bo'lmaydi");
        }

        long pendingCount = invitationRepository.countByWorkspaceIdAndStatus(workspaceId, InvitationStatus.PENDING);
        if (pendingCount >= 50) {
            throw new BadRequestException("INVITE_LIMIT_REACHED", "Ushbu g'alvada kutilayotgan takliflar soni 50 tadan oshib ketdi. Eski takliflarni bekor qiling.");
        }

        int durationDays = (request != null && request.getDurationDays() != null && request.getDurationDays() > 0)
                ? request.getDurationDays() : 7;
        Integer maxUses = (request != null && request.getMaxUses() != null && request.getMaxUses() > 0)
                ? request.getMaxUses() : null;

        String rawToken = InvitationTokenUtil.generateToken();
        String tokenHash = InvitationTokenUtil.hashToken(rawToken);

        WorkspaceInvitation invitation = WorkspaceInvitation.builder()
                .workspaceId(workspaceId)
                .senderId(currentUser.getId())
                .type(InvitationType.LINK)
                .tokenHash(tokenHash)
                .role(roleToAssign)
                .status(InvitationStatus.PENDING)
                .maxUses(maxUses)
                .useCount(0)
                .emailDeliveryStatus(EmailDeliveryStatus.NOT_APPLICABLE)
                .expiresAt(LocalDateTime.now(ZoneOffset.UTC).plusDays(durationDays))
                .build();

        WorkspaceInvitation saved = invitationRepository.save(invitation);
        return toDto(saved, workspace.getTitle(), currentUser.getName(), rawToken);
    }

    /**
     * Havolani yangilash: eski faol link-takliflarni bekor qiladi va yangi xom token yaratadi.
     */
    @Transactional
    public WorkspaceInvitationDto regenerateLinkInvite(String workspaceId, CreateLinkInviteRequest request, User currentUser) {
        authorizationService.checkAdmin(workspaceId, currentUser);

        List<WorkspaceInvitation> existing = invitationRepository.findByWorkspaceIdAndStatus(workspaceId, InvitationStatus.PENDING);
        for (WorkspaceInvitation inv : existing) {
            if (inv.getType() == InvitationType.LINK) {
                inv.setStatus(InvitationStatus.CANCELLED);
                invitationRepository.save(inv);
            }
        }

        Workspace workspace = authorizationService.checkAdmin(workspaceId, currentUser);
        WorkspaceRole roleToAssign = request != null && request.getRole() != null ? request.getRole() : WorkspaceRole.MEMBER;
        if (roleToAssign == WorkspaceRole.OWNER) {
            throw new BadRequestException("CANNOT_INVITE_OWNER", "A'zoga OWNER rolini berib bo'lmaydi");
        }

        int durationDays = (request != null && request.getDurationDays() != null && request.getDurationDays() > 0)
                ? request.getDurationDays() : 7;
        Integer maxUses = (request != null && request.getMaxUses() != null && request.getMaxUses() > 0)
                ? request.getMaxUses() : null;

        String rawToken = InvitationTokenUtil.generateToken();
        String tokenHash = InvitationTokenUtil.hashToken(rawToken);

        WorkspaceInvitation invitation = WorkspaceInvitation.builder()
                .workspaceId(workspaceId)
                .senderId(currentUser.getId())
                .type(InvitationType.LINK)
                .tokenHash(tokenHash)
                .role(roleToAssign)
                .status(InvitationStatus.PENDING)
                .maxUses(maxUses)
                .useCount(0)
                .emailDeliveryStatus(EmailDeliveryStatus.NOT_APPLICABLE)
                .expiresAt(LocalDateTime.now(ZoneOffset.UTC).plusDays(durationDays))
                .build();

        WorkspaceInvitation saved = invitationRepository.save(invitation);
        return toDto(saved, workspace.getTitle(), currentUser.getName(), rawToken);
    }

    /**
     * Email taklifnomani qayta yuborish (Resend).
     */
    @Transactional
    public WorkspaceInvitationDto resendEmailInvite(String workspaceId, String invitationId, User currentUser) {
        Workspace workspace = authorizationService.checkAdmin(workspaceId, currentUser);

        WorkspaceInvitation invitation = invitationRepository.findById(invitationId)
                .orElseThrow(() -> new ResourceNotFoundException("INVITE_NOT_FOUND", "Taklif topilmadi"));

        if (!invitation.getWorkspaceId().equals(workspaceId)) {
            throw new BadRequestException("VALIDATION_ERROR", "Taklif ushbu ish maydoniga tegishli emas");
        }
        if (invitation.getStatus() != InvitationStatus.PENDING) {
            throw new BadRequestException("INVITE_ALREADY_USED", "Faqat kutilayotgan (PENDING) taklifnomani qayta yuborish mumkin");
        }
        if (invitation.getType() != InvitationType.EMAIL || invitation.getReceiverEmail() == null) {
            throw new BadRequestException("INVALID_INVITE_TYPE", "Faqat email orqali yuborilgan taklifnomani qayta jo'natish mumkin");
        }

        String rawToken = InvitationTokenUtil.generateToken();
        String tokenHash = InvitationTokenUtil.hashToken(rawToken);
        invitation.setTokenHash(tokenHash);
        invitation.setExpiresAt(LocalDateTime.now(ZoneOffset.UTC).plusDays(7));

        EmailService.EmailDeliveryResult deliveryResult = emailService.sendInvitationEmailDirect(
                invitation.getReceiverEmail(), workspace.getTitle(), currentUser.getName(), invitation.getRole().name(), rawToken);
        invitation.setEmailDeliveryStatus(deliveryResult.status());
        invitation.setEmailDeliveryError(deliveryResult.errorMessage());

        WorkspaceInvitation saved = invitationRepository.save(invitation);
        return toDto(saved, workspace.getTitle(), currentUser.getName(), rawToken);
    }

    @Transactional
    public WorkspaceInvitationDto createTelegramInvite(String workspaceId, WorkspaceRole role, User currentUser) {
        return createOrGetLinkInvite(workspaceId, CreateLinkInviteRequest.builder().role(role).build(), currentUser);
    }

    /**
     * Ommaviy ko'rish uchun minimal xavfsiz ma'lumotlar qaytarish (GET /api/invitations/{token})
     */
    @Transactional(readOnly = true)
    public PublicInvitationDto getPublicInvitation(String token) {
        WorkspaceInvitation invitation = findInvitationByTokenOrId(token);

        if (invitation.getStatus() == InvitationStatus.CANCELLED) {
            throw new ResourceNotFoundException("INVITE_CANCELLED", "Taklif topilmadi yoki bekor qilingan");
        }
        if (invitation.getStatus() == InvitationStatus.EXPIRED) {
            throw new ResourceNotFoundException("INVITE_EXPIRED", "Taklif topilmadi yoki muddati o'tgan");
        }

        String workspaceTitle = workspaceRepository.findById(invitation.getWorkspaceId())
                .map(Workspace::getTitle)
                .orElse("Noma'lum");

        String senderName = userRepository.findById(invitation.getSenderId())
                .map(u -> u.getFullName() != null ? u.getFullName() : u.getName())
                .orElse("Jamoa administratori");

        boolean isExpired = (invitation.getExpiresAt() != null && invitation.getExpiresAt().isBefore(LocalDateTime.now(ZoneOffset.UTC)))
                || invitation.getStatus() != InvitationStatus.PENDING
                || (invitation.getType() == InvitationType.LINK && invitation.getMaxUses() != null && invitation.getUseCount() >= invitation.getMaxUses());

        return PublicInvitationDto.builder()
                .workspaceTitle(workspaceTitle)
                .inviterName(senderName)
                .role(invitation.getRole())
                .type(invitation.getType())
                .expiresAt(invitation.getExpiresAt() != null ? invitation.getExpiresAt().toInstant(ZoneOffset.UTC) : null)
                .expired(isExpired)
                .build();
    }

    @Transactional(readOnly = true)
    public WorkspaceInvitationDto getInvitationDetails(String tokenOrId) {
        WorkspaceInvitation invitation = findInvitationByTokenOrId(tokenOrId);
        String wsTitle = workspaceRepository.findById(invitation.getWorkspaceId()).map(Workspace::getTitle).orElse("Noma'lum");
        String senderName = userRepository.findById(invitation.getSenderId()).map(u -> u.getFullName() != null ? u.getFullName() : u.getName()).orElse("Administrator");
        return toDto(invitation, wsTitle, senderName);
    }

    /**
     * Taklifni qabul qilish (Atomik qabul va parallel so'rovlar xavfsizligi).
     */
    @Transactional
    public void acceptInvitation(String tokenOrId, User currentUser) {
        WorkspaceInvitation invitation = findInvitationByTokenOrId(tokenOrId);
        processAcceptInvitation(invitation, currentUser);
    }

    /**
     * GET /api/invitations/me dagi taklifni ID bo'yicha qabul qilish.
     * Faqat taklif receiverId yoki unga tegishli email egasiga ishlaydi.
     */
    @Transactional
    public void acceptInvitationById(String id, User currentUser) {
        WorkspaceInvitation invitation = invitationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("INVITE_NOT_FOUND", "Taklif topilmadi"));

        if (invitation.getType() != InvitationType.EMAIL) {
            throw new BadRequestException("INVALID_INVITE_TYPE", "Faqat EMAIL turidagi takliflarni id orqali qabul qilish mumkin");
        }

        validateReceiverOwnership(invitation, currentUser);
        processAcceptInvitation(invitation, currentUser);
    }

    /**
     * GET /api/invitations/me dagi taklifni ID bo'yicha rad etish.
     * Faqat EMAIL turidagi va taklif receiverId egasiga ishlaydi.
     */
    @Transactional
    public void rejectInvitationById(String id, User currentUser) {
        WorkspaceInvitation invitation = invitationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("INVITE_NOT_FOUND", "Taklif topilmadi"));

        if (invitation.getType() != InvitationType.EMAIL) {
            throw new BadRequestException("INVALID_INVITE_TYPE", "Faqat EMAIL turidagi takliflarni id orqali rad qilish mumkin");
        }

        validateReceiverOwnership(invitation, currentUser);

        if (invitation.getStatus() != InvitationStatus.PENDING) {
            throw new BadRequestException("INVITE_ALREADY_USED", "Taklif allaqachon yakunlangan");
        }

        invitation.setStatus(InvitationStatus.REJECTED);
        invitationRepository.save(invitation);

        Workspace ws = workspaceRepository.findById(invitation.getWorkspaceId()).orElse(null);
        String wsTitle = ws != null ? ws.getTitle() : "loyihaga";

        if (!invitation.getSenderId().equals(currentUser.getId())) {
            notificationService.notifyUser(
                    invitation.getSenderId(),
                    "Taklif rad etildi",
                    currentUser.getName() + " sizning " + wsTitle + " ish maydoniga taklifingizni rad etdi."
            );
            userRepository.findById(invitation.getSenderId()).ifPresent(sender -> {
                if (sender.getTelegramChatId() != null) {
                    telegramNotificationService.sendMessage(
                            sender.getTelegramChatId(),
                            "❌ <b>" + TelegramUtil.escapeHtml(currentUser.getName()) + "</b> sizning <b>" + TelegramUtil.escapeHtml(wsTitle) + "</b> loyihasiga taklifingizni rad etdi."
                    );
                }
            });
        }
    }

    private void validateReceiverOwnership(WorkspaceInvitation invitation, User currentUser) {
        // Tizimda email_verified tasdiqlash oqimi mavjud emasligi sababli, receiverId == null
        // bo'lganda faqat email mos kelishiga tayanib bo'lmaydi (har kim o'z profiliga har qanday emailni kiritishi mumkin).
        // Shuning uchun id orqali boshqarishda FAQAT taklifning receiverId ustuni joriy foydalanuvchi ID'siga
        // teng bo'lishiga tayaniladi.
        if (invitation.getReceiverId() == null || !invitation.getReceiverId().equals(currentUser.getId())) {
            throw new ForbiddenException("INVITE_NOT_FOR_USER", "Bu taklif sizga tegishli emas");
        }
    }

    private void processAcceptInvitation(WorkspaceInvitation invitation, User currentUser) {
        if (invitation.getStatus() == InvitationStatus.CANCELLED) {
            throw new BadRequestException("INVITE_CANCELLED", "Ushbu taklifnoma bekor qilingan");
        }
        if (invitation.getStatus() == InvitationStatus.REJECTED) {
            throw new BadRequestException("INVITE_REJECTED", "Ushbu taklifnoma allaqachon rad etilgan");
        }
        if (invitation.getStatus() == InvitationStatus.EXPIRED) {
            throw new BadRequestException("INVITE_EXPIRED", "Taklif yaroqsiz yoki muddati o'tgan");
        }
        if (invitation.getType() == InvitationType.EMAIL && invitation.getStatus() == InvitationStatus.ACCEPTED) {
            throw new BadRequestException("INVITE_ALREADY_USED", "Ushbu taklifnoma allaqachon qabul qilingan");
        }
        if (invitation.getType() == InvitationType.LINK && invitation.getStatus() == InvitationStatus.ACCEPTED) {
            throw new BadRequestException("INVITE_LIMIT_REACHED", "Ushbu taklif havolasining foydalanish limiti tugagan");
        }
        if (invitation.getExpiresAt() != null && invitation.getExpiresAt().isBefore(LocalDateTime.now(ZoneOffset.UTC))) {
            invitation.setStatus(InvitationStatus.EXPIRED);
            invitationRepository.save(invitation);
            throw new BadRequestException("INVITE_EXPIRED", "Taklif yaroqsiz yoki muddati o'tgan");
        }
        if (invitation.getType() == InvitationType.LINK && invitation.getMaxUses() != null && invitation.getUseCount() >= invitation.getMaxUses()) {
            throw new BadRequestException("INVITE_LIMIT_REACHED", "Ushbu taklif havolasining foydalanish limiti tugagan");
        }

        // Email taklifnomani tekshirish
        if (invitation.getType() == InvitationType.EMAIL) {
            if (currentUser.getEmail() != null && currentUser.getEmail().endsWith("@taskcenter.local")) {
                throw new BadRequestException("INVITE_DUMMY_EMAIL", "Ushbu taklifnoma aniq email egasiga yuborilgan. Profilingiz Telegram orqali ochilgan. Qo'shilish uchun LINK taklif havolasidan foydalaning yoki ish maydoni adminiga murojaat qiling.");
            }
            if (invitation.getReceiverEmail() != null && !invitation.getReceiverEmail().equalsIgnoreCase(currentUser.getEmail())) {
                String masked = InvitationTokenUtil.maskEmail(invitation.getReceiverEmail());
                throw new ForbiddenException("INVITE_EMAIL_MISMATCH", "Bu taklif " + masked + " manziliga yuborilgan. Iltimos, tegishli hisob bilan kiring", masked);
            }
            if (invitation.getReceiverId() != null && !invitation.getReceiverId().equals(currentUser.getId())) {
                throw new ForbiddenException("INVITE_NOT_FOR_USER", "Bu taklif sizga tegishli emas");
            }
        }

        // MUHIM (3-talab): Foydalanuvchi allaqachon a'zo bo'lsa use_count oshirilmasin
        if (memberRepository.existsByWorkspaceIdAndUserId(invitation.getWorkspaceId(), currentUser.getId())) {
            throw new ConflictException("ALREADY_MEMBER", "Siz allaqachon ushbu ish maydoni a'zosisiz");
        }

        // Atomik tarzda joy band qilish / statusni yangilash
        if (invitation.getType() == InvitationType.LINK) {
            int updatedRows = invitationRepository.incrementUseCountAtomic(invitation.getId(), LocalDateTime.now(ZoneOffset.UTC));
            if (updatedRows == 0) {
                throw new BadRequestException("INVITE_LIMIT_REACHED", "Ushbu taklif havolasining foydalanish limiti to'lgan yoki muddati o'tgan");
            }
            invitation.setUseCount(invitation.getUseCount() + 1);
        } else {
            int updatedRows = invitationRepository.acceptEmailInvitationAtomic(invitation.getId(), currentUser.getId(), LocalDateTime.now(ZoneOffset.UTC));
            if (updatedRows == 0) {
                throw new BadRequestException("INVITE_ALREADY_USED", "Taklif allaqachon qabul qilingan yoki muddati o'tgan");
            }
            invitation.setStatus(InvitationStatus.ACCEPTED);
            invitation.setReceiverId(currentUser.getId());
        }

        WorkspaceMember member = WorkspaceMember.builder()
                .workspaceId(invitation.getWorkspaceId())
                .userId(currentUser.getId())
                .role(invitation.getRole())
                .build();
        memberRepository.save(member);
        authorizationService.evictRole(invitation.getWorkspaceId(), currentUser.getId());

        Workspace ws = workspaceRepository.findById(invitation.getWorkspaceId()).orElse(null);
        String wsTitle = ws != null ? ws.getTitle() : "loyihaga";

        // Taklif qiluvchiga bildirishnoma yuborish
        if (!invitation.getSenderId().equals(currentUser.getId())) {
            notificationService.notifyUser(
                    invitation.getSenderId(),
                    "Taklif qabul qilindi",
                    currentUser.getName() + " sizning " + wsTitle + " ish maydoniga taklifingizni qabul qildi."
            );
            userRepository.findById(invitation.getSenderId()).ifPresent(sender -> {
                if (sender.getTelegramChatId() != null) {
                    telegramNotificationService.sendMessage(
                            sender.getTelegramChatId(),
                            "✅ <b>" + TelegramUtil.escapeHtml(currentUser.getName()) + "</b> sizning <b>" + TelegramUtil.escapeHtml(wsTitle) + "</b> loyihasiga taklifingizni qabul qildi."
                    );
                }
            });
        }
    }

    /**
     * Taklifni rad etish.
     * LINK taklifda bitta user rad etishi umumiy havolani buzmasligi kerak; faqat EMAIL taklifda status REJECTED bo'ladi.
     */
    @Transactional
    public void rejectInvitation(String tokenOrId, User currentUser) {
        WorkspaceInvitation invitation = findInvitationByTokenOrId(tokenOrId);

        if (invitation.getStatus() == InvitationStatus.REJECTED) {
            throw new BadRequestException("INVITE_REJECTED", "Ushbu taklifnoma allaqachon rad etilgan");
        }
        if (invitation.getStatus() == InvitationStatus.CANCELLED) {
            throw new BadRequestException("INVITE_CANCELLED", "Ushbu taklifnoma bekor qilingan");
        }
        if (invitation.getStatus() == InvitationStatus.ACCEPTED) {
            throw new BadRequestException("INVITE_ALREADY_USED", "Ushbu taklifnoma allaqachon qabul qilingan");
        }
        if (invitation.getStatus() != InvitationStatus.PENDING) {
            throw new BadRequestException("INVITE_ALREADY_USED", "Taklif allaqachon yakunlangan");
        }

        if (invitation.getType() == InvitationType.LINK) {
            log.info("Foydalanuvchi {} umumiy havola-taklifni rad etdi (havola faol qoladi): id={}", currentUser.getId(), invitation.getId());
            return;
        }

        if (invitation.getReceiverId() != null && !invitation.getReceiverId().equals(currentUser.getId())) {
            throw new ForbiddenException("INVITE_NOT_FOR_USER", "Bu taklif sizga tegishli emas");
        }
        if (invitation.getReceiverEmail() != null && !invitation.getReceiverEmail().equalsIgnoreCase(currentUser.getEmail())) {
            String masked = InvitationTokenUtil.maskEmail(invitation.getReceiverEmail());
            throw new ForbiddenException("INVITE_EMAIL_MISMATCH", "Bu taklif " + masked + " manziliga yuborilgan. Iltimos, tegishli hisob bilan kiring", masked);
        }

        invitation.setStatus(InvitationStatus.REJECTED);
        invitationRepository.save(invitation);

        Workspace ws = workspaceRepository.findById(invitation.getWorkspaceId()).orElse(null);
        String wsTitle = ws != null ? ws.getTitle() : "loyihaga";

        if (!invitation.getSenderId().equals(currentUser.getId())) {
            notificationService.notifyUser(
                    invitation.getSenderId(),
                    "Taklif rad etildi",
                    currentUser.getName() + " sizning " + wsTitle + " ish maydoniga taklifingizni rad etdi."
            );
            userRepository.findById(invitation.getSenderId()).ifPresent(sender -> {
                if (sender.getTelegramChatId() != null) {
                    telegramNotificationService.sendMessage(
                            sender.getTelegramChatId(),
                            "❌ <b>" + TelegramUtil.escapeHtml(currentUser.getName()) + "</b> sizning <b>" + TelegramUtil.escapeHtml(wsTitle) + "</b> loyihasiga taklifingizni rad etdi."
                    );
                }
            });
        }
    }

    @Transactional
    public void cancelInvitation(String workspaceId, String id, User currentUser) {
        authorizationService.checkAdmin(workspaceId, currentUser);

        WorkspaceInvitation invitation = invitationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("INVITE_NOT_FOUND", "Taklif topilmadi"));

        if (!invitation.getWorkspaceId().equals(workspaceId)) {
            throw new BadRequestException("VALIDATION_ERROR", "Ushbu taklif berilgan workspace ga tegishli emas");
        }

        invitation.setStatus(InvitationStatus.CANCELLED);
        invitationRepository.save(invitation);
    }

    @Transactional(readOnly = true)
    public List<WorkspaceInvitationDto> getMyPendingInvitations(User currentUser) {
        return invitationRepository.findByReceiverIdAndStatusOrderByCreatedAtDesc(currentUser.getId(), InvitationStatus.PENDING)
                .stream().map(inv -> {
                    String wsTitle = workspaceRepository.findById(inv.getWorkspaceId()).map(Workspace::getTitle).orElse("Noma'lum");
                    String senderName = userRepository.findById(inv.getSenderId()).map(u -> u.getFullName() != null ? u.getFullName() : u.getName()).orElse("Administrator");
                    return toDto(inv, wsTitle, senderName);
                }).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<WorkspaceInvitationDto> getWorkspaceInvitations(String workspaceId, User currentUser) {
        Workspace ws = authorizationService.checkAdmin(workspaceId, currentUser);
        String wsTitle = ws.getTitle();
        return invitationRepository.findByWorkspaceIdOrderByCreatedAtDesc(workspaceId)
                .stream().map(inv -> {
                    String senderName = userRepository.findById(inv.getSenderId()).map(u -> u.getFullName() != null ? u.getFullName() : u.getName()).orElse("Administrator");
                    return toDto(inv, wsTitle, senderName);
                }).collect(Collectors.toList());
    }

    /**
     * Brevo test email jo'natish: faqat so'rov yuboruvchining o'z emailiga va soatiga 5 ta limit bilan.
     */
    @Transactional(readOnly = true)
    public TestEmailResponse testEmail(String workspaceId, User currentUser) {
        authorizationService.checkAdmin(workspaceId, currentUser);

        if (currentUser.getEmail() == null || currentUser.getEmail().endsWith("@taskcenter.local")) {
            throw new BadRequestException("INVITE_DUMMY_EMAIL", "Sizning profilingiz Telegram orqali ochilgan bo'lib, haqiqiy email manzili biriktirilmagan. Sinov emailini yuborib bo'lmaydi.");
        }

        long waitSeconds = rateLimitingService.tryConsumeUserLimit("test-email", currentUser.getId(), 5, 60);
        if (waitSeconds > 0) {
            throw new RateLimitException("TEST_EMAIL_LIMIT", "Sinov xatini yuborish limiti oshdi (soatiga ko'pi bilan 5 ta). Iltimos, " + waitSeconds + " soniyadan keyin qayta urinib ko'ring.", waitSeconds);
        }

        return emailService.sendTestEmail(currentUser.getEmail());
    }
}
