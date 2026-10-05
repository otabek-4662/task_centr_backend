package com.taskcenter.service;

import com.taskcenter.dto.TelegramReminderTaskDto;
import com.taskcenter.model.TelegramReminderLog;
import com.taskcenter.model.User;
import com.taskcenter.model.Workspace;
import com.taskcenter.repository.TaskRepository;
import com.taskcenter.repository.TelegramReminderLogRepository;
import com.taskcenter.repository.UserRepository;
import com.taskcenter.repository.WorkspaceRepository;
import com.taskcenter.util.TelegramUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Telegram eslatma (DUE_TOMORROW) va kunlik digest (DAILY_DIGEST) scheduler.
 * <p>
 * Spring Boot default TaskScheduler single-threaded, shuning uchun bitta JVM da
 * bir vaqtda ikki nusxa ishlamaydi. Ko'p instansiyali deploymentda ShedLock
 * yoki DB-level lock kerak.
 */
@Slf4j
@Service
@ConditionalOnProperty(name = "telegram.bot.token")
public class TelegramSchedulerService {

    private static final int TELEGRAM_MAX_TEXT_LENGTH = 4096;
    private static final int MAX_INLINE_BUTTONS = 10;
    private static final int BUTTON_TITLE_MAX_LENGTH = 30;

    private final TaskRepository taskRepository;
    private final TelegramReminderLogRepository reminderLogRepository;
    private final UserRepository userRepository;
    private final WorkspaceRepository workspaceRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final Clock clock;
    private final String miniappBaseUrl;

    public TelegramSchedulerService(
            TaskRepository taskRepository,
            TelegramReminderLogRepository reminderLogRepository,
            UserRepository userRepository,
            WorkspaceRepository workspaceRepository,
            ApplicationEventPublisher eventPublisher,
            Clock clock,
            @org.springframework.beans.factory.annotation.Value("${telegram.miniapp.base-url:https://task-centr-backend.onrender.com}") String miniappBaseUrl) {
        this.taskRepository = taskRepository;
        this.reminderLogRepository = reminderLogRepository;
        this.userRepository = userRepository;
        this.workspaceRepository = workspaceRepository;
        this.eventPublisher = eventPublisher;
        this.clock = clock;
        this.miniappBaseUrl = miniappBaseUrl;
    }

    // ────────────────── Scheduled entry points ──────────────────

    /**
     * Har kuni 18:00 (Asia/Tashkent) da ertangi muddatli vazifalar uchun eslatma.
     */
    @Scheduled(cron = "${telegram.scheduler.reminder-cron:0 0 18 * * *}", zone = "Asia/Tashkent")
    public void scheduleDueTomorrowReminders() {
        sendDueTomorrowReminders(LocalDateTime.now(clock));
    }

    /**
     * Har kuni 08:00 (Asia/Tashkent) da kunlik digest.
     */
    @Scheduled(cron = "${telegram.scheduler.digest-cron:0 0 8 * * *}", zone = "Asia/Tashkent")
    public void scheduleDailyDigest() {
        sendDailyDigest(LocalDateTime.now(clock));
    }

    // ────────────────── Inner logic (test uchun) ──────────────────

    /**
     * Ertaga muddati keladigan, bajarilmagan tasklarni assignee'larga eslatadi.
     * <p>
     * Har task uchun telegram_reminder_log ga yozib qo'yadi (unique buzilsa yubormas).
     *
     * @param now joriy vaqt (test uchun parametr)
     */
    void sendDueTomorrowReminders(LocalDateTime now) {
        LocalDate tomorrow = now.toLocalDate().plusDays(1);

        List<TelegramReminderTaskDto> tasks = taskRepository
                .findTasksWithAssigneesForTelegramReminder(tomorrow, tomorrow);

        if (tasks.isEmpty()) {
            log.debug("Ertangi muddatli vazifalar topilmadi: {}", tomorrow);
            return;
        }

        // Userlarni batch yuklash (quiet hours uchun)
        Set<String> userIds = tasks.stream()
                .map(TelegramReminderTaskDto::getUserId)
                .collect(Collectors.toSet());
        Map<String, User> userMap = userRepository.findByIdIn(userIds).stream()
                .collect(Collectors.toMap(User::getId, Function.identity()));

        // Userlar bo'yicha guruhlash
        Map<String, List<TelegramReminderTaskDto>> byUser = tasks.stream()
                .collect(Collectors.groupingBy(TelegramReminderTaskDto::getUserId));

        for (Map.Entry<String, List<TelegramReminderTaskDto>> entry : byUser.entrySet()) {
            try {
                processReminderForUser(entry.getKey(), entry.getValue(), userMap, now, tomorrow);
            } catch (Exception e) {
                log.error("Eslatma yuborishda xato, userId={}: {}", entry.getKey(), e.getMessage());
            }
        }
    }

    /**
     * Kunlik digest: bugun muddati va muddati o'tgan bajarilmagan tasklar.
     *
     * @param now joriy vaqt (test uchun parametr)
     */
    void sendDailyDigest(LocalDateTime now) {
        LocalDate today = now.toLocalDate();

        List<TelegramReminderTaskDto> tasks = taskRepository.findTasksForTelegramDigest(today);

        if (tasks.isEmpty()) {
            log.debug("Digest uchun vazifalar topilmadi");
            return;
        }

        // Userlarni batch yuklash (quiet hours uchun)
        Set<String> userIds = tasks.stream()
                .map(TelegramReminderTaskDto::getUserId)
                .collect(Collectors.toSet());
        Map<String, User> userMap = userRepository.findByIdIn(userIds).stream()
                .collect(Collectors.toMap(User::getId, Function.identity()));

        // Workspace nomlarini batch yuklash
        Set<String> wsIds = tasks.stream()
                .map(TelegramReminderTaskDto::getWorkspaceId)
                .collect(Collectors.toSet());
        Map<String, String> wsNameMap = workspaceRepository.findAllById(wsIds).stream()
                .collect(Collectors.toMap(Workspace::getId, Workspace::getTitle));

        // Userlar bo'yicha guruhlash
        Map<String, List<TelegramReminderTaskDto>> byUser = tasks.stream()
                .collect(Collectors.groupingBy(TelegramReminderTaskDto::getUserId));

        for (Map.Entry<String, List<TelegramReminderTaskDto>> entry : byUser.entrySet()) {
            try {
                processDigestForUser(entry.getKey(), entry.getValue(), userMap, wsNameMap, now, today);
            } catch (Exception e) {
                log.error("Digest yuborishda xato, userId={}: {}", entry.getKey(), e.getMessage());
            }
        }
    }

    // ────────────────── Per-user processing ──────────────────

    private void processReminderForUser(String userId,
                                        List<TelegramReminderTaskDto> userTasks,
                                        Map<String, User> userMap,
                                        LocalDateTime now,
                                        LocalDate tomorrow) {
        Long chatId = userTasks.get(0).getTelegramChatId();
        if (chatId == null) return;

        // Sokin soatlar tekshiruvi
        User user = userMap.get(userId);
        if (user != null && isInQuietHours(now.toLocalTime(),
                user.getTelegramQuietStart(), user.getTelegramQuietEnd())) {
            log.debug("Sokin soat, eslatma yuborilmaydi: userId={}", userId);
            return;
        }

        // Hali yuborilmagan tasklarni filtr va log yoz
        List<TelegramReminderTaskDto> newTasks = new ArrayList<>();
        for (TelegramReminderTaskDto task : userTasks) {
            if (reminderLogRepository.existsByTaskIdAndUserIdAndType(
                    task.getTaskId(), userId,
                    TelegramReminderLog.ReminderType.DUE_TOMORROW)) {
                continue;
            }
            try {
                reminderLogRepository.save(TelegramReminderLog.builder()
                        .taskId(task.getTaskId())
                        .userId(userId)
                        .type(TelegramReminderLog.ReminderType.DUE_TOMORROW)
                        .build());
                newTasks.add(task);
            } catch (Exception e) {
                log.warn("Eslatma logi saqlashda xato: taskId={}, userId={}: {}",
                        task.getTaskId(), userId, e.getMessage());
            }
        }

        if (newTasks.isEmpty()) return;

        // Xabar tuzish
        StringBuilder sb = new StringBuilder();
        sb.append("⏰ <b>Ertaga muddati tugaydigan vazifalar</b>\n");
        sb.append("📅 ").append(tomorrow).append("\n\n");
        for (TelegramReminderTaskDto t : newTasks) {
            sb.append("📋 ").append(TelegramUtil.escapeHtml(t.getTitle())).append("\n");
        }

        // TASK_VIEW va WebApp inline tugmalar (max 10)
        int maxButtons = Math.min(newTasks.size(), MAX_INLINE_BUTTONS);
        InlineKeyboardMarkup keyboard = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();
        for (int i = 0; i < maxButtons; i++) {
            TelegramReminderTaskDto t = newTasks.get(i);
            InlineKeyboardButton btn = new InlineKeyboardButton();
            String btnTitle = t.getTitle();
            if (btnTitle.length() > BUTTON_TITLE_MAX_LENGTH) {
                btnTitle = btnTitle.substring(0, BUTTON_TITLE_MAX_LENGTH - 3) + "...";
            }
            btn.setText("📋 " + btnTitle);
            btn.setCallbackData("TASK_VIEW_" + t.getTaskId());
            rows.add(List.of(btn));

            InlineKeyboardButton webAppBtn = new InlineKeyboardButton();
            webAppBtn.setText("📱 Ochish");
            webAppBtn.setWebApp(new org.telegram.telegrambots.meta.api.objects.webapp.WebAppInfo(miniappBaseUrl + "/app/?task=" + t.getTaskId()));
            rows.add(List.of(webAppBtn));
        }
        keyboard.setKeyboard(rows);

        eventPublisher.publishEvent(new TelegramMessageEvent(chatId, sb.toString(), keyboard));
    }

    private void processDigestForUser(String userId,
                                      List<TelegramReminderTaskDto> userTasks,
                                      Map<String, User> userMap,
                                      Map<String, String> wsNameMap,
                                      LocalDateTime now,
                                      LocalDate today) {
        Long chatId = userTasks.get(0).getTelegramChatId();
        if (chatId == null) return;

        // Sokin soatlar tekshiruvi
        User user = userMap.get(userId);
        if (user != null && isInQuietHours(now.toLocalTime(),
                user.getTelegramQuietStart(), user.getTelegramQuietEnd())) {
            log.debug("Sokin soat, digest yuborilmaydi: userId={}", userId);
            return;
        }

        // Statistika
        long todayCount = userTasks.stream()
                .filter(t -> t.getDueDate().isEqual(today)).count();
        long overdueCount = userTasks.stream()
                .filter(t -> t.getDueDate().isBefore(today)).count();

        // Xabar tuzish
        StringBuilder sb = new StringBuilder();
        sb.append("📊 <b>Kunlik hisobot</b>\n\n");
        if (todayCount > 0) {
            sb.append("📅 Bugun muddati: ").append(todayCount).append(" ta\n");
        }
        if (overdueCount > 0) {
            sb.append("🔴 Muddati o'tgan: ").append(overdueCount).append(" ta\n");
        }
        sb.append("\n");

        int shown = 0;
        int totalTasks = userTasks.size();
        for (TelegramReminderTaskDto t : userTasks) {
            String wsName = wsNameMap.getOrDefault(t.getWorkspaceId(), "—");
            String emoji = t.getDueDate().isBefore(today) ? "🔴" : "📅";
            String line = emoji + " " + TelegramUtil.escapeHtml(t.getTitle())
                    + " | " + TelegramUtil.escapeHtml(wsName)
                    + " | " + t.getDueDate() + "\n";

            int remaining = totalTasks - shown;
            String suffix = "... va yana " + remaining + " ta";
            if (sb.length() + line.length() > TELEGRAM_MAX_TEXT_LENGTH - suffix.length() - 1) {
                sb.append(suffix);
                break;
            }
            sb.append(line);
            shown++;
        }

        // TASK_VIEW inline tugmalar (max 10)
        int maxButtons = Math.min(userTasks.size(), MAX_INLINE_BUTTONS);
        InlineKeyboardMarkup keyboard = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();
        for (int i = 0; i < maxButtons; i++) {
            TelegramReminderTaskDto t = userTasks.get(i);
            InlineKeyboardButton btn = new InlineKeyboardButton();
            String btnTitle = t.getTitle();
            if (btnTitle.length() > BUTTON_TITLE_MAX_LENGTH) {
                btnTitle = btnTitle.substring(0, BUTTON_TITLE_MAX_LENGTH - 3) + "...";
            }
            btn.setText("📋 " + btnTitle);
            btn.setCallbackData("TASK_VIEW_" + t.getTaskId());
            rows.add(List.of(btn));

            InlineKeyboardButton webAppBtn = new InlineKeyboardButton();
            webAppBtn.setText("📱 Ochish");
            webAppBtn.setWebApp(new org.telegram.telegrambots.meta.api.objects.webapp.WebAppInfo(miniappBaseUrl + "/app/?task=" + t.getTaskId()));
            rows.add(List.of(webAppBtn));
        }
        keyboard.setKeyboard(rows);

        eventPublisher.publishEvent(new TelegramMessageEvent(chatId, sb.toString(), keyboard));
    }

    // ────────────────── Quiet hours ──────────────────

    /**
     * Berilgan vaqt sokin soatlar orasida ekanligini tekshiradi.
     * Tun bo'ylab o'tadigan oraliqlar ham qo'llab-quvvatlanadi (masalan, 22:00–08:00).
     *
     * @param now   tekshiriladigan vaqt
     * @param start sokin soat boshlanishi (null bo'lsa cheklov yo'q)
     * @param end   sokin soat tugashi (null bo'lsa cheklov yo'q)
     * @return true — sokin soatda, yubormaslik kerak
     */
    static boolean isInQuietHours(LocalTime now, LocalTime start, LocalTime end) {
        return TelegramUtil.isInQuietHours(now, start, end);
    }
}
