package com.taskcenter.service;

import com.taskcenter.dto.TaskDto;
import com.taskcenter.dto.UserDto;
import com.taskcenter.dto.UserTaskStatsProjection;
import com.taskcenter.dto.WebSocketEvent;
import com.taskcenter.model.BoardColumn;
import com.taskcenter.model.IssueType;
import com.taskcenter.model.Priority;
import com.taskcenter.model.Task;
import com.taskcenter.model.User;
import com.taskcenter.model.Workspace;
import com.taskcenter.repository.ColumnRepository;
import com.taskcenter.repository.TaskRepository;
import com.taskcenter.repository.UserRepository;
import com.taskcenter.repository.WorkspaceRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.scheduling.annotation.Async;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.task.TaskExecutor;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import jakarta.annotation.PostConstruct;
import org.telegram.telegrambots.meta.api.methods.commands.SetMyCommands;
import org.telegram.telegrambots.meta.api.objects.commands.BotCommand;
import org.telegram.telegrambots.meta.api.objects.commands.scope.BotCommandScopeDefault;
import com.taskcenter.model.TaskActivityType;
import com.taskcenter.repository.TelegramReminderLogRepository;
import com.taskcenter.util.LexoRankUtil;
import com.taskcenter.util.TelegramUtil;

@Slf4j
@Service
@org.springframework.boot.autoconfigure.condition.ConditionalOnProperty(name = "telegram.bot.token")
public class TelegramBotService extends TelegramLongPollingBot {

    private final String botUsername;
    private final UserRepository userRepository;
    private final TaskRepository taskRepository;
    private final ColumnRepository columnRepository;
    private final WorkspaceRepository workspaceRepository;
    private final WorkspaceAuthorizationService authorizationService;
    private final TaskExecutor taskExecutor;
    private final Clock clock;
    private final TaskActivityService activityService;
    private final TelegramNotificationService telegramNotificationService;
    private final TelegramReminderLogRepository telegramReminderLogRepository;
    private final WebSocketNotifier webSocketNotifier;
    private final String miniappBaseUrl;

    public enum TaskCreationStep {
        AWAITING_TITLE,
        AWAITING_WORKSPACE,
        AWAITING_PRIORITY
    }

    public static class TaskCreationState {
        private String title;
        private String workspaceId;
        private TaskCreationStep step;
        private final LocalDateTime createdAt = LocalDateTime.now();

        public TaskCreationState(TaskCreationStep step) {
            this.step = step;
        }

        public boolean isExpired() {
            return createdAt.isBefore(LocalDateTime.now().minusMinutes(15));
        }

        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }
        public String getWorkspaceId() { return workspaceId; }
        public void setWorkspaceId(String workspaceId) { this.workspaceId = workspaceId; }
        public TaskCreationStep getStep() { return step; }
        public void setStep(TaskCreationStep step) { this.step = step; }
    }

    private final Map<Long, TaskCreationState> taskCreationStates = new ConcurrentHashMap<>();

    private static class RateLimit {
        int attempts;
        LocalDateTime windowStart;
        RateLimit(int a, LocalDateTime w) { this.attempts = a; this.windowStart = w; }
    }
    private final Map<Long, RateLimit> startRateLimits = new ConcurrentHashMap<>();

    public TelegramBotService(
            @Value("${telegram.bot.token:}") String botToken,
            @Value("${telegram.bot.username:}") String botUsername,
            UserRepository userRepository,
            TaskRepository taskRepository,
            ColumnRepository columnRepository,
            WorkspaceRepository workspaceRepository,
            WorkspaceAuthorizationService authorizationService,
            TaskExecutor taskExecutor,
            Clock clock,
            TaskActivityService activityService,
            TelegramNotificationService telegramNotificationService,
            TelegramReminderLogRepository telegramReminderLogRepository,
            WebSocketNotifier webSocketNotifier,
            @Value("${telegram.miniapp.base-url:https://task-centr-backend.onrender.com}") String miniappBaseUrl) {
        super(botToken);
        this.botUsername = botUsername;
        this.userRepository = userRepository;
        this.taskRepository = taskRepository;
        this.columnRepository = columnRepository;
        this.workspaceRepository = workspaceRepository;
        this.authorizationService = authorizationService;
        this.taskExecutor = taskExecutor;
        this.clock = clock;
        this.activityService = activityService;
        this.telegramNotificationService = telegramNotificationService;
        this.telegramReminderLogRepository = telegramReminderLogRepository;
        this.webSocketNotifier = webSocketNotifier;
        this.miniappBaseUrl = miniappBaseUrl;
    }

    @Override
    public String getBotUsername() {
        return botUsername;
    }

    @Async
    @EventListener(ApplicationReadyEvent.class)
    public void initCommands() {
        try {
            List<BotCommand> commands = List.of(
                new BotCommand("start", "Botni ishga tushirish"),
                new BotCommand("create_task", "Yangi vazifa yaratish"),
                new BotCommand("my_tasks", "Bosh og'riqlarim"),
                new BotCommand("stats", "Shaxsiy statistika va maqom"),
                new BotCommand("overdue", "Muddati o'tgan vazifalar"),
                new BotCommand("tasks_today", "Bugungi vazifalar"),
                new BotCommand("app", "Mini Appni ochish"),
                new BotCommand("help", "Yordam"),
                new BotCommand("unlink", "Akkauntni uzish")
            );
            execute(new SetMyCommands(commands, new BotCommandScopeDefault(), null));
            log.info("Telegram bot commands registered successfully");
        } catch (Exception e) {
            log.error("Failed to register telegram bot commands", e);
        }
    }

    @Override
    @Transactional
    public void onUpdateReceived(Update update) {
        if (update.hasCallbackQuery()) {
            if (update.getCallbackQuery().getMessage() == null || 
                update.getCallbackQuery().getMessage().getChat() == null || 
                !"private".equals(update.getCallbackQuery().getMessage().getChat().getType())) {
                return;
            }
            handleCallbackQuery(update.getCallbackQuery());
        } else if (update.hasMessage() && update.getMessage().hasText()) {
            if (update.getMessage().getChat() == null || 
                !"private".equals(update.getMessage().getChat().getType())) {
                sendMessage(update.getMessage().getChatId(), "Ushbu bot faqat shaxsiy chatda ishlaydi.");
                return;
            }
            String text = update.getMessage().getText().trim();
            Long chatId = update.getMessage().getChatId();

            if (text.startsWith("/start")) {
                taskCreationStates.remove(chatId);
                handleStart(chatId, text);
            } else if (text.equals("/cancel") || text.equalsIgnoreCase("bekor qilish")) {
                if (taskCreationStates.remove(chatId) != null) {
                    sendMessage(chatId, "❌ Vazifa yaratish bekor qilindi.");
                } else {
                    sendMessage(chatId, "Hozirda bekor qilinadigan faol jarayon yo'q.");
                }
            } else if (text.equals("/create_task") || text.equals("➕ Yangi vazifa")) {
                handleCreateTaskCommand(chatId, null);
            } else if (text.startsWith("/create_task ")) {
                String title = text.substring(13).trim();
                handleCreateTaskCommand(chatId, title);
            } else if (text.equals("/my_tasks") || text.equals("📌 Bosh og'riqlarim")) {
                taskCreationStates.remove(chatId);
                handleMyTasks(chatId);
            } else if (text.equals("/status") || text.equals("/stats") || text.equals("🏆 Mening maqomim")) {
                taskCreationStates.remove(chatId);
                handleStats(chatId, null);
            } else if (text.equals("/overdue")) {
                taskCreationStates.remove(chatId);
                handleOverdue(chatId);
            } else if (text.equals("/tasks_today")) {
                taskCreationStates.remove(chatId);
                handleTasksToday(chatId);
            } else if (text.equals("/app")) {
                taskCreationStates.remove(chatId);
                handleAppCommand(chatId);
            } else if (text.equals("/help") || text.equals("ℹ️ Yordam")) {
                handleHelp(chatId);
            } else if (text.equals("/unlink")) {
                taskCreationStates.remove(chatId);
                handleUnlink(chatId);
            } else {
                TaskCreationState state = taskCreationStates.get(chatId);
                if (state != null && !state.isExpired() && state.getStep() == TaskCreationStep.AWAITING_TITLE) {
                    handleTaskCreationTitleInput(chatId, text);
                } else {
                    taskCreationStates.remove(chatId);
                    sendMessage(chatId, "Kechirasiz, bu buyruqni tushunmadim. Quyidagi menyu tugmalaridan foydalaning yoki /help buyrug'ini bering.");
                }
            }
        }
    }

    private void handleStart(Long chatId, String text) {
        String[] parts = text.split(" ");
        if (parts.length > 1) {
            String token = parts[1].trim();
            if (token.startsWith("task_")) {
                handleTaskLink(chatId, token.substring(5));
                return;
            }

            RateLimit limit = startRateLimits.compute(chatId, (k, v) -> {
                LocalDateTime now = LocalDateTime.now();
                if (v == null || v.windowStart.isBefore(now.minusMinutes(15))) {
                    return new RateLimit(1, now);
                }
                v.attempts++;
                return v;
            });

            if (limit.attempts > 5) {
                sendMessage(chatId, "⚠️ Siz juda ko'p marotaba noto'g'ri urinish qildingiz. Iltimos, 15 daqiqadan so'ng qayta urinib ko'ring.");
                return;
            }

            Optional<User> userOpt = userRepository.findByTelegramLinkToken(token);
            if (userOpt.isPresent()) {
                User user = userOpt.get();
                if (user.getTelegramLinkTokenExpiresAt() != null && user.getTelegramLinkTokenExpiresAt().isBefore(LocalDateTime.now())) {
                    sendMessage(chatId, "⚠️ Token muddati eskirgan (15 daqiqa o'tib ketgan). Iltimos, web ilovadan yangi token oling.");
                    return;
                }

                Optional<User> alreadyLinked = userRepository.findByTelegramChatId(chatId);
                if (alreadyLinked.isPresent() && !alreadyLinked.get().getId().equals(user.getId())) {
                    sendMessage(chatId, "⚠️ Ushbu Telegram raqam allaqachon boshqa akkauntga ulangan. Bitta raqam faqat bitta akkauntga ulanishi mumkin.");
                    return;
                }

                user.setTelegramChatId(chatId);
                user.setTelegramLinkToken(null);
                user.setTelegramLinkTokenExpiresAt(null);
                userRepository.save(user);
                startRateLimits.remove(chatId);

                String welcomeMsg = "🎉 Davraga xush kelibsiz, <b>" + TelegramUtil.escapeHtml(user.getFullName() != null ? user.getFullName() : user.getName()) + "</b>!\n\n"
                        + "Akkauntingiz muvaffaqiyatli ulandi ☕️\n"
                        + "Endi barcha g'alva va bosh og'riqlardan (vazifalardan) xabardor bo'lib turasiz hamda ularni shu yerdan boshqara olasiz!";
                sendMessageWithMainKeyboard(chatId, welcomeMsg);
            } else {
                sendMessage(chatId, "⚠️ Noto'g'ri yoki muddati eskirgan token. Iltimos, web ilovaning 'Hujram' bo'limidan yangi token oling.");
            }
        } else {
            Optional<User> linkedOpt = userRepository.findByTelegramChatId(chatId);
            if (linkedOpt.isPresent()) {
                User user = linkedOpt.get();
                sendMessageWithMainKeyboard(chatId, "Assalomu alaykum, <b>" + TelegramUtil.escapeHtml(user.getName()) + "</b>! 'Choylashamiz' botiga xush kelibsiz. Nima qilamiz?");
            } else {
                String help = "☕️ Assalomu alaykum! 'Choylashamiz' tizimining rasmiy botiga xush kelibsiz.\n\n"
                        + "Telegram orqali bildirishnomalarni olish va vazifalaringizni boshqarish uchun akkauntingizni ulang:\n"
                        + "1. Shu yerdagi 'Bosh og'riq' tugmasini bosing (yoki /app buyrug'ini yuboring).\n"
                        + "2. Ochilgan Mini App ichida loginingiz va parolingiz bilan tizimga kiring, shunda hisobingiz avtomatik ulanadi.\n"
                        + "(Yoki web ilovadagi 'Hujram' sahifasidan ham ulanishingiz mumkin).";
                sendMessage(chatId, help);
            }
        }
    }

    private void handleTaskLink(Long chatId, String taskId) {
        Optional<User> linkedOpt = userRepository.findByTelegramChatId(chatId);
        if (linkedOpt.isEmpty()) {
            String help = "☕️ Assalomu alaykum! 'Choylashamiz' tizimining rasmiy botiga xush kelibsiz.\n\n"
                    + "Telegram orqali bildirishnomalarni olish va vazifalaringizni boshqarish uchun akkauntingizni ulang:\n"
                    + "1. Shu yerdagi 'Bosh og'riq' tugmasini bosing (yoki /app buyrug'ini yuboring).\n"
                    + "2. Ochilgan Mini App ichida loginingiz va parolingiz bilan tizimga kiring, shunda hisobingiz avtomatik ulanadi.\n"
                    + "(Yoki web ilovadagi 'Hujram' sahifasidan ham ulanishingiz mumkin).";
            sendMessage(chatId, help);
            return;
        }

        User user = linkedOpt.get();
        Optional<Task> taskOpt = taskRepository.findById(taskId);
        if (taskOpt.isEmpty()) {
            sendMessage(chatId, "Bu vazifaga ruxsatingiz yo'q.");
            return;
        }

        Task task = taskOpt.get();
        try {
            authorizationService.checkAccess(task.getWorkspaceId(), user);
        } catch (Exception e) {
            sendMessage(chatId, "Bu vazifaga ruxsatingiz yo'q.");
            return;
        }

        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setText("📌 Vazifa: <b>" + TelegramUtil.escapeHtml(task.getTitle()) + "</b>");
        message.setParseMode("HTML");
        message.setReplyMarkup(TelegramUtil.createTaskViewKeyboard(task.getId(), task.getTitle(), miniappBaseUrl));
        executeWithRetry(message, chatId, 0);
    }

    private void handleAppCommand(Long chatId) {
        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setText("Mini App orqali barcha vazifalarni qulay boshqaring 👇");
        
        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        InlineKeyboardButton btn = new InlineKeyboardButton();
        btn.setText("📱 Ochish");
        btn.setWebApp(new org.telegram.telegrambots.meta.api.objects.webapp.WebAppInfo(miniappBaseUrl + "/app/"));
        markup.setKeyboard(List.of(List.of(btn)));
        message.setReplyMarkup(markup);
        
        executeWithRetry(message, chatId, 0);
    }

    private void handleMyTasks(Long chatId) {
        Optional<User> userOpt = userRepository.findByTelegramChatId(chatId);
        if (userOpt.isEmpty()) {
            sendMessage(chatId, "Sizning akkauntingiz hali ulanmagan. Iltimos, web ilovaning 'Hujram' sahifasidan ulaning.");
            return;
        }

        List<Task> tasks = taskRepository.findActiveTasksByUserId(userOpt.get().getId());
        if (tasks.isEmpty()) {
            sendMessage(chatId, "Sizda hozircha birorta ham bosh og'riq (faol vazifa) yo'q. Qoyil, bemalol choylashib o'tirsangiz bo'ladi! ☕️");
        } else {
            sendTasksList(chatId, tasks);
        }
    }

    private void handleStatus(Long chatId) {
        handleStats(chatId, null);
    }

    private void handleStats(Long chatId, CallbackQuery callbackQuery) {
        Optional<User> userOpt = userRepository.findByTelegramChatId(chatId);
        if (userOpt.isEmpty()) {
            if (callbackQuery != null) {
                answerCallback(callbackQuery, null);
            }
            sendMessage(chatId, "Akkauntingiz ulanmagan. Iltimos, web ilovadan ulang.");
            return;
        }

        User user = userOpt.get();
        LocalDate today = LocalDate.now(clock);
        UserTaskStatsProjection stats = taskRepository.getUserTaskStats(user.getId(), today);

        long totalAssigned = stats != null && stats.getTotalAssigned() != null ? stats.getTotalAssigned() : 0L;
        long completedCount = stats != null && stats.getCompletedCount() != null ? stats.getCompletedCount() : 0L;
        long activeCount = stats != null && stats.getActiveCount() != null ? stats.getActiveCount() : 0L;
        long overdueCount = stats != null && stats.getOverdueCount() != null ? stats.getOverdueCount() : 0L;
        long dueTodayCount = stats != null && stats.getDueTodayCount() != null ? stats.getDueTodayCount() : 0L;

        String statusNickname = UserDto.calculateStatusNickname(totalAssigned);

        long currentRankThreshold = 0;
        long nextRankThreshold = 1;
        String nextRank = "Do'konga chopuvchi";

        if (totalAssigned < 1) {
            currentRankThreshold = 0;
            nextRankThreshold = 1;
            nextRank = "Do'konga chopuvchi";
        } else if (totalAssigned <= 10) {
            currentRankThreshold = 1;
            nextRankThreshold = 11;
            nextRank = "O'zimizdan";
        } else if (totalAssigned <= 25) {
            currentRankThreshold = 11;
            nextRankThreshold = 26;
            nextRank = "Ishonganimiz";
        } else if (totalAssigned <= 50) {
            currentRankThreshold = 26;
            nextRankThreshold = 51;
            nextRank = "Ko'cha ko'rgan";
        } else if (totalAssigned <= 100) {
            currentRankThreshold = 51;
            nextRankThreshold = 101;
            nextRank = "Katta uka";
        } else if (totalAssigned <= 200) {
            currentRankThreshold = 101;
            nextRankThreshold = 201;
            nextRank = "Katta aka";
        } else {
            nextRank = null;
        }

        int percentage = 100;
        long tasksNeeded = 0;
        if (nextRank != null) {
            long range = nextRankThreshold - currentRankThreshold;
            long progress = Math.max(0, totalAssigned - currentRankThreshold);
            percentage = (int) Math.min(100, Math.max(0, (progress * 100) / (range > 0 ? range : 1)));
            tasksNeeded = Math.max(0, nextRankThreshold - totalAssigned);
        }
        int filled = Math.min(10, Math.max(0, percentage / 10));
        String bar = "▓".repeat(filled) + "░".repeat(10 - filled);

        int completionRate = totalAssigned > 0 ? (int) Math.round(((double) completedCount / totalAssigned) * 100) : 0;

        StringBuilder sb = new StringBuilder();
        sb.append("📊 <b>Shaxsiy unumdorlik statistikasi</b>\n\n");
        sb.append("👤 Foydalanuvchi: <b>").append(TelegramUtil.escapeHtml(user.getFullName() != null ? user.getFullName() : user.getName())).append("</b>\n");
        sb.append("🏆 Maqomingiz: <b>").append(TelegramUtil.escapeHtml(statusNickname)).append("</b>\n");
        if (nextRank != null) {
            sb.append("📈 Keyingi darajaga ('").append(TelegramUtil.escapeHtml(nextRank)).append("'): [<b>").append(bar).append("</b>] ").append(percentage).append("% (yana ").append(tasksNeeded).append(" ta)\n\n");
        } else {
            sb.append("👑 Siz allaqachon eng oliy darajadasiz ('Katta aka')!\n\n");
        }

        sb.append("📌 <b>Vazifalar holati:</b>\n");
        sb.append("• Jami biriktirilgan: <b>").append(totalAssigned).append(" ta</b>\n");
        sb.append("• 🟢 Faol (jarayonda): <b>").append(activeCount).append(" ta</b>\n");
        sb.append("• ✅ Bajarilgan: <b>").append(completedCount).append(" ta</b> (Samaradorlik: <b>").append(completionRate).append("%</b>)\n");
        sb.append("• ⏰ Muddati o'tgan: <b>").append(overdueCount).append(" ta</b>\n");
        sb.append("• 📅 Bugun kutilayotgan: <b>").append(dueTodayCount).append(" ta</b>");

        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();

        List<InlineKeyboardButton> row1 = new ArrayList<>();
        if (overdueCount > 0) {
            InlineKeyboardButton overdueBtn = new InlineKeyboardButton();
            overdueBtn.setText("⏰ Muddati o'tganlar (" + overdueCount + ")");
            overdueBtn.setCallbackData("STATS_OVERDUE");
            row1.add(overdueBtn);
        }
        if (dueTodayCount > 0) {
            InlineKeyboardButton todayBtn = new InlineKeyboardButton();
            todayBtn.setText("📅 Bugungilar (" + dueTodayCount + ")");
            todayBtn.setCallbackData("STATS_TODAY");
            row1.add(todayBtn);
        }
        if (!row1.isEmpty()) {
            rows.add(row1);
        }

        List<InlineKeyboardButton> row2 = new ArrayList<>();
        InlineKeyboardButton myTasksBtn = new InlineKeyboardButton();
        myTasksBtn.setText("📌 Faol vazifalar");
        myTasksBtn.setCallbackData("STATS_ACTIVE");
        row2.add(myTasksBtn);

        InlineKeyboardButton refreshBtn = new InlineKeyboardButton();
        refreshBtn.setText("🔄 Yangilash");
        refreshBtn.setCallbackData("STATS_REFRESH");
        row2.add(refreshBtn);
        rows.add(row2);

        markup.setKeyboard(rows);

        if (callbackQuery != null && callbackQuery.getMessage() != null && callbackQuery.getMessage().getMessageId() != null) {
            answerCallback(callbackQuery, "Statistika yangilandi 🔄");
            try {
                EditMessageText edit = new EditMessageText();
                edit.setChatId(String.valueOf(chatId));
                edit.setMessageId(callbackQuery.getMessage().getMessageId());
                edit.setText(sb.toString());
                edit.setParseMode("HTML");
                edit.setReplyMarkup(markup);
                execute(edit);
            } catch (TelegramApiException e) {
                sendMessageWithInlineKeyboard(chatId, sb.toString(), markup);
            }
        } else {
            sendMessageWithInlineKeyboard(chatId, sb.toString(), markup);
        }
    }

    private void handleOverdue(Long chatId) {
        Optional<User> userOpt = userRepository.findByTelegramChatId(chatId);
        if (userOpt.isEmpty()) {
            sendMessage(chatId, "Sizning akkauntingiz hali ulanmagan. Iltimos, web ilovadan ulaning.");
            return;
        }

        LocalDate today = LocalDate.now(clock);
        List<Task> overdueTasks = taskRepository.findOverdueTasksByUserId(userOpt.get().getId(), today);
        if (overdueTasks.isEmpty()) {
            sendMessage(chatId, "🎉 Ajoyib! Sizda birorta ham muddati o'tgan bosh og'riq yo'q. Qozon tinch! ☕️");
        } else {
            sendTasksList(chatId, overdueTasks, "⏰ <b>Muddati o'tgan vazifalar ro'yxati:</b>\n(Batafsil ko'rish uchun tanlang)");
        }
    }

    private void handleTasksToday(Long chatId) {
        Optional<User> userOpt = userRepository.findByTelegramChatId(chatId);
        if (userOpt.isEmpty()) {
            sendMessage(chatId, "Sizning akkauntingiz hali ulanmagan. Iltimos, web ilovadan ulaning.");
            return;
        }

        LocalDate today = LocalDate.now(clock);
        List<Task> todayTasks = taskRepository.findDueTodayTasksByUserId(userOpt.get().getId(), today);
        if (todayTasks.isEmpty()) {
            sendMessage(chatId, "📅 Bugun muddati tugaydigan shoshilinch vazifalar yo'q. ☕️");
        } else {
            sendTasksList(chatId, todayTasks, "📅 <b>Bugun bajarilishi kerak bo'lgan vazifalar:</b>\n(Batafsil ko'rish uchun tanlang)");
        }
    }

    private void handleCreateTaskCommand(Long chatId, String initialTitle) {
        Optional<User> userOpt = userRepository.findByTelegramChatId(chatId);
        if (userOpt.isEmpty()) {
            sendMessage(chatId, "Sizning akkauntingiz hali ulanmagan. Iltimos, web ilovaning 'Hujram' sahifasidan ulaning.");
            return;
        }

        User user = userOpt.get();
        List<Workspace> workspaces = workspaceRepository.findByOwnerIdOrMemberUserId(user.getId());
        if (workspaces.isEmpty()) {
            sendMessage(chatId, "⚠️ Sizda hali birorta ham faol g'alva yo'q. Avval web ilovada yangi g'alva yarating.");
            return;
        }

        if (initialTitle != null && !initialTitle.isBlank()) {
            String title = initialTitle.trim();
            if (title.length() > 255) {
                title = title.substring(0, 255);
            }

            if (workspaces.size() == 1) {
                completeTaskCreation(chatId, user, workspaces.get(0).getId(), title, Priority.MEDIUM, null);
            } else {
                TaskCreationState state = new TaskCreationState(TaskCreationStep.AWAITING_WORKSPACE);
                state.setTitle(title);
                taskCreationStates.put(chatId, state);
                sendWorkspaceSelectionKeyboard(chatId, workspaces, title);
            }
        } else {
            TaskCreationState state = new TaskCreationState(TaskCreationStep.AWAITING_TITLE);
            taskCreationStates.put(chatId, state);
            sendMessage(chatId, "📝 <b>Yangi vazifa yaratish</b>\n\nIltimos, vazifa (bosh og'riq) nomini yozib yuboring:\n<i>(Bekor qilish uchun /cancel deb yozing)</i>");
        }
    }

    private void handleTaskCreationTitleInput(Long chatId, String title) {
        Optional<User> userOpt = userRepository.findByTelegramChatId(chatId);
        if (userOpt.isEmpty()) {
            taskCreationStates.remove(chatId);
            sendMessage(chatId, "Akkauntingiz ulanmagan. Iltimos, web ilovadan ulang.");
            return;
        }

        User user = userOpt.get();
        String cleanTitle = title.trim();
        if (cleanTitle.isBlank()) {
            sendMessage(chatId, "Vazifa nomi bo'sh bo'lishi mumkin emas. Iltimos, nomini kiriting:");
            return;
        }
        if (cleanTitle.length() > 255) {
            cleanTitle = cleanTitle.substring(0, 255);
        }

        List<Workspace> workspaces = workspaceRepository.findByOwnerIdOrMemberUserId(user.getId());
        if (workspaces.isEmpty()) {
            taskCreationStates.remove(chatId);
            sendMessage(chatId, "⚠️ Sizda faol g'alva yo'q. Vazifa yaratish bekor qilindi.");
            return;
        }

        TaskCreationState state = taskCreationStates.get(chatId);
        if (state == null) {
            state = new TaskCreationState(TaskCreationStep.AWAITING_PRIORITY);
        }
        state.setTitle(cleanTitle);

        if (workspaces.size() == 1) {
            state.setWorkspaceId(workspaces.get(0).getId());
            state.setStep(TaskCreationStep.AWAITING_PRIORITY);
            sendPrioritySelectionKeyboard(chatId, workspaces.get(0).getTitle(), cleanTitle);
        } else {
            state.setStep(TaskCreationStep.AWAITING_WORKSPACE);
            sendWorkspaceSelectionKeyboard(chatId, workspaces, cleanTitle);
        }
    }

    private void sendWorkspaceSelectionKeyboard(Long chatId, List<Workspace> workspaces, String title) {
        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setText("📋 Vazifa: <b>" + TelegramUtil.escapeHtml(title) + "</b>\n\n🏢 Ushbu vazifani qaysi g'alvaga qo'shamiz?");
        message.setParseMode("HTML");

        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();

        int limit = Math.min(workspaces.size(), 10);
        for (int i = 0; i < limit; i++) {
            Workspace ws = workspaces.get(i);
            List<InlineKeyboardButton> row = new ArrayList<>();
            InlineKeyboardButton btn = new InlineKeyboardButton();
            btn.setText("🏢 " + ws.getTitle());
            btn.setCallbackData("CT_WS_" + ws.getId());
            row.add(btn);
            rows.add(row);
        }

        List<InlineKeyboardButton> cancelRow = new ArrayList<>();
        InlineKeyboardButton cancelBtn = new InlineKeyboardButton();
        cancelBtn.setText("❌ Bekor qilish");
        cancelBtn.setCallbackData("CT_CANCEL");
        cancelRow.add(cancelBtn);
        rows.add(cancelRow);

        markup.setKeyboard(rows);
        message.setReplyMarkup(markup);

        executeWithRetry(message, chatId, 0);
    }

    private void sendPrioritySelectionKeyboard(Long chatId, String workspaceTitle, String taskTitle) {
        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setText("📋 Vazifa: <b>" + TelegramUtil.escapeHtml(taskTitle) + "</b>\n"
                + "🏢 G'alva: <b>" + TelegramUtil.escapeHtml(workspaceTitle) + "</b>\n\n"
                + "⚠️ Muhimlik darajasini tanlang:");
        message.setParseMode("HTML");

        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();

        List<InlineKeyboardButton> row1 = new ArrayList<>();
        InlineKeyboardButton lowBtn = new InlineKeyboardButton();
        lowBtn.setText("🟢 Oddiy (LOW)");
        lowBtn.setCallbackData("CT_PRIO_LOW");
        row1.add(lowBtn);

        InlineKeyboardButton medBtn = new InlineKeyboardButton();
        medBtn.setText("🟡 O'rtacha (MEDIUM)");
        medBtn.setCallbackData("CT_PRIO_MEDIUM");
        row1.add(medBtn);
        rows.add(row1);

        List<InlineKeyboardButton> row2 = new ArrayList<>();
        InlineKeyboardButton highBtn = new InlineKeyboardButton();
        highBtn.setText("🔴 Yuqori (HIGH)");
        highBtn.setCallbackData("CT_PRIO_HIGH");
        row2.add(highBtn);

        InlineKeyboardButton urgentBtn = new InlineKeyboardButton();
        urgentBtn.setText("🔥 Shoshilinch (URGENT)");
        urgentBtn.setCallbackData("CT_PRIO_URGENT");
        row2.add(urgentBtn);
        rows.add(row2);

        List<InlineKeyboardButton> cancelRow = new ArrayList<>();
        InlineKeyboardButton cancelBtn = new InlineKeyboardButton();
        cancelBtn.setText("❌ Bekor qilish");
        cancelBtn.setCallbackData("CT_CANCEL");
        cancelRow.add(cancelBtn);
        rows.add(cancelRow);

        markup.setKeyboard(rows);
        message.setReplyMarkup(markup);

        executeWithRetry(message, chatId, 0);
    }

    private void completeTaskCreation(Long chatId, User user, String workspaceId, String title,
                                      Priority priority, CallbackQuery callbackQuery) {
        taskCreationStates.remove(chatId);

        try {
            authorizationService.checkCanEdit(workspaceId, user);
        } catch (Exception e) {
            if (callbackQuery != null) {
                answerCallback(callbackQuery, "Ruxsat yo'q");
            }
            sendMessage(chatId, "⚠️ Ushbu g'alvada vazifa yaratish uchun ruxsatingiz yo'q.");
            return;
        }

        List<BoardColumn> columns = columnRepository.findByWorkspaceIdOrderByOrderAsc(workspaceId);
        if (columns.isEmpty()) {
            if (callbackQuery != null) {
                answerCallback(callbackQuery, "Ustunlar topilmadi");
            }
            sendMessage(chatId, "⚠️ Ushbu g'alvada bosh og'riq ustunlari mavjud emas.");
            return;
        }

        BoardColumn targetColumn = columns.stream()
                .filter(c -> Boolean.FALSE.equals(c.getIsDone()))
                .findFirst()
                .orElse(columns.get(0));

        String maxRank = taskRepository.findMaxLexoRankByColumnId(targetColumn.getId());
        String lexoRank = LexoRankUtil.getMiddle(maxRank, null);

        Task task = Task.builder()
                .workspaceId(workspaceId)
                .columnId(targetColumn.getId())
                .title(title)
                .priority(priority != null ? priority : Priority.MEDIUM)
                .issueType(IssueType.TASK)
                .lexoRank(lexoRank)
                .build();
        task.getAssignees().add(user);

        Task saved = taskRepository.save(task);

        activityService.logActivity(saved.getId(), user, TaskActivityType.TASK_CREATED, "task", null, saved.getTitle());

        if (webSocketNotifier != null) {
            webSocketNotifier.notifyWorkspace(workspaceId, WebSocketEvent.builder()
                    .type("TASK_CREATED")
                    .workspaceId(workspaceId)
                    .data(TaskDto.fromEntity(saved))
                    .build());
        }

        Optional<Workspace> wsOpt = workspaceRepository.findById(workspaceId);
        String wsTitle = wsOpt.map(Workspace::getTitle).orElse("G'alva");

        String msg = "🎉 <b>Yangi vazifa muvaffaqiyatli yaratildi!</b>\n\n"
                + "📋 Sarlavha: <b>" + TelegramUtil.escapeHtml(saved.getTitle()) + "</b>\n"
                + "🏢 G'alva: <b>" + TelegramUtil.escapeHtml(wsTitle) + "</b>\n"
                + "📊 Holati: <b>" + TelegramUtil.escapeHtml(targetColumn.getTitle()) + "</b>\n"
                + "⚠️ Muhimligi: <b>" + saved.getPriority().name() + "</b>\n\n"
                + "Vazifa sizga avtomatik biriktirildi. ☕️";

        InlineKeyboardMarkup markup = TelegramUtil.createTaskViewKeyboard(saved.getId(), saved.getTitle(), miniappBaseUrl);

        if (callbackQuery != null && callbackQuery.getMessage() != null && callbackQuery.getMessage().getMessageId() != null) {
            answerCallback(callbackQuery, "Vazifa yaratildi ✅");
            try {
                EditMessageText edit = new EditMessageText();
                edit.setChatId(String.valueOf(chatId));
                edit.setMessageId(callbackQuery.getMessage().getMessageId());
                edit.setText(msg);
                edit.setParseMode("HTML");
                edit.setReplyMarkup(markup);
                execute(edit);
            } catch (TelegramApiException e) {
                sendMessageWithInlineKeyboard(chatId, msg, markup);
            }
        } else {
            sendMessageWithInlineKeyboard(chatId, msg, markup);
        }
    }

    private void handleHelp(Long chatId) {
        String help = "📌 <b>Botdagi mavjud buyruqlar:</b>\n\n"
                + "🔹 ➕ <b>Yangi vazifa</b> (/create_task) — Yangi bosh og'riq (vazifa) yaratish va o'zingizga biriktirish\n"
                + "🔹 📌 <b>Bosh og'riqlarim</b> (/my_tasks) — Sizga biriktirilgan faol vazifalarni ko'rish va boshqarish\n"
                + "🔹 🏆 <b>Mening maqomim & Statistika</b> (/stats, /status) — Shaxsiy unumdorlik, maqom va darajangiz\n"
                + "🔹 ⏰ <b>Muddati o'tganlar</b> (/overdue) — Muddati o'tib ketgan vazifalar ro'yxati\n"
                + "🔹 📅 <b>Bugungi vazifalar</b> (/tasks_today) — Bugun bajarilishi kerak bo'lgan vazifalar\n"
                + "🔹 📱 <b>Mini App</b> (/app) — Web ilovani ochish\n"
                + "🔹 ❌ <b>Bekor qilish</b> (/cancel) — Boshlangan vazifa yaratish jarayonini bekor qilish\n"
                + "🔹 ℹ️ <b>Yordam</b> (/help) — Ushbu qo'llanma\n"
                + "🔹 🚪 <b>Akkauntni uzish</b> (/unlink) — Telegram akkauntingizni tizimdan uzish";
        sendMessage(chatId, help);
    }

    private void handleUnlink(Long chatId) {
        Optional<User> userOpt = userRepository.findByTelegramChatId(chatId);
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            SendMessage message = new SendMessage();
            message.setChatId(String.valueOf(chatId));
            message.setText("Haqiqatan ham akkauntingizni tizimdan uzmoqchimisiz?");
            message.setParseMode("HTML");

            InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
            List<InlineKeyboardButton> row = new ArrayList<>();
            InlineKeyboardButton yesBtn = new InlineKeyboardButton();
            yesBtn.setText("Ha, uzish");
            yesBtn.setCallbackData("UNLINK_YES_" + user.getId());

            InlineKeyboardButton noBtn = new InlineKeyboardButton();
            noBtn.setText("Bekor qilish");
            noBtn.setCallbackData("UNLINK_NO");

            row.add(yesBtn);
            row.add(noBtn);
            markup.setKeyboard(List.of(row));
            message.setReplyMarkup(markup);

            executeWithRetry(message, chatId, 0);
        } else {
            sendMessage(chatId, "Sizning akkauntingiz allaqachon ulanmagan.");
        }
    }

    private void sendTasksList(Long chatId, List<Task> tasks) {
        sendTasksList(chatId, tasks, "Sizning faol bosh og'riqlaringiz (vazifalar) ro'yxati:\n(Batafsil ko'rish uchun tanlang)");
    }

    private void sendTasksList(Long chatId, List<Task> tasks, String titleHeader) {
        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setText(titleHeader);

        InlineKeyboardMarkup markupInline = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rowsInline = new ArrayList<>();

        for (Task t : tasks) {
            List<InlineKeyboardButton> rowInline = new ArrayList<>();
            InlineKeyboardButton button = new InlineKeyboardButton();
            button.setText("📌 " + t.getTitle());
            button.setCallbackData("TASK_VIEW_" + t.getId());
            rowInline.add(button);
            rowsInline.add(rowInline);

            List<InlineKeyboardButton> webAppRow = new ArrayList<>();
            InlineKeyboardButton webAppBtn = new InlineKeyboardButton();
            webAppBtn.setText("📱 Ochish");
            webAppBtn.setWebApp(new org.telegram.telegrambots.meta.api.objects.webapp.WebAppInfo(miniappBaseUrl + "/app/?task=" + t.getId()));
            webAppRow.add(webAppBtn);
            rowsInline.add(webAppRow);
        }

        markupInline.setKeyboard(rowsInline);
        message.setReplyMarkup(markupInline);
        message.setParseMode("HTML");

        executeWithRetry(message, chatId, 0);
    }

    private void handleCallbackQuery(CallbackQuery callbackQuery) {
        String callData = callbackQuery.getData();
        Long chatId = callbackQuery.getMessage().getChatId();

        if (!callData.startsWith("TASK_DONE_") && !callData.startsWith("TASK_SNOOZE_")
                && !callData.startsWith("STATS_") && !callData.startsWith("CT_")) {
            answerCallback(callbackQuery, null);
        }

        Optional<User> userOpt = userRepository.findByTelegramChatId(chatId);
        if (userOpt.isEmpty()) {
            if (callData.startsWith("TASK_DONE_") || callData.startsWith("TASK_SNOOZE_")
                    || callData.startsWith("STATS_") || callData.startsWith("CT_")) {
                answerCallback(callbackQuery, null);
            }
            sendMessage(chatId, "Sizning akkauntingiz hali ulanmagan. Iltimos, ulanish uchun web ilovadan token oling.");
            return;
        }
        User user = userOpt.get();

        if (callData.startsWith("TASK_VIEW_")) {
            String taskId = callData.substring(10);
            Optional<Task> taskOpt = taskRepository.findById(taskId);

            if (taskOpt.isPresent()) {
                Task task = taskOpt.get();
                
                try {
                    authorizationService.checkAccess(task.getWorkspaceId(), user);
                } catch (Exception e) {
                    sendMessage(chatId, "⚠️ Ushbu vazifa bo'yicha ruxsatingiz yo'q (G'alvaga a'zo emassiz).");
                    return;
                }

                Optional<BoardColumn> colOpt = columnRepository.findById(task.getColumnId());
                String colName = colOpt.isPresent() ? colOpt.get().getTitle() : "Noma'lum";

                String workspaceName = "Mavjud emas";
                if (task.getWorkspaceId() != null) {
                    Optional<Workspace> wsOpt = workspaceRepository.findById(task.getWorkspaceId());
                    if (wsOpt.isPresent()) {
                        workspaceName = wsOpt.get().getTitle();
                    }
                }

                String details = buildTaskViewText(task, colName, workspaceName);

                SendMessage message = new SendMessage();
                message.setChatId(String.valueOf(chatId));
                message.setText(details);
                message.setParseMode("HTML");
                message.setReplyMarkup(buildTaskViewKeyboard(task));

                executeWithRetry(message, chatId, 0);
            } else {
                sendMessage(chatId, "Bosh og'riq (vazifa) topilmadi.");
            }
        } else if (callData.startsWith("TASK_STATUS_SELECT_")) {
            String taskId = callData.substring(19);
            Optional<Task> taskOpt = taskRepository.findById(taskId);

            if (taskOpt.isPresent()) {
                Task task = taskOpt.get();
                
                try {
                    authorizationService.checkAccess(task.getWorkspaceId(), user);
                } catch (Exception e) {
                    sendMessage(chatId, "⚠️ Ushbu g'alvaga a'zo emassiz, ruxsat yo'q.");
                    return;
                }

                List<BoardColumn> columns = columnRepository.findByWorkspaceIdOrderByOrderAsc(task.getWorkspaceId());

                SendMessage message = new SendMessage();
                message.setChatId(String.valueOf(chatId));
                message.setText("Ushbu vazifani qaysi ustunga o'tkazmoqchisiz?");

                InlineKeyboardMarkup markupInline = new InlineKeyboardMarkup();
                List<List<InlineKeyboardButton>> rowsInline = new ArrayList<>();

                for (int i = 0; i < columns.size(); i++) {
                    BoardColumn col = columns.get(i);
                    List<InlineKeyboardButton> rowInline = new ArrayList<>();
                    InlineKeyboardButton btn = new InlineKeyboardButton();
                    btn.setText(col.getTitle());
                    int orderVal = col.getOrder() != null ? col.getOrder() : (i + 1);
                    btn.setCallbackData("TASK_STATUS_CHANGE_" + taskId + "_" + orderVal);
                    rowInline.add(btn);
                    rowsInline.add(rowInline);
                }

                markupInline.setKeyboard(rowsInline);
                message.setReplyMarkup(markupInline);
                message.setParseMode("HTML");

                executeWithRetry(message, chatId, 0);
            } else {
                sendMessage(chatId, "Vazifa topilmadi.");
            }
        } else if (callData.startsWith("TASK_STATUS_CHANGE_")) {
            String remainder = callData.substring(19);
            int lastUnderscore = remainder.lastIndexOf('_');
            if (lastUnderscore == -1) {
                sendMessage(chatId, "Bu tugma eskirgan, xabarni qayta oching");
                return;
            }
            String taskId = remainder.substring(0, lastUnderscore);
            String orderStr = remainder.substring(lastUnderscore + 1);

            int orderNum;
            try {
                orderNum = Integer.parseInt(orderStr);
            } catch (NumberFormatException e) {
                sendMessage(chatId, "Bu tugma eskirgan, xabarni qayta oching");
                return;
            }

            Optional<Task> taskOpt = taskRepository.findById(taskId);
            if (taskOpt.isEmpty()) {
                sendMessage(chatId, "Vazifa topilmadi.");
                return;
            }
            Task task = taskOpt.get();

            try {
                authorizationService.checkAccess(task.getWorkspaceId(), user);
                authorizationService.checkCanEdit(task.getWorkspaceId(), user);
            } catch (Exception e) {
                sendMessage(chatId, "⚠️ Ruxsat yo'q.");
                return;
            }

            List<BoardColumn> columns = columnRepository.findByWorkspaceIdOrderByOrderAsc(task.getWorkspaceId());
            BoardColumn targetCol = null;
            for (BoardColumn col : columns) {
                if (col.getOrder() != null && col.getOrder().equals(orderNum)) {
                    targetCol = col;
                    break;
                }
            }
            if (targetCol == null) {
                if (orderNum >= 1 && orderNum <= columns.size()) {
                    targetCol = columns.get(orderNum - 1);
                } else if (orderNum >= 0 && orderNum < columns.size()) {
                    targetCol = columns.get(orderNum);
                }
            }

            if (targetCol == null) {
                sendMessage(chatId, "Yangi ustun topilmadi.");
                return;
            }

            if (targetCol.getWorkspaceId() == null || !targetCol.getWorkspaceId().equals(task.getWorkspaceId())) {
                sendMessage(chatId, "⚠️ Noto'g'ri ustun (boshqa g'alvaga tegishli).");
                return;
            }

            task.setColumnId(targetCol.getId());
            taskRepository.save(task);
            sendMessage(chatId, "✅ Vazifa holati muvaffaqiyatli o'zgartirildi: " + targetCol.getTitle());
        } else if (callData.equals("STATS_REFRESH")) {
            handleStats(chatId, callbackQuery);
        } else if (callData.equals("STATS_OVERDUE")) {
            answerCallback(callbackQuery, null);
            handleOverdue(chatId);
        } else if (callData.equals("STATS_TODAY")) {
            answerCallback(callbackQuery, null);
            handleTasksToday(chatId);
        } else if (callData.equals("STATS_ACTIVE")) {
            answerCallback(callbackQuery, null);
            handleMyTasks(chatId);
        } else if (callData.equals("CT_CANCEL")) {
            taskCreationStates.remove(chatId);
            answerCallback(callbackQuery, "Bekor qilindi");
            if (callbackQuery.getMessage() != null && callbackQuery.getMessage().getMessageId() != null) {
                try {
                    EditMessageText edit = new EditMessageText();
                    edit.setChatId(String.valueOf(chatId));
                    edit.setMessageId(callbackQuery.getMessage().getMessageId());
                    edit.setText("❌ Vazifa yaratish bekor qilindi.");
                    execute(edit);
                } catch (Exception e) {
                    sendMessage(chatId, "❌ Vazifa yaratish bekor qilindi.");
                }
            } else {
                sendMessage(chatId, "❌ Vazifa yaratish bekor qilindi.");
            }
        } else if (callData.startsWith("CT_WS_")) {
            String wsId = callData.substring(6);
            TaskCreationState state = taskCreationStates.get(chatId);
            if (state == null || state.isExpired()) {
                taskCreationStates.remove(chatId);
                answerCallback(callbackQuery, "Jarayon eskirgan");
                sendMessage(chatId, "⚠️ Jarayon muddati o'tgan. Iltimos, /create_task buyrug'ini qaytadan bering.");
                return;
            }
            try {
                authorizationService.checkAccess(wsId, user);
                authorizationService.checkCanEdit(wsId, user);
            } catch (Exception e) {
                answerCallback(callbackQuery, "Ruxsat yo'q");
                sendMessage(chatId, "⚠️ Ushbu g'alvada ruxsatingiz yo'q.");
                return;
            }
            state.setWorkspaceId(wsId);
            state.setStep(TaskCreationStep.AWAITING_PRIORITY);
            answerCallback(callbackQuery, null);
            Optional<Workspace> wsOpt = workspaceRepository.findById(wsId);
            String wsTitle = wsOpt.map(Workspace::getTitle).orElse("Tanlangan g'alva");
            sendPrioritySelectionKeyboard(chatId, wsTitle, state.getTitle());
        } else if (callData.startsWith("CT_PRIO_")) {
            String prioStr = callData.substring(8);
            TaskCreationState state = taskCreationStates.get(chatId);
            if (state == null || state.isExpired() || state.getWorkspaceId() == null) {
                taskCreationStates.remove(chatId);
                answerCallback(callbackQuery, "Jarayon eskirgan");
                sendMessage(chatId, "⚠️ Jarayon muddati o'tgan. Iltimos, /create_task buyrug'ini qaytadan bering.");
                return;
            }
            Priority priority;
            try {
                priority = Priority.valueOf(prioStr);
            } catch (Exception e) {
                priority = Priority.MEDIUM;
            }
            completeTaskCreation(chatId, user, state.getWorkspaceId(), state.getTitle(), priority, callbackQuery);
        } else if (callData.startsWith("TASK_DONE_")) {
            handleTaskDone(callbackQuery, chatId, user, callData.substring(10));
        } else if (callData.startsWith("TASK_SNOOZE_")) {
            handleTaskSnooze(callbackQuery, chatId, user, callData.substring(12));
        } else if (callData.startsWith("UNLINK_YES_")) {
            String targetUserId = callData.substring(11);
            if (targetUserId.isBlank() || !user.getId().equals(targetUserId)) {
                sendMessage(chatId, "⚠️ Xatolik: Ruxsat yo'q.");
                return;
            }
            user.setTelegramChatId(null);
            userRepository.save(user);
            sendMessage(chatId, "Telegram akkauntingiz tizimdan muvaffaqiyatli uzildi. Qayta ulash uchun web ilovadan token oling.");
        } else if (callData.equals("UNLINK_NO")) {
            sendMessage(chatId, "Amaliyot bekor qilindi.");
        } else {
            sendMessage(chatId, "Bu tugma eskirgan, xabarni qayta oching");
        }
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void handleTelegramMessageEvent(TelegramMessageEvent event) {
        if (event.keyboard() != null) {
            sendMessageWithInlineKeyboard(event.chatId(), event.message(), event.keyboard());
        } else {
            sendMessage(event.chatId(), event.message());
        }
    }

    public void sendMessage(Long chatId, String text) {
        if (chatId == null) return;
        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setText(text);
        message.setParseMode("HTML");
        executeWithRetry(message, chatId, 0);
    }

    public void sendMessageWithMainKeyboard(Long chatId, String text) {
        if (chatId == null) return;
        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setText(text);
        message.setParseMode("HTML");
        message.setReplyMarkup(getMainKeyboard());
        executeWithRetry(message, chatId, 0);
    }

    public void sendMessageWithInlineKeyboard(Long chatId, String text, InlineKeyboardMarkup keyboard) {
        if (chatId == null) return;
        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setText(text);
        message.setParseMode("HTML");
        message.setReplyMarkup(keyboard);
        executeWithRetry(message, chatId, 0);
    }

    private void executeWithRetry(SendMessage message, Long chatId, int attempt) {
        try {
            execute(message);
        } catch (org.telegram.telegrambots.meta.exceptions.TelegramApiRequestException e) {
            if (e.getErrorCode() != null && e.getErrorCode() == 429 && attempt == 0) {
                int retryAfter = (e.getParameters() != null && e.getParameters().getRetryAfter() != null) 
                        ? e.getParameters().getRetryAfter() : 5;
                CompletableFuture.runAsync(() -> {
                    try {
                        executeWithRetry(message, chatId, attempt + 1);
                    } catch (Exception ex) {
                        handleTelegramError(chatId, ex.getMessage());
                    }
                }, CompletableFuture.delayedExecutor(retryAfter, java.util.concurrent.TimeUnit.SECONDS, taskExecutor));
            } else {
                handleTelegramError(chatId, e.getMessage());
            }
        } catch (TelegramApiException e) {
            handleTelegramError(chatId, e.getMessage());
        }
    }

    private void handleTelegramError(Long chatId, String msg) {
        if (msg == null) msg = "";
        msg = msg.toLowerCase();
        if (msg.contains("forbidden") || msg.contains("403") || msg.contains("chat not found")) {
            log.warn("Foydalanuvchi botni bloklagan yoki chat topilmadi. chatId={} o'chirilmoqda. {}", chatId, msg);
            userRepository.findByTelegramChatId(chatId).ifPresent(u -> {
                u.setTelegramChatId(null);
                userRepository.save(u);
            });
        } else {
            log.error("Telegram xabar yuborishda xatolik: {}", msg);
        }
    }

    private void handleTaskDone(CallbackQuery callbackQuery, Long chatId, User user, String taskId) {
        Optional<Task> taskOpt = taskRepository.findById(taskId);
        if (taskOpt.isEmpty()) {
            answerCallback(callbackQuery, "Vazifa topilmadi.");
            return;
        }
        Task task = taskOpt.get();

        if (Boolean.TRUE.equals(task.getIsArchived()) || task.getDeletedAt() != null) {
            answerCallback(callbackQuery, "⚠️ Vazifa arxivlangan yoki o'chirilgan.");
            return;
        }

        try {
            authorizationService.checkCanEdit(task.getWorkspaceId(), user);
        } catch (Exception e) {
            answerCallback(callbackQuery, "⚠️ Ruxsat yo'q.");
            return;
        }

        List<BoardColumn> columns = columnRepository.findByWorkspaceIdOrderByOrderAsc(task.getWorkspaceId());
        BoardColumn doneColumn = columns.stream()
                .filter(c -> Boolean.TRUE.equals(c.getIsDone()))
                .findFirst()
                .orElse(null);

        if (doneColumn == null) {
            answerCallback(callbackQuery, "Bu g'alvada 'bajarilgan' ustun belgilanmagan.");
            return;
        }

        if (doneColumn.getId().equals(task.getColumnId())) {
            answerCallback(callbackQuery, "Allaqachon bajarilgan ✅");
            return;
        }

        String oldColId = task.getColumnId();
        BoardColumn oldColumn = columns.stream()
                .filter(c -> c.getId().equals(oldColId))
                .findFirst()
                .orElse(null);
        String oldColumnTitle = oldColumn != null ? oldColumn.getTitle() : oldColId;

        task.setColumnId(doneColumn.getId());
        String maxRank = taskRepository.findMaxLexoRankByColumnId(doneColumn.getId());
        task.setLexoRank(com.taskcenter.util.LexoRankUtil.getMiddle(maxRank, null));
        taskRepository.save(task);

        activityService.logActivity(task.getId(), user, TaskActivityType.STATUS_UPDATED,
                "columnId", oldColId, doneColumn.getId());

        telegramNotificationService.sendTaskMovedNotification(task, user, oldColumnTitle, doneColumn.getTitle());

        answerCallback(callbackQuery, "✅ Bajarildi!");
        editTaskViewMessage(callbackQuery, chatId, task);
    }

    private void handleTaskSnooze(CallbackQuery callbackQuery, Long chatId, User user, String taskId) {
        Optional<Task> taskOpt = taskRepository.findById(taskId);
        if (taskOpt.isEmpty()) {
            answerCallback(callbackQuery, "Vazifa topilmadi.");
            return;
        }
        Task task = taskOpt.get();

        if (Boolean.TRUE.equals(task.getIsArchived()) || task.getDeletedAt() != null) {
            answerCallback(callbackQuery, "⚠️ Vazifa arxivlangan yoki o'chirilgan.");
            return;
        }

        try {
            authorizationService.checkCanEdit(task.getWorkspaceId(), user);
        } catch (Exception e) {
            answerCallback(callbackQuery, "⚠️ Ruxsat yo'q.");
            return;
        }

        LocalDate tomorrow = LocalDate.now(clock).plusDays(1);
        if (task.getDueDate() != null && task.getDueDate().isAfter(tomorrow)) {
            answerCallback(callbackQuery, "Muddat allaqachon " + task.getDueDate() + " (ertadan keyin).");
            return;
        }

        LocalDate oldDate = task.getDueDate();
        task.setDueDate(tomorrow);
        taskRepository.save(task);

        telegramReminderLogRepository.deleteByTaskId(task.getId());

        activityService.logActivity(task.getId(), user, TaskActivityType.DUE_DATE_SNOOZED,
                "dueDate", String.valueOf(oldDate), String.valueOf(tomorrow));

        answerCallback(callbackQuery, "📅 Muddat ertaga surildi!");
        editTaskViewMessage(callbackQuery, chatId, task);
    }

    private void answerCallback(CallbackQuery callbackQuery, String text) {
        if (callbackQuery.getId() == null) return;
        try {
            AnswerCallbackQuery answer = new AnswerCallbackQuery();
            answer.setCallbackQueryId(callbackQuery.getId());
            answer.setText(text);
            execute(answer);
        } catch (TelegramApiException e) {
            log.warn("answerCallbackQuery xatolik: {}", e.getMessage());
        }
    }

    private void editTaskViewMessage(CallbackQuery callbackQuery, Long chatId, Task task) {
        if (callbackQuery.getMessage() == null || callbackQuery.getMessage().getMessageId() == null) {
            return;
        }
        try {
            Optional<BoardColumn> colOpt = columnRepository.findById(task.getColumnId());
            String colName = colOpt.isPresent() ? colOpt.get().getTitle() : "Noma'lum";

            String workspaceName = "Mavjud emas";
            if (task.getWorkspaceId() != null) {
                Optional<Workspace> wsOpt = workspaceRepository.findById(task.getWorkspaceId());
                if (wsOpt.isPresent()) {
                    workspaceName = wsOpt.get().getTitle();
                }
            }

            String details = buildTaskViewText(task, colName, workspaceName);

            EditMessageText edit = new EditMessageText();
            edit.setChatId(String.valueOf(chatId));
            edit.setMessageId(callbackQuery.getMessage().getMessageId());
            edit.setText(details);
            edit.setParseMode("HTML");
            edit.setReplyMarkup(buildTaskViewKeyboard(task));
            execute(edit);
        } catch (TelegramApiException e) {
            String msg = e.getMessage() != null ? e.getMessage().toLowerCase() : "";
            if (msg.contains("message is not modified") || msg.contains("message to edit not found")) {
                log.debug("Xabarni yangilash imkonsiz: {}", e.getMessage());
            } else {
                log.warn("editMessageText xatolik: {}", e.getMessage());
            }
        }
    }

    private String buildTaskViewText(Task task, String colName, String workspaceName) {
        StringBuilder sb = new StringBuilder();
        sb.append("📋 Vazifa: <b>").append(TelegramUtil.escapeHtml(task.getTitle())).append("</b>\n");
        sb.append("🏢 G'alva: <b>").append(TelegramUtil.escapeHtml(workspaceName)).append("</b>\n");
        sb.append("📊 Holati (Ustun): <b>").append(TelegramUtil.escapeHtml(colName)).append("</b>\n");
        sb.append("⚠️ Muhimligi: <b>").append(TelegramUtil.escapeHtml(task.getPriority() != null ? task.getPriority().name() : "O'rtacha")).append("</b>\n");
        if (task.getDueDate() != null) {
            sb.append("📅 Muddat: <b>").append(task.getDueDate()).append("</b>\n");
        }
        if (task.getDescription() != null && !task.getDescription().isBlank()) {
            sb.append("📝 Tafsilot: ").append(TelegramUtil.escapeHtml(task.getDescription()));
        }
        return sb.toString();
    }

    private InlineKeyboardMarkup buildTaskViewKeyboard(Task task) {
        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();

        List<InlineKeyboardButton> row1 = new ArrayList<>();
        InlineKeyboardButton statusBtn = new InlineKeyboardButton();
        statusBtn.setText("🔄 Holatni o'zgartirish");
        statusBtn.setCallbackData("TASK_STATUS_SELECT_" + task.getId());
        row1.add(statusBtn);
        rows.add(row1);

        List<InlineKeyboardButton> row2 = new ArrayList<>();
        InlineKeyboardButton doneBtn = new InlineKeyboardButton();
        doneBtn.setText("✅ Bajarildi");
        doneBtn.setCallbackData("TASK_DONE_" + task.getId());
        row2.add(doneBtn);

        InlineKeyboardButton snoozeBtn = new InlineKeyboardButton();
        snoozeBtn.setText("📅 Ertaga suring");
        snoozeBtn.setCallbackData("TASK_SNOOZE_" + task.getId());
        row2.add(snoozeBtn);
        rows.add(row2);

        List<InlineKeyboardButton> row3 = new ArrayList<>();
        InlineKeyboardButton webAppBtn = new InlineKeyboardButton();
        webAppBtn.setText("📱 Ochish");
        webAppBtn.setWebApp(new org.telegram.telegrambots.meta.api.objects.webapp.WebAppInfo(miniappBaseUrl + "/app/?task=" + task.getId()));
        row3.add(webAppBtn);
        rows.add(row3);

        markup.setKeyboard(rows);
        return markup;
    }

    private ReplyKeyboardMarkup getMainKeyboard() {
        ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();
        keyboardMarkup.setResizeKeyboard(true);
        keyboardMarkup.setSelective(true);

        List<KeyboardRow> keyboard = new ArrayList<>();
        KeyboardRow row1 = new KeyboardRow();
        row1.add("📌 Bosh og'riqlarim");
        row1.add("➕ Yangi vazifa");

        KeyboardRow row2 = new KeyboardRow();
        row2.add("🏆 Mening maqomim");
        row2.add("ℹ️ Yordam");

        keyboard.add(row1);
        keyboard.add(row2);
        keyboardMarkup.setKeyboard(keyboard);
        return keyboardMarkup;
    }
}
