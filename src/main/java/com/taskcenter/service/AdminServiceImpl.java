package com.taskcenter.service;

import com.taskcenter.dto.TaskActivityDto;
import com.taskcenter.dto.admin.*;
import com.taskcenter.exception.ResourceNotFoundException;
import com.taskcenter.model.User;
import com.taskcenter.model.Workspace;
import com.taskcenter.repository.AttachmentRepository;
import com.taskcenter.repository.TaskActivityRepository;
import com.taskcenter.repository.TaskRepository;
import com.taskcenter.repository.UserRepository;
import com.taskcenter.repository.WorkspaceMemberRepository;
import com.taskcenter.repository.WorkspaceRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.lang.management.ManagementFactory;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
public class AdminServiceImpl implements AdminService {

    private final UserRepository userRepository;
    private final WorkspaceRepository workspaceRepository;
    private final WorkspaceMemberRepository workspaceMemberRepository;
    private final TaskRepository taskRepository;
    private final AttachmentRepository attachmentRepository;
    private final CacheManager cacheManager;
    private final GarbageCollectorService garbageCollectorService;
    private final InvitationCleanupService invitationCleanupService;
    private final TaskActivityRepository taskActivityRepository;

    @Autowired(required = false)
    private TelegramBotService telegramBotService;

    @Autowired(required = false)
    private EmailService emailService;

    @Value("${telegram.bot.webhook-url:}")
    private String webhookUrl;

    public AdminServiceImpl(
            UserRepository userRepository,
            WorkspaceRepository workspaceRepository,
            WorkspaceMemberRepository workspaceMemberRepository,
            TaskRepository taskRepository,
            AttachmentRepository attachmentRepository,
            CacheManager cacheManager,
            GarbageCollectorService garbageCollectorService,
            InvitationCleanupService invitationCleanupService,
            TaskActivityRepository taskActivityRepository) {
        this.userRepository = userRepository;
        this.workspaceRepository = workspaceRepository;
        this.workspaceMemberRepository = workspaceMemberRepository;
        this.taskRepository = taskRepository;
        this.attachmentRepository = attachmentRepository;
        this.cacheManager = cacheManager;
        this.garbageCollectorService = garbageCollectorService;
        this.invitationCleanupService = invitationCleanupService;
        this.taskActivityRepository = taskActivityRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public AdminDashboardStatsDto getDashboardStats() {
        long totalUsers = userRepository.count();
        long totalWorkspaces = workspaceRepository.count();
        long totalTasks = taskRepository.count();
        long completedTasks = taskRepository.countCompletedTasks();
        long openTasks = taskRepository.countOpenTasks();
        long telegramLinkedUsers = userRepository.countByTelegramChatIdIsNotNull();
        long totalAttachments = attachmentRepository.count();

        Runtime runtime = Runtime.getRuntime();
        long jvmUsedMemoryMb = (runtime.totalMemory() - runtime.freeMemory()) / (1024 * 1024);
        long jvmMaxMemoryMb = runtime.maxMemory() / (1024 * 1024);
        long uptimeSeconds = ManagementFactory.getRuntimeMXBean().getUptime() / 1000;

        String botStatus = "INACTIVE";
        if (telegramBotService != null) {
            String renderUrl = System.getenv("RENDER_EXTERNAL_URL");
            if ((webhookUrl != null && !webhookUrl.isBlank()) || (renderUrl != null && !renderUrl.isBlank())) {
                botStatus = "WEBHOOK_ACTIVE";
            } else {
                botStatus = "LONG_POLLING_ACTIVE";
            }
        }

        return AdminDashboardStatsDto.builder()
                .totalUsers(totalUsers)
                .totalWorkspaces(totalWorkspaces)
                .totalTasks(totalTasks)
                .completedTasks(completedTasks)
                .openTasks(openTasks)
                .telegramLinkedUsers(telegramLinkedUsers)
                .totalAttachments(totalAttachments)
                .jvmUsedMemoryMb(jvmUsedMemoryMb)
                .jvmMaxMemoryMb(jvmMaxMemoryMb)
                .uptimeSeconds(uptimeSeconds)
                .botStatus(botStatus)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AdminUserDto> getUsers(String search, Pageable pageable) {
        Page<User> usersPage = userRepository.searchUsers(search, pageable);
        return usersPage.map(u -> {
            int wsCount = workspaceMemberRepository.countByUserId(u.getId());
            return AdminUserDto.builder()
                    .id(u.getId())
                    .name(u.getName())
                    .fullName(u.getFullName())
                    .email(u.getEmail())
                    .role(u.getRole() != null ? u.getRole().name() : "USER")
                    .telegramChatId(u.getTelegramChatId())
                    .telegramLinked(u.getTelegramChatId() != null)
                    .lastSeenAt(u.getLastSeenAt())
                    .workspaceCount(wsCount)
                    .build();
        });
    }

    @Override
    @Transactional
    public AdminUserDto updateUserRole(String userId, AdminUserRoleRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Foydalanuvchi topilmadi: " + userId));

        user.setRole(request.getRole());
        User saved = userRepository.save(user);

        Cache usersCache = cacheManager.getCache("users");
        if (usersCache != null) {
            usersCache.evict(user.getName());
            usersCache.evict(user.getEmail());
        }

        int wsCount = workspaceMemberRepository.countByUserId(saved.getId());
        return AdminUserDto.builder()
                .id(saved.getId())
                .name(saved.getName())
                .fullName(saved.getFullName())
                .email(saved.getEmail())
                .role(saved.getRole().name())
                .telegramChatId(saved.getTelegramChatId())
                .telegramLinked(saved.getTelegramChatId() != null)
                .lastSeenAt(saved.getLastSeenAt())
                .workspaceCount(wsCount)
                .build();
    }

    @Override
    @Transactional
    public void deleteUser(String userId) {
        if (!userRepository.existsById(userId)) {
            throw new ResourceNotFoundException("Foydalanuvchi topilmadi: " + userId);
        }
        userRepository.deleteById(userId);
        Cache usersCache = cacheManager.getCache("users");
        if (usersCache != null) {
            usersCache.clear();
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AdminWorkspaceDto> getWorkspaces(String search, Pageable pageable) {
        Page<Workspace> workspacesPage = workspaceRepository.searchWorkspaces(search, pageable);
        return workspacesPage.map(w -> {
            int memberCount = workspaceMemberRepository.countByWorkspaceId(w.getId());
            int taskCount = (int) taskRepository.countAllByWorkspaceId(w.getId());
            String ownerName = "Noma'lum";
            if (w.getOwnerId() != null) {
                Optional<User> ownerOpt = userRepository.findById(w.getOwnerId());
                if (ownerOpt.isPresent()) {
                    ownerName = ownerOpt.get().getFullName() != null ? ownerOpt.get().getFullName() : ownerOpt.get().getName();
                }
            }
            return AdminWorkspaceDto.builder()
                    .id(w.getId())
                    .name(w.getTitle())
                    .description(w.getDescription())
                    .ownerId(w.getOwnerId())
                    .ownerName(ownerName)
                    .memberCount(memberCount)
                    .taskCount(taskCount)
                    .createdAt(w.getCreatedAt())
                    .build();
        });
    }

    @Override
    @Transactional
    public void deleteWorkspace(String workspaceId) {
        if (!workspaceRepository.existsById(workspaceId)) {
            throw new ResourceNotFoundException("Ish maydoni topilmadi: " + workspaceId);
        }
        workspaceRepository.deleteById(workspaceId);
        Cache wsCache = cacheManager.getCache("workspaces");
        if (wsCache != null) {
            wsCache.clear();
        }
    }

    @Override
    public AdminBroadcastResultDto broadcastTelegramMessage(AdminBroadcastRequest request) {
        long startTime = System.currentTimeMillis();
        List<User> recipients = userRepository.findAllByTelegramChatIdIsNotNull();
        int total = recipients.size();
        int success = 0;
        int failed = 0;

        if (telegramBotService != null && !recipients.isEmpty()) {
            for (User recipient : recipients) {
                try {
                    telegramBotService.enqueueMessage(recipient.getTelegramChatId(), request.getMessage());
                    success++;
                } catch (Exception ex) {
                    log.warn("Telegram broadcast yuborishda xatolik user={}: {}", recipient.getId(), ex.getMessage());
                    failed++;
                }
            }
        } else {
            log.info("Telegram broadcast: TelegramBotService faol emas yoki qabul qiluvchilar yo'q.");
        }

        long duration = System.currentTimeMillis() - startTime;
        return AdminBroadcastResultDto.builder()
                .totalRecipients(total)
                .successfulDeliveries(success)
                .failedDeliveries(failed)
                .durationMs(duration)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public Page<TaskActivityDto> getAuditLogs(Pageable pageable) {
        return taskActivityRepository.findAll(pageable)
                .map(TaskActivityDto::fromEntity);
    }

    @Override
    public List<AdminSystemJobDto> getSystemJobs() {
        List<AdminSystemJobDto> jobs = new ArrayList<>();
        jobs.add(AdminSystemJobDto.builder()
                .jobKey("garbage_collector")
                .name("Chiqindilarni tozalash (Garbage Collector)")
                .description("30 kundan ko'p vaqt oldin o'chirilgan (soft-deleted) vazifalar va fayllarni jismoniy tozalash")
                .status("IDLE")
                .lastRunAt(LocalDateTime.now())
                .build());

        jobs.add(AdminSystemJobDto.builder()
                .jobKey("invitation_cleanup")
                .name("Taklifnomalarni tozalash (Invitation Cleanup)")
                .description("Muddati o'tgan va 30 kundan eski bekor bo'lgan taklifnomalarni tozalash")
                .status("IDLE")
                .lastRunAt(LocalDateTime.now())
                .build());

        jobs.add(AdminSystemJobDto.builder()
                .jobKey("email_queue_processor")
                .name("Email navbatini qayta ishlash (Email Queue Processor)")
                .description("Kutilayotgan (PENDING) yoki to'xtab qolgan taklifnoma emaillarini qayta jo'natish")
                .status("IDLE")
                .lastRunAt(LocalDateTime.now())
                .build());

        return jobs;
    }

    @Override
    public void triggerSystemJob(String jobKey) {
        if ("garbage_collector".equalsIgnoreCase(jobKey)) {
            log.info("Admin tomonidan Garbage Collector qo'lda ishga tushirildi");
            garbageCollectorService.cleanUpOldDeletedRecords();
        } else if ("invitation_cleanup".equalsIgnoreCase(jobKey)) {
            log.info("Admin tomonidan Invitation Cleanup qo'lda ishga tushirildi");
            invitationCleanupService.cleanupExpiredInvitations();
        } else if ("email_queue_processor".equalsIgnoreCase(jobKey)) {
            log.info("Admin tomonidan Email Queue Processor qo'lda ishga tushirildi");
            if (emailService != null) {
                emailService.processPendingJobs();
            }
        } else {
            throw new IllegalArgumentException("Noma'lum xizmat kaliti: " + jobKey);
        }
    }

    @Override
    public void evictAllCaches() {
        for (String cacheName : cacheManager.getCacheNames()) {
            Cache cache = cacheManager.getCache(cacheName);
            if (cache != null) {
                cache.clear();
            }
        }
        log.info("Admin: Barcha keshlar tozalandi.");
    }
}
