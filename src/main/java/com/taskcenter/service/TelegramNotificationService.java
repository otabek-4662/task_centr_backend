package com.taskcenter.service;

import com.taskcenter.model.Task;
import com.taskcenter.model.User;
import com.taskcenter.model.Workspace;
import com.taskcenter.repository.TaskRepository;
import com.taskcenter.repository.UserRepository;
import com.taskcenter.repository.WorkspaceMemberRepository;
import com.taskcenter.repository.WorkspaceRepository;
import com.taskcenter.util.TelegramUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;

import java.time.Clock;
import java.time.LocalTime;
import java.util.*;

@Slf4j
@Service
public class TelegramNotificationService {

    private final ApplicationEventPublisher eventPublisher;
    private final TaskRepository taskRepository;
    private final UserRepository userRepository;
    private final WorkspaceMemberRepository workspaceMemberRepository;
    private final WorkspaceRepository workspaceRepository;
    private final Clock clock;
    private final String miniappBaseUrl;

    public TelegramNotificationService(
            ApplicationEventPublisher eventPublisher,
            TaskRepository taskRepository,
            UserRepository userRepository,
            WorkspaceMemberRepository workspaceMemberRepository,
            WorkspaceRepository workspaceRepository,
            Clock clock,
            @org.springframework.beans.factory.annotation.Value("${telegram.miniapp.base-url:https://task-centr-backend.onrender.com}") String miniappBaseUrl) {
        this.eventPublisher = eventPublisher;
        this.taskRepository = taskRepository;
        this.userRepository = userRepository;
        this.workspaceMemberRepository = workspaceMemberRepository;
        this.workspaceRepository = workspaceRepository;
        this.clock = clock;
        this.miniappBaseUrl = miniappBaseUrl;
    }

    /**
     * Izoh yozilganda mention va watcher/assignee larni xabardor qilish.
     * Agar bir user ham mention, ham watcher/assignee bo'lsa, faqat bitta xabar ketsin (mention ustuvor).
     */
    public void sendCommentAndMentionNotifications(Task task, User actor, String commentText) {
        if (task == null || actor == null) {
            return;
        }

        try {
            // 1. Task ning assignees va watchers larini bitta so'rovda yuklash (N+1 bo'lmasligi uchun)
            Task taskWithDetails = taskRepository.findByIdWithAssigneesAndWatchers(task.getId()).orElse(task);

            String escapedComment = TelegramUtil.truncateComment(commentText, 100);
            String actorName = actor.getName();
            InlineKeyboardMarkup keyboard = TelegramUtil.createTaskViewKeyboard(taskWithDetails.getId(), taskWithDetails.getTitle(), miniappBaseUrl);

            // 2. Mention larni ajratib olish
            Set<String> mentionedUsernames = TelegramUtil.extractMentionedUsernames(commentText);
            mentionedUsernames.remove(actor.getName()); // actor o'zini o'zi mention qilsa e'tiborsiz qoldirish

            Map<String, User> mentionRecipients = new HashMap<>();
            if (!mentionedUsernames.isEmpty()) {
                List<User> mentionedUsers = userRepository.findByNameIn(mentionedUsernames);
                for (User u : mentionedUsers) {
                    if (isWorkspaceMember(taskWithDetails.getWorkspaceId(), u.getId())) {
                        mentionRecipients.put(u.getId(), u);
                    }
                }
            }

            // 3. Watcher va Assignee larni yig'ish
            Map<String, User> commentRecipients = new HashMap<>();
            if (taskWithDetails.getAssignees() != null) {
                for (User u : taskWithDetails.getAssignees()) {
                    commentRecipients.put(u.getId(), u);
                }
            }
            if (taskWithDetails.getWatchers() != null) {
                for (User u : taskWithDetails.getWatchers()) {
                    commentRecipients.put(u.getId(), u);
                }
            }

            // Deduplication: mention bo'lgan userlar commentRecipients dan olib tashlanadi (chunki mention ustuvor)
            for (String mentionUserId : mentionRecipients.keySet()) {
                commentRecipients.remove(mentionUserId);
            }

            LocalTime now = LocalTime.now(clock);

            // 4. Mention xabarlarini yuborish
            String mentionMsg = TelegramUtil.escapeHtml(actorName) + " sizni eslab o'tdi: " + escapedComment;
            for (User recipient : mentionRecipients.values()) {
                try {
                    sendIfEligible(recipient, actor, mentionMsg, keyboard, recipient.isTelegramNotifyComments(), now);
                } catch (Exception e) {
                    log.error("Telegram mention bildirishnomasi yuborishda xatolik: userId={}", recipient.getId(), e);
                }
            }

            // 5. Izoh xabarlarini yuborish
            String commentMsg = TelegramUtil.escapeHtml(actorName) + " izoh yozdi: " + escapedComment;
            for (User recipient : commentRecipients.values()) {
                try {
                    sendIfEligible(recipient, actor, commentMsg, keyboard, recipient.isTelegramNotifyComments(), now);
                } catch (Exception e) {
                    log.error("Telegram izoh bildirishnomasi yuborishda xatolik: userId={}", recipient.getId(), e);
                }
            }
        } catch (Exception e) {
            log.error("sendCommentAndMentionNotifications bajarishda umumiy xatolik: taskId={}", task.getId(), e);
        }
    }

    /**
     * Task boshqa ustunga o'tganda watcher va assigneelarga xabar yuborish.
     */
    public void sendTaskMovedNotification(Task task, User actor, String oldColumnTitle, String newColumnTitle) {
        if (task == null || actor == null) {
            return;
        }

        try {
            Task taskWithDetails = taskRepository.findByIdWithAssigneesAndWatchers(task.getId()).orElse(task);

            Map<String, User> recipients = new HashMap<>();
            if (taskWithDetails.getAssignees() != null) {
                for (User u : taskWithDetails.getAssignees()) {
                    recipients.put(u.getId(), u);
                }
            }
            if (taskWithDetails.getWatchers() != null) {
                for (User u : taskWithDetails.getWatchers()) {
                    recipients.put(u.getId(), u);
                }
            }

            String msg = "Task '" + TelegramUtil.escapeHtml(taskWithDetails.getTitle()) + "' "
                    + TelegramUtil.escapeHtml(oldColumnTitle) + " dan "
                    + TelegramUtil.escapeHtml(newColumnTitle) + " ga o'tdi";

            InlineKeyboardMarkup keyboard = TelegramUtil.createTaskViewKeyboard(taskWithDetails.getId(), taskWithDetails.getTitle(), miniappBaseUrl);
            LocalTime now = LocalTime.now(clock);

            for (User recipient : recipients.values()) {
                try {
                    sendIfEligible(recipient, actor, msg, keyboard, recipient.isTelegramNotifyMoves(), now);
                } catch (Exception e) {
                    log.error("Telegram ustunga o'tish bildirishnomasi yuborishda xatolik: userId={}", recipient.getId(), e);
                }
            }
        } catch (Exception e) {
            log.error("sendTaskMovedNotification bajarishda umumiy xatolik: taskId={}", task.getId(), e);
        }
    }

    /**
     * Workspace ga taklif qilinganda taklif qilingan userga xabar yuborish (agar tizimda bo'lsa va Telegram ulangan bo'lsa).
     * Bu xabarda TASK_VIEW tugmasi bo'lmaydi.
     */
    public void sendWorkspaceInviteNotification(Workspace workspace, User receiver, User actor) {
        if (workspace == null || receiver == null || actor == null) {
            return;
        }

        try {
            String msg = "Sizni '" + TelegramUtil.escapeHtml(workspace.getTitle()) + "' ga taklif qilishdi";
            LocalTime now = LocalTime.now(clock);
            sendIfEligible(receiver, actor, msg, null, receiver.isTelegramNotifyInvites(), now);
        } catch (Exception e) {
            log.error("Telegram taklif bildirishnomasi yuborishda xatolik: userId={}", receiver.getId(), e);
        }
    }

    /**
     * Foydalanuvchiga vazifa biriktirilganda xabar yuborish.
     */
    public void sendTaskAssignedNotification(Task task, User assignee, User actor) {
        if (task == null || assignee == null || actor == null) {
            return;
        }

        try {
            String msg = "🔔 Sizga yangi bosh og'riq (vazifa) biriktirildi!\n\n📌 Nomi: "
                    + TelegramUtil.escapeHtml(task.getTitle())
                    + "\n⚠️ Muhimligi: " + (task.getPriority() != null ? task.getPriority() : "MEDIUM")
                    + "\n\nQozonda qaynatish vaqti keldi! ☕️";

            InlineKeyboardMarkup keyboard = TelegramUtil.createTaskViewKeyboard(task.getId(), task.getTitle(), miniappBaseUrl);
            LocalTime now = LocalTime.now(clock);

            sendIfEligible(assignee, actor, msg, keyboard, assignee.isTelegramNotifyAssigned(), now);
        } catch (Exception e) {
            log.error("Telegram vazifa biriktirish bildirishnomasi yuborishda xatolik: userId={}, taskId={}", assignee.getId(), task.getId(), e);
        }
    }

    private void sendIfEligible(User recipient, User actor, String message, InlineKeyboardMarkup keyboard,
                                boolean isNotificationEnabled, LocalTime now) {
        if (recipient == null) {
            return;
        }
        // O'zini o'zi xabardor qilmaslik
        if (recipient.getId().equals(actor.getId())) {
            return;
        }
        // Telegram chat ID mavjudligini tekshirish
        if (recipient.getTelegramChatId() == null) {
            return;
        }
        // Foydalanuvchi sozlamasi yoqilganligini tekshirish
        if (!isNotificationEnabled) {
            return;
        }
        // Sokin soat oraliqqa tushsa yuborilmaydi (keyinga qoldirilmaydi)
        if (TelegramUtil.isInQuietHours(now, recipient.getTelegramQuietStart(), recipient.getTelegramQuietEnd())) {
            return;
        }

        // Asinxron va AFTER_COMMIT ketishi uchun ApplicationEventPublisher orqali publish qilish
        eventPublisher.publishEvent(new TelegramMessageEvent(recipient.getTelegramChatId(), message, keyboard));
    }

    private boolean isWorkspaceMember(String workspaceId, String userId) {
        if (workspaceId == null || userId == null) return false;
        return workspaceMemberRepository.existsByWorkspaceIdAndUserId(workspaceId, userId)
                || workspaceRepository.findById(workspaceId).map(w -> userId.equals(w.getOwnerId())).orElse(false);
    }
}
