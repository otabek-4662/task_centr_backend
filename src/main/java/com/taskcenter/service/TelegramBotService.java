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
import com.taskcenter.model.WorkspaceInvitation;
import com.taskcenter.model.WorkspaceMember;
import com.taskcenter.model.InvitationStatus;
import com.taskcenter.model.InvitationType;
import com.taskcenter.repository.WorkspaceInvitationRepository;
import com.taskcenter.repository.WorkspaceMemberRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.scheduling.annotation.Async;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
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
@org.springframework.boot.autoconfigure.condition.ConditionalOnProperty(name = "telegram.bot.enabled", havingValue = "true")
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
    private final WorkspaceInvitationRepository invitationRepository;
    private final WorkspaceMemberRepository memberRepository;
    private final WorkspaceInvitationService workspaceInvitationService;
    private FocusTimerService focusTimerService;
    private TaskDependencyService taskDependencyService;
    private RecurringTaskService recurringTaskService;
    private CalendarAndReportService calendarAndReportService;

    @org.springframework.beans.factory.annotation.Autowired(required = false)
    public void setFocusTimerService(FocusTimerService focusTimerService) {
        this.focusTimerService = focusTimerService;
    }

    @org.springframework.beans.factory.annotation.Autowired(required = false)
    public void setTaskDependencyService(TaskDependencyService taskDependencyService) {
        this.taskDependencyService = taskDependencyService;
    }

    @org.springframework.beans.factory.annotation.Autowired(required = false)
    public void setRecurringTaskService(RecurringTaskService recurringTaskService) {
        this.recurringTaskService = recurringTaskService;
    }

    @org.springframework.beans.factory.annotation.Autowired(required = false)
    public void setCalendarAndReportService(CalendarAndReportService calendarAndReportService) {
        this.calendarAndReportService = calendarAndReportService;
    }

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
            return createdAt.isBefore(LocalDateTime.now().minusMinutes(30));
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

    private static org.telegram.telegrambots.bots.DefaultBotOptions createDefaultBotOptions() {
        org.telegram.telegrambots.bots.DefaultBotOptions options = new org.telegram.telegrambots.bots.DefaultBotOptions();
        options.setBackOff(new com.taskcenter.util.TelegramBotBackOff());
        options.setGetUpdatesTimeout(50);
        return options;
    }

    @org.springframework.beans.factory.annotation.Autowired
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
            @Value("${telegram.miniapp.base-url:https://task-centr-backend.onrender.com}") String miniappBaseUrl,
            WorkspaceInvitationRepository invitationRepository,
            WorkspaceMemberRepository memberRepository,
            @org.springframework.context.annotation.Lazy WorkspaceInvitationService workspaceInvitationService) {
        super(createDefaultBotOptions(), botToken);
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
        this.invitationRepository = invitationRepository;
        this.memberRepository = memberRepository;
        this.workspaceInvitationService = workspaceInvitationService;
    }

    public TelegramBotService(
            String botToken,
            String botUsername,
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
            String miniappBaseUrl,
            WorkspaceInvitationRepository invitationRepository,
            WorkspaceMemberRepository memberRepository) {
        this(botToken, botUsername, userRepository, taskRepository, columnRepository,
             workspaceRepository, authorizationService, taskExecutor, clock, activityService,
             telegramNotificationService, telegramReminderLogRepository, webSocketNotifier,
             miniappBaseUrl, invitationRepository, memberRepository, null);
    }

    public TelegramBotService(
            String botToken,
            String botUsername,
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
            String miniappBaseUrl) {
        this(botToken, botUsername, userRepository, taskRepository, columnRepository,
             workspaceRepository, authorizationService, taskExecutor, clock, activityService,
             telegramNotificationService, telegramReminderLogRepository, webSocketNotifier,
             miniappBaseUrl, null, null, null);
    }

    @Override
    public String getBotUsername() {
        return botUsername;
    }

    public void cleanupTaskCreationStates() {
        taskCreationStates.entrySet().removeIf(entry -> entry.getValue().isExpired());
    }

    @Async
    @EventListener(ApplicationReadyEvent.class)
    public void initCommands() {
        try {
            List<BotCommand> commands = List.of(
                new BotCommand("start", "Botni ishga tushirish"),
                new BotCommand("create_task", "Yangi vazifa yaratish"),
                new BotCommand("my_tasks", "Bosh og'riqlarim"),
                new BotCommand("tasks_today", "Bugungi vazifalar"),
                new BotCommand("overdue", "Muddati o'tgan vazifalar"),
                new BotCommand("search", "Vazifalarni qidirish (/search so'z)"),
                new BotCommand("timer", "Jonli Pomodoro / Fokus taymeri"),
                new BotCommand("calendar", "Google / Apple Kalendar sinxronizatsiyasi"),
                new BotCommand("report", "Kunlik PDF hisobot"),
                new BotCommand("link_group", "Telegram guruhni loyihaga ulash"),
                new BotCommand("leaderboard", "Guruh peshqadamlari (Choyxona Leaderboard)"),
                new BotCommand("stats", "Shaxsiy statistika va maqom"),
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
    public void onUpdateReceived(Update update) {
        if (update.hasInlineQuery()) {
            handleInlineQuery(update.getInlineQuery());
            return;
        }
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
                String grpText = update.getMessage().getText();
                if (grpText != null && isGroupCommandOrHashtag(grpText.trim())) {
                    handleGroupMessage(update.getMessage());
                    return;
                }
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
            } else if (text.equals("/overdue") || text.equals("⏰ Muddati o'tganlar")) {
                taskCreationStates.remove(chatId);
                handleOverdue(chatId);
            } else if (text.equals("/tasks_today") || text.equals("/today") || text.equals("📅 Bugungi vazifalar")) {
                taskCreationStates.remove(chatId);
                handleTasksToday(chatId);
            } else if (text.equals("/search") || text.equals("🔍 Qidiruv")) {
                taskCreationStates.remove(chatId);
                sendMessage(chatId, "🔍 <b>Vazifa qidirish:</b>\n<code>/search kalit_so'z</code> ko'rinishida yozing (masalan: <code>/search LOY-12</code> yoki <code>/search dizayn</code>) yoki istalgan chatda <code>@" + (botUsername != null ? botUsername : "bot") + " so'z</code> orqali inline qidiring.");
            } else if (text.startsWith("/search ")) {
                taskCreationStates.remove(chatId);
                handleSearchTasks(chatId, text.substring(8).trim());
            } else if (text.equals("/timer") || text.equals("/pomodoro") || text.equals("⏱ Fokus taymer")) {
                taskCreationStates.remove(chatId);
                handleTimerCommand(chatId);
            } else if (text.equals("/calendar") || text.equals("📆 Kalendar")) {
                taskCreationStates.remove(chatId);
                handleCalendarCommand(chatId);
            } else if (text.equals("/report") || text.equals("📄 PDF Hisobot")) {
                taskCreationStates.remove(chatId);
                handleReportCommand(chatId);
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
                if (state != null) {
                    if (state.isExpired()) {
                        taskCreationStates.remove(chatId);
                        sendMessage(chatId, "⚠️ Jarayon muddati o'tgan. Iltimos, yangi vazifa yaratish uchun /create_task buyrug'ini bering.");
                    } else if (state.getStep() == TaskCreationStep.AWAITING_TITLE) {
                        handleTaskCreationTitleInput(chatId, text);
                    } else {
                        taskCreationStates.remove(chatId);
                        sendMessage(chatId, "Kechirasiz, bu buyruqni tushunmadim. Quyidagi menyu tugmalaridan foydalaning yoki /help buyrug'ini bering.");
                    }
                } else if (update.getMessage().getForwardDate() != null
                        || update.getMessage().getForwardFrom() != null
                        || update.getMessage().getForwardSenderName() != null) {
                    handleCreateTaskCommand(chatId, text);
                } else {
                    sendMessage(chatId, "Kechirasiz, bu buyruqni tushunmadim. Quyidagi menyu tugmalaridan foydalaning yoki /help buyrug'ini bering.");
                }
            }
        } else if (update.hasMessage()
                && update.getMessage().getChat() != null
                && "private".equals(update.getMessage().getChat().getType())) {
            Long chatId = update.getMessage().getChatId();
            String caption = update.getMessage().getCaption();
            if (caption != null && !caption.isBlank()) {
                handleCreateTaskCommand(chatId, caption.trim());
            } else if (update.getMessage().hasVoice() || update.getMessage().hasAudio()) {
                String voiceTitle = "🎙 Ovozli vazifa (" + LocalDate.now(clock) + ")";
                handleCreateTaskCommand(chatId, voiceTitle);
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
            if (token.startsWith("inv_") || token.startsWith("invite_")) {
                String inviteId = token.startsWith("inv_") ? token.substring(4) : token.substring(7);
                handleInviteLink(chatId, inviteId);
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

                if (user.getTelegramChatId() != null && !user.getTelegramChatId().equals(chatId)) {
                    sendMessage(user.getTelegramChatId(), "⚠️ Akkauntingiz boshqa Telegram'ga ulandi.");
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

                if (workspaceInvitationService != null) {
                    String pendingToken = workspaceInvitationService.getPendingChatInvite(chatId);
                    if (pendingToken != null) {
                        try {
                            workspaceInvitationService.acceptInvitation(pendingToken, user);
                            workspaceInvitationService.removePendingChatInvite(chatId);
                            sendMessage(chatId, "🎉 <b>Akkauntingiz ulandi!</b> Siz avvalgi taklifnoma bo'yicha loyiha jamoasiga avtomatik qo'shildingiz.");
                        } catch (Exception e) {
                            log.warn("Telegram auto-join pending invite error: {}", e.getMessage());
                            workspaceInvitationService.removePendingChatInvite(chatId);
                        }
                    }
                }
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

    private void handleInviteLink(Long chatId, String tokenOrId) {
        Optional<WorkspaceInvitation> invOpt;
        if (workspaceInvitationService != null) {
            try {
                invOpt = Optional.of(workspaceInvitationService.findInvitationByTokenOrId(tokenOrId));
            } catch (Exception e) {
                invOpt = Optional.empty();
            }
        } else if (invitationRepository != null) {
            invOpt = invitationRepository.findById(tokenOrId);
        } else {
            sendMessage(chatId, "⚠️ Taklifnomalar xizmati vaqtincha mavjud emas.");
            return;
        }

        if (invOpt.isEmpty()) {
            sendMessage(chatId, "❌ Taklifnoma topilmadi yoki bekor qilingan.");
            return;
        }

        WorkspaceInvitation inv = invOpt.get();
        if (inv.getStatus() == InvitationStatus.CANCELLED) {
            sendMessage(chatId, "⚠️ Ushbu taklifnoma administrator tomonidan bekor qilingan.");
            return;
        }
        if (inv.getStatus() == InvitationStatus.REJECTED) {
            sendMessage(chatId, "ℹ️ Ushbu taklifnoma rad etilgan.");
            return;
        }
        if (inv.getStatus() == InvitationStatus.ACCEPTED && inv.getType() == InvitationType.EMAIL) {
            sendMessage(chatId, "ℹ️ Ushbu taklifnoma allaqachon qabul qilingan.");
            return;
        }
        if (inv.getExpiresAt() != null && inv.getExpiresAt().isBefore(LocalDateTime.now(java.time.ZoneOffset.UTC))) {
            sendMessage(chatId, "⚠️ Ushbu taklifnomaning amal qilish muddati tugagan.");
            return;
        }
        if (inv.getType() == InvitationType.LINK && inv.getMaxUses() != null && inv.getUseCount() >= inv.getMaxUses()) {
            sendMessage(chatId, "⚠️ Ushbu taklif havolasining foydalanish limiti tugagan.");
            return;
        }

        Workspace workspace = workspaceRepository.findById(inv.getWorkspaceId()).orElse(null);
        if (workspace == null) {
            sendMessage(chatId, "❌ Taklif qilingan workspace topilmadi.");
            return;
        }

        String inviterName = userRepository.findById(inv.getSenderId())
                .map(u -> u.getFullName() != null ? u.getFullName() : u.getName())
                .orElse("Jamoa administratori");

        Optional<User> userOpt = userRepository.findByTelegramChatId(chatId);
        String wsTitle = TelegramUtil.escapeHtml(workspace.getTitle());
        String roleName = inv.getRole() != null ? inv.getRole().getDisplayName() : "A'zo";

        if (userOpt.isPresent()) {
            User user = userOpt.get();
            if (workspace.getOwnerId().equals(user.getId()) || (memberRepository != null && memberRepository.existsByWorkspaceIdAndUserId(workspace.getId(), user.getId()))) {
                sendMessage(chatId, "ℹ️ Siz allaqachon <b>" + wsTitle + "</b> loyihasining a'zosisiz.");
                return;
            }

            if (inv.getType() == InvitationType.EMAIL && user.getEmail() != null && user.getEmail().endsWith("@taskcenter.local")) {
                sendMessage(chatId, "⚠️ Ushbu taklifnoma aniq email egasiga yuborilgan. Telegram profilingizdagi dummy email bilan uni qabul qilib bo'lmaydi. Iltimos, veb ilovada emailingizni tasdiqlang yoki havola-taklifdan (invite link) foydalaning.");
                return;
            }

            String msg = "📨 <b>Yangi jamoaga taklifnoma!</b>\n\n"
                    + "🏢 Ishchi maydon: <b>" + wsTitle + "</b>\n"
                    + "👤 Taklif etuvchi: <b>" + TelegramUtil.escapeHtml(inviterName) + "</b>\n"
                    + "🎭 Berilayotgan rol: <b>" + roleName + "</b>\n\n"
                    + "Taklifni qabul qilasizmi?";

            InlineKeyboardMarkup keyboard = new InlineKeyboardMarkup();
            List<List<InlineKeyboardButton>> rows = new ArrayList<>();
            List<InlineKeyboardButton> row = new ArrayList<>();

            InlineKeyboardButton acceptBtn = new InlineKeyboardButton();
            acceptBtn.setText("✅ Qabul qilish");
            acceptBtn.setCallbackData("ACCEPT_INV:" + tokenOrId);
            row.add(acceptBtn);

            InlineKeyboardButton rejectBtn = new InlineKeyboardButton();
            rejectBtn.setText("❌ Rad etish");
            rejectBtn.setCallbackData("REJECT_INV:" + tokenOrId);
            row.add(rejectBtn);

            rows.add(row);
            keyboard.setKeyboard(rows);

            sendMessageWithInlineKeyboard(chatId, msg, keyboard);
        } else {
            if (workspaceInvitationService != null) {
                workspaceInvitationService.storePendingChatInvite(chatId, tokenOrId);
            }

            String webLink = (miniappBaseUrl != null ? miniappBaseUrl : "https://task-center-frontend.onrender.com")
                    + "/invite/" + tokenOrId;

            String msg = "📨 <b>Yangi jamoaga taklifnoma!</b>\n\n"
                    + "🏢 Ishchi maydon: <b>" + wsTitle + "</b>\n"
                    + "👤 Taklif etuvchi: <b>" + TelegramUtil.escapeHtml(inviterName) + "</b>\n"
                    + "🎭 Berilayotgan rol: <b>" + roleName + "</b>\n\n"
                    + "Ushbu jamoaga qo'shilish uchun avval veb ilovada ro'yxatdan o'ting yoki profilingizni Telegram'ga ulang.\n"
                    + "Hisobingiz ulangandan so'ng siz avtomatik tarzda jamoaga qo'shilasiz!";

            InlineKeyboardMarkup keyboard = new InlineKeyboardMarkup();
            List<List<InlineKeyboardButton>> rows = new ArrayList<>();
            List<InlineKeyboardButton> row = new ArrayList<>();

            InlineKeyboardButton openBtn = new InlineKeyboardButton();
            openBtn.setText("🌐 Taklifni ilovada ochish");
            openBtn.setUrl(webLink);
            row.add(openBtn);

            rows.add(row);
            keyboard.setKeyboard(rows);

            sendMessageWithInlineKeyboard(chatId, msg, keyboard);
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
            nextRank = "O'zimizning bola";
        } else if (totalAssigned <= 25) {
            currentRankThreshold = 11;
            nextRankThreshold = 26;
            nextRank = "Ishongan tog'imiz";
        } else if (totalAssigned <= 50) {
            currentRankThreshold = 26;
            nextRankThreshold = 51;
            nextRank = "Ko'cha ko'rgan";
        } else if (totalAssigned <= 100) {
            currentRankThreshold = 51;
            nextRankThreshold = 101;
            nextRank = "Okaxon";
        } else if (totalAssigned <= 200) {
            currentRankThreshold = 101;
            nextRankThreshold = 201;
            nextRank = "Choyxona oqsoqoli";
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
            sb.append("👑 Siz allaqachon eng oliy darajadasiz ('Choyxona oqsoqoli')!\n\n");
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

    private void handleSearchTasks(Long chatId, String query) {
        Optional<User> userOpt = userRepository.findByTelegramChatId(chatId);
        if (userOpt.isEmpty()) {
            sendMessage(chatId, "Sizning akkauntingiz hali ulanmagan. Iltimos, web ilovadan ulaning.");
            return;
        }
        if (query == null || query.isBlank()) {
            sendMessage(chatId, "🔍 Qidirish uchun so'z kiriting: <code>/search so'z</code>");
            return;
        }
        String q = query.trim().toLowerCase();
        List<Task> activeTasks = taskRepository.findActiveTasksByUserId(userOpt.get().getId());
        List<Task> matched = activeTasks.stream()
                .filter(t -> t != null && (
                        (t.getTitle() != null && t.getTitle().toLowerCase().contains(q)) ||
                        (t.getPublicId() != null && t.getPublicId().toLowerCase().contains(q)) ||
                        (t.getDescription() != null && t.getDescription().toLowerCase().contains(q))
                ))
                .toList();
        if (matched.isEmpty()) {
            sendMessage(chatId, "🔍 <b>\"" + TelegramUtil.escapeHtml(query) + "\"</b> bo'yicha faol vazifalar topilmadi.");
        } else {
            sendTasksList(chatId, matched, "🔍 <b>Qidiruv natijalari (" + matched.size() + " ta):</b>\n(Batafsil ko'rish uchun tanlang)");
        }
    }

    private String generateSequentialPublicId(String workspaceId, Workspace workspace) {
        String cleanPrefix = "WFM";
        if (workspace != null && workspace.getKeyPrefix() != null && !workspace.getKeyPrefix().isBlank()) {
            String head = workspace.getKeyPrefix().split("-")[0].replaceAll("[^a-zA-Z0-9]", "").toUpperCase();
            if (!head.isEmpty()) {
                cleanPrefix = head;
            }
        }
        long baseCount = taskRepository.countAllByWorkspaceId(workspaceId);
        long currentSeq = (workspace != null && workspace.getTaskSeq() != null) ? workspace.getTaskSeq() : 0L;
        long nextSeq = Math.max(baseCount, currentSeq) + 1L;
        String candidate = cleanPrefix + "-" + nextSeq;
        while (taskRepository.existsByPublicId(candidate)) {
            nextSeq++;
            candidate = cleanPrefix + "-" + nextSeq;
        }
        if (workspace != null) {
            workspace.setTaskSeq(nextSeq);
        }
        return candidate;
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
        String lexoRank = LexoRankUtil.getMiddleSafe(maxRank, null);

        Optional<Workspace> wsForSeqOpt = workspaceRepository.findById(workspaceId);
        String sequentialPublicId = generateSequentialPublicId(workspaceId, wsForSeqOpt.orElse(null));

        Task task = Task.builder()
                .workspaceId(workspaceId)
                .columnId(targetColumn.getId())
                .publicId(sequentialPublicId)
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
                + "🔹 📅 <b>Bugungi vazifalar</b> (/tasks_today, /today) — Bugun bajarilishi kerak bo'lgan vazifalar\n"
                + "🔹 🔍 <b>Qidiruv</b> (/search so'z) — Vazifalarni kod yoki nom bo'yicha tezkor qidirish\n"
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

        int maxTasks = 80;
        int displayCount = Math.min(tasks.size(), maxTasks);

        StringBuilder textBuilder = new StringBuilder(titleHeader);
        if (tasks.size() > maxTasks) {
            int remaining = tasks.size() - maxTasks;
            textBuilder.append("\n\n... va yana ").append(remaining).append(" ta vazifa. Hammasini Mini App'da ko'ring.");
        }
        message.setText(textBuilder.toString());

        InlineKeyboardMarkup markupInline = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rowsInline = new ArrayList<>();

        for (int i = 0; i < displayCount; i++) {
            Task t = tasks.get(i);
            List<InlineKeyboardButton> rowInline = new ArrayList<>();
            InlineKeyboardButton button = new InlineKeyboardButton();
            String title = t.getTitle() != null ? t.getTitle() : "Nomsiz";
            if (title.length() > 36) {
                title = title.substring(0, 33) + "...";
            }
            button.setText("📌 " + title);
            button.setCallbackData("TASK_VIEW_" + t.getId());
            rowInline.add(button);
            rowsInline.add(rowInline);
        }

        List<InlineKeyboardButton> webAppRow = new ArrayList<>();
        InlineKeyboardButton webAppBtn = new InlineKeyboardButton();
        webAppBtn.setText("📱 Ochish");
        webAppBtn.setWebApp(new org.telegram.telegrambots.meta.api.objects.webapp.WebAppInfo(miniappBaseUrl + "/app/"));
        webAppRow.add(webAppBtn);
        rowsInline.add(webAppRow);

        markupInline.setKeyboard(rowsInline);
        message.setReplyMarkup(markupInline);
        message.setParseMode("HTML");

        executeWithRetry(message, chatId, 0);
    }

    private void handleCallbackQuery(CallbackQuery callbackQuery) {
        String callData = callbackQuery.getData();
        Long chatId = callbackQuery.getMessage().getChatId();

        if (!callData.startsWith("TASK_DONE_") && !callData.startsWith("TASK_SNOOZE_")
                && !callData.startsWith("TASK_LOG30_") && !callData.startsWith("TIMER_")
                && !callData.startsWith("TASK_RECUR_")
                && !callData.startsWith("STATS_") && !callData.startsWith("CT_")) {
            answerCallback(callbackQuery, null);
        }

        Optional<User> userOpt = userRepository.findByTelegramChatId(chatId);
        if (userOpt.isEmpty()) {
            if (callData.startsWith("TASK_DONE_") || callData.startsWith("TASK_SNOOZE_")
                    || callData.startsWith("TASK_LOG30_") || callData.startsWith("TIMER_")
                    || callData.startsWith("TASK_RECUR_")
                    || callData.startsWith("STATS_") || callData.startsWith("CT_")) {
                answerCallback(callbackQuery, null);
            }
            sendMessage(chatId, "Sizning akkauntingiz hali ulanmagan. Iltimos, ulanish uchun web ilovadan token oling.");
            return;
        }
        User user = userOpt.get();

        if (callData.startsWith("ACCEPT_INV:")) {
            String inviteId = callData.substring(11);
            handleAcceptInvitationCallback(callbackQuery, user, inviteId);
            return;
        } else if (callData.startsWith("REJECT_INV:")) {
            String inviteId = callData.substring(11);
            handleRejectInvitationCallback(callbackQuery, user, inviteId);
            return;
        }

        if (callData.startsWith("TASK_VIEW_")) {
            String taskId = callData.substring(10);
            Optional<Task> taskOpt = taskRepository.findById(taskId);

            if (taskOpt.isPresent()) {
                Task task = taskOpt.get();
                
                if (Boolean.TRUE.equals(task.getIsArchived())) {
                    sendMessage(chatId, "⚠️ Ushbu vazifa arxivlangan.");
                    return;
                }

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
                
                if (Boolean.TRUE.equals(task.getIsArchived())) {
                    sendMessage(chatId, "⚠️ Vazifa arxivlangan. Holatini o'zgartirib bo'lmaydi.");
                    return;
                }

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

            if (Boolean.TRUE.equals(task.getIsArchived())) {
                sendMessage(chatId, "⚠️ Vazifa arxivlangan. Holatini o'zgartirib bo'lmaydi.");
                return;
            }

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
                sendMessage(chatId, "⚠️ Jarayon muddati o'tgan. Iltimos, yangi vazifa yaratish uchun /create_task buyrug'ini bering.");
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
                sendMessage(chatId, "⚠️ Jarayon muddati o'tgan. Iltimos, yangi vazifa yaratish uchun /create_task buyrug'ini bering.");
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
        } else if (callData.startsWith("TASK_LOG30_")) {
            handleTaskQuickWorklog(callbackQuery, chatId, user, callData.substring(11));
        } else if (callData.startsWith("TIMER_START_")) {
            handleTimerStartCallback(callbackQuery, chatId, user, callData.substring(12));
        } else if (callData.equals("TIMER_STOP")) {
            handleTimerStopCallback(callbackQuery, chatId, user);
        } else if (callData.startsWith("TASK_RECUR_")) {
            handleTaskRecurrenceCycleCallback(callbackQuery, chatId, user, callData.substring(11));
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
            answerCallback(callbackQuery, null);
            sendMessage(chatId, "Bu tugma eskirgan, xabarni qayta oching");
        }
    }

    private void handleAcceptInvitationCallback(CallbackQuery callbackQuery, User user, String inviteId) {
        Long chatId = callbackQuery.getMessage().getChatId();
        Integer messageId = callbackQuery.getMessage().getMessageId();

        if (workspaceInvitationService != null) {
            try {
                workspaceInvitationService.acceptInvitation(inviteId, user);
                String successMsg = "🎉 <b>Tabriklaymiz!</b> Siz jamoaga muvaffaqiyatli qo'shildingiz!";
                EditMessageText edit = new EditMessageText();
                edit.setChatId(String.valueOf(chatId));
                edit.setMessageId(messageId);
                edit.setText(successMsg);
                edit.setParseMode("HTML");
                execute(edit);
                return;
            } catch (Exception e) {
                String errMsg = "⚠️ " + e.getMessage();
                try {
                    EditMessageText edit = new EditMessageText();
                    edit.setChatId(String.valueOf(chatId));
                    edit.setMessageId(messageId);
                    edit.setText(errMsg);
                    execute(edit);
                } catch (Exception ignored) {
                    sendMessage(chatId, errMsg);
                }
                return;
            }
        }

        if (invitationRepository == null || memberRepository == null) {
            sendMessage(chatId, "⚠️ Taklifnomalar xizmati vaqtincha mavjud emas.");
            return;
        }

        Optional<WorkspaceInvitation> invOpt = invitationRepository.findById(inviteId);
        if (invOpt.isEmpty()) {
            sendMessage(chatId, "❌ Taklifnoma topilmadi.");
            return;
        }

        WorkspaceInvitation inv = invOpt.get();
        if (inv.getStatus() != InvitationStatus.PENDING || (inv.getExpiresAt() != null && inv.getExpiresAt().isBefore(LocalDateTime.now(java.time.ZoneOffset.UTC)))) {
            sendMessage(chatId, "⚠️ Ushbu taklifnomaning amal qilish muddati o'tgan.");
            return;
        }

        Workspace ws = workspaceRepository.findById(inv.getWorkspaceId()).orElse(null);
        String wsTitle = ws != null ? TelegramUtil.escapeHtml(ws.getTitle()) : "loyihaga";

        if (!memberRepository.existsByWorkspaceIdAndUserId(inv.getWorkspaceId(), user.getId())) {
            WorkspaceMember member = WorkspaceMember.builder()
                    .workspaceId(inv.getWorkspaceId())
                    .userId(user.getId())
                    .role(inv.getRole())
                    .build();
            memberRepository.save(member);
            authorizationService.evictRole(inv.getWorkspaceId(), user.getId());
        }

        if (user.getEmail() != null && user.getEmail().endsWith("@taskcenter.local")
                && inv.getReceiverEmail() != null && inv.getReceiverEmail().contains("@")) {
            user.setEmail(inv.getReceiverEmail());
            userRepository.save(user);
        }

        inv.setReceiverId(user.getId());
        inv.setStatus(InvitationStatus.ACCEPTED);
        invitationRepository.save(inv);

        String successMsg = "🎉 <b>Tabriklaymiz!</b> Siz <b>" + wsTitle + "</b> jamoasiga muvaffaqiyatli qo'shildingiz!";
        try {
            EditMessageText edit = new EditMessageText();
            edit.setChatId(String.valueOf(chatId));
            edit.setMessageId(messageId);
            edit.setText(successMsg);
            edit.setParseMode("HTML");
            execute(edit);
        } catch (Exception e) {
            sendMessage(chatId, successMsg);
        }
    }

    private void handleRejectInvitationCallback(CallbackQuery callbackQuery, User user, String inviteId) {
        Long chatId = callbackQuery.getMessage().getChatId();
        Integer messageId = callbackQuery.getMessage().getMessageId();

        if (workspaceInvitationService != null) {
            try {
                workspaceInvitationService.rejectInvitation(inviteId, user);
                String rejectMsg = "❌ Taklifnoma rad etildi.";
                EditMessageText edit = new EditMessageText();
                edit.setChatId(String.valueOf(chatId));
                edit.setMessageId(messageId);
                edit.setText(rejectMsg);
                edit.setParseMode("HTML");
                execute(edit);
                return;
            } catch (Exception e) {
                String errMsg = "⚠️ " + e.getMessage();
                try {
                    EditMessageText edit = new EditMessageText();
                    edit.setChatId(String.valueOf(chatId));
                    edit.setMessageId(messageId);
                    edit.setText(errMsg);
                    execute(edit);
                } catch (Exception ignored) {
                    sendMessage(chatId, errMsg);
                }
                return;
            }
        }

        if (invitationRepository == null) {
            sendMessage(chatId, "⚠️ Taklifnomalar xizmati vaqtincha mavjud emas.");
            return;
        }

        Optional<WorkspaceInvitation> invOpt = invitationRepository.findById(inviteId);
        if (invOpt.isPresent()) {
            WorkspaceInvitation inv = invOpt.get();
            if (inv.getStatus() == InvitationStatus.PENDING) {
                inv.setStatus(InvitationStatus.REJECTED);
                invitationRepository.save(inv);
            }
        }

        String rejectMsg = "❌ Taklifnoma rad etildi.";
        try {
            EditMessageText edit = new EditMessageText();
            edit.setChatId(String.valueOf(chatId));
            edit.setMessageId(messageId);
            edit.setText(rejectMsg);
            edit.setParseMode("HTML");
            execute(edit);
        } catch (Exception e) {
            sendMessage(chatId, rejectMsg);
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

    private final java.util.concurrent.BlockingQueue<QueuedTelegramMessage> outboundMessageQueue = new java.util.concurrent.LinkedBlockingQueue<>();

    private static class QueuedTelegramMessage {
        final SendMessage message;
        final Long chatId;
        final int attempts;

        QueuedTelegramMessage(SendMessage message, Long chatId, int attempts) {
            this.message = message;
            this.chatId = chatId;
            this.attempts = attempts;
        }
    }

    public void enqueueMessage(Long chatId, String text) {
        enqueueMessage(chatId, text, null);
    }

    public void enqueueMessage(Long chatId, String text, InlineKeyboardMarkup keyboard) {
        if (chatId == null || text == null) return;
        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setText(text);
        message.setParseMode("HTML");
        if (keyboard != null) {
            message.setReplyMarkup(keyboard);
        }
        outboundMessageQueue.offer(new QueuedTelegramMessage(message, chatId, 0));
    }

    @jakarta.annotation.PostConstruct
    public void startQueueWorker() {
        Thread worker = new Thread(() -> {
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    QueuedTelegramMessage item = outboundMessageQueue.take();
                    executeWithRetry(item.message, item.chatId, item.attempts);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                } catch (Exception e) {
                    log.error("Telegram queue worker xatoligi: {}", e.getMessage());
                }
            }
        }, "telegram-queue-worker");
        worker.setDaemon(true);
        worker.start();
    }

    private static final Object rateLimitLock = new Object();
    private static long lastMessageTime = 0;

    private void enforceRateLimit() {
        synchronized (rateLimitLock) {
            long now = System.currentTimeMillis();
            long elapsed = now - lastMessageTime;
            if (elapsed < 40) { // ~25 msgs/sec (safe under Telegram 30/s limit)
                try {
                    Thread.sleep(40 - elapsed);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
            lastMessageTime = System.currentTimeMillis();
        }
    }

    private void executeWithRetry(SendMessage message, Long chatId, int attempt) {
        try {
            enforceRateLimit();
            execute(message);
        } catch (org.telegram.telegrambots.meta.exceptions.TelegramApiRequestException e) {
            if (e.getErrorCode() != null && e.getErrorCode() == 429 && attempt < 3) {
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
            sendErrorMessage(chatId);
        }
    }

    private void sendErrorMessage(Long chatId) {
        if (chatId == null) return;
        try {
            SendMessage m = new SendMessage();
            m.setChatId(String.valueOf(chatId));
            m.setText("Xatolik yuz berdi, keyinroq urinib ko'ring.");
            execute(m);
        } catch (Exception e) {
            log.error("Xatolik xabarini yuborishda ham xatolik: {}", e.getMessage());
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
            
            boolean isAssignee = task.getAssignees().stream().anyMatch(u -> u.getId().equals(user.getId()));
            boolean isCreator = user.getId().equals(task.getCreatedBy());
            boolean isAdminOrOwner = authorizationService.isOwnerOrAdmin(task.getWorkspaceId(), user.getId());
            
            if (!isAssignee && !isCreator && !isAdminOrOwner) {
                answerCallback(callbackQuery, "Bu vazifa sizga biriktirilmagan.");
                return;
            }
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

        if (taskDependencyService != null) {
            List<Task> blockers = taskDependencyService.getUnfinishedBlockers(task);
            if (!blockers.isEmpty()) {
                String bCode = blockers.get(0).getPublicId() != null ? blockers.get(0).getPublicId() : blockers.get(0).getTitle();
                answerCallback(callbackQuery, "⛔ Avval [" + bCode + "] vazifasini yakunlang!");
                return;
            }
        }

        task.setColumnId(doneColumn.getId());
        String maxRank = taskRepository.findMaxLexoRankByColumnId(doneColumn.getId());
        task.setLexoRank(LexoRankUtil.getMiddleSafe(maxRank, null));
        taskRepository.save(task);

        if (taskDependencyService != null) {
            taskDependencyService.notifyUnblockedTasksOnDone(task, user);
        }
        if (recurringTaskService != null) {
            recurringTaskService.spawnNextTaskWhenCompleted(task);
        }

        activityService.logActivity(task.getId(), user, TaskActivityType.STATUS_UPDATED,
                "columnId", oldColId, doneColumn.getId());

        telegramNotificationService.sendTaskMovedNotification(task, user, oldColumnTitle, doneColumn.getTitle());
        if (webSocketNotifier != null) {
            webSocketNotifier.notifyWorkspace(task.getWorkspaceId(), WebSocketEvent.builder()
                    .type("TASK_MOVED")
                    .workspaceId(task.getWorkspaceId())
                    .data(TaskDto.fromEntity(task))
                    .build());
        }

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
            
            boolean isAssignee = task.getAssignees().stream().anyMatch(u -> u.getId().equals(user.getId()));
            boolean isCreator = user.getId().equals(task.getCreatedBy());
            boolean isAdminOrOwner = authorizationService.isOwnerOrAdmin(task.getWorkspaceId(), user.getId());
            
            if (!isAssignee && !isCreator && !isAdminOrOwner) {
                answerCallback(callbackQuery, "Bu vazifa sizga biriktirilmagan.");
                return;
            }
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
        if (webSocketNotifier != null) {
            webSocketNotifier.notifyWorkspace(task.getWorkspaceId(), WebSocketEvent.builder()
                    .type("TASK_UPDATED")
                    .workspaceId(task.getWorkspaceId())
                    .data(TaskDto.fromEntity(task))
                    .build());
        }

        answerCallback(callbackQuery, "📅 Muddat ertaga surildi!");
        editTaskViewMessage(callbackQuery, chatId, task);
    }

    private void handleTaskQuickWorklog(CallbackQuery callbackQuery, Long chatId, User user, String taskId) {
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
        double prevHours = task.getLoggedHours() != null ? task.getLoggedHours() : 0.0;
        task.logTime(0.5);
        taskRepository.save(task);
        activityService.logActivity(task.getId(), user, TaskActivityType.TIME_LOGGED,
                "loggedHours", String.valueOf(prevHours), String.valueOf(task.getLoggedHours()));
        if (webSocketNotifier != null) {
            webSocketNotifier.notifyWorkspace(task.getWorkspaceId(), WebSocketEvent.builder()
                    .type("TASK_UPDATED")
                    .workspaceId(task.getWorkspaceId())
                    .data(TaskDto.fromEntity(task))
                    .build());
        }
        answerCallback(callbackQuery, "⏱ +30 daqiqa yozildi! (Jami: " + task.getLoggedHours() + " soat)");
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
        String title = task.getTitle();
        if (title != null && title.length() > 200) {
            title = title.substring(0, 197) + "...";
        }
        sb.append("📋 Vazifa: <b>").append(TelegramUtil.escapeHtml(title)).append("</b>\n");
        sb.append("🏢 G'alva: <b>").append(TelegramUtil.escapeHtml(workspaceName)).append("</b>\n");
        sb.append("📊 Holati (Ustun): <b>").append(TelegramUtil.escapeHtml(colName)).append("</b>\n");
        sb.append("⚠️ Muhimligi: <b>").append(TelegramUtil.escapeHtml(task.getPriority() != null ? task.getPriority().name() : "O'rtacha")).append("</b>\n");
        if (task.getPublicId() != null && !task.getPublicId().isBlank()) {
            sb.append("🔑 Kod: <b>").append(TelegramUtil.escapeHtml(task.getPublicId())).append("</b>\n");
        }
        if (task.getDueDate() != null) {
            sb.append("📅 Muddat: <b>").append(task.getDueDate()).append("</b>\n");
        }
        if (task.getLoggedHours() != null && task.getLoggedHours() > 0) {
            sb.append("⏱ Sarflangan vaqt: <b>").append(task.getLoggedHours()).append(" soat</b>\n");
        }
        if (task.getRecurrenceRule() != null && task.getRecurrenceRule() != com.taskcenter.model.RecurrenceRule.NONE) {
            sb.append("🔁 Takrorlanish: <b>").append(task.getRecurrenceRule().name()).append("</b>\n");
        }
        
        String prefix = sb.toString();
        
        if (task.getDescription() != null && !task.getDescription().isBlank()) {
            String desc = task.getDescription();
            if (desc.length() > 1500) {
                desc = desc.substring(0, 1497) + "...";
            }
            String escapedDesc = TelegramUtil.escapeHtml(desc);
            int availableLength = 4000 - prefix.length() - 15;
            if (escapedDesc.length() > availableLength) {
                if (availableLength > 3) {
                    while (TelegramUtil.escapeHtml(desc).length() > availableLength && desc.length() > 10) {
                        desc = desc.substring(0, desc.length() - 100);
                    }
                    if (!desc.endsWith("...")) {
                        desc = desc.substring(0, Math.max(0, desc.length() - 3)) + "...";
                    }
                    escapedDesc = TelegramUtil.escapeHtml(desc);
                } else {
                    escapedDesc = "...";
                }
            }
            sb.append("📝 Tafsilot: ").append(escapedDesc);
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

        InlineKeyboardButton log30Btn = new InlineKeyboardButton();
        log30Btn.setText("⏱ +30 daq");
        log30Btn.setCallbackData("TASK_LOG30_" + task.getId());
        row1.add(log30Btn);
        rows.add(row1);

        List<InlineKeyboardButton> rowTimer = new ArrayList<>();
        InlineKeyboardButton focusBtn = new InlineKeyboardButton();
        focusBtn.setText("🍅 Fokus (25m)");
        focusBtn.setCallbackData("TIMER_START_" + task.getId());
        rowTimer.add(focusBtn);

        InlineKeyboardButton recurBtn = new InlineKeyboardButton();
        String rLabel = (task.getRecurrenceRule() != null && task.getRecurrenceRule() != com.taskcenter.model.RecurrenceRule.NONE)
                ? "🔁 " + task.getRecurrenceRule().name()
                : "🔁 Takrorlash";
        recurBtn.setText(rLabel);
        recurBtn.setCallbackData("TASK_RECUR_" + task.getId());
        rowTimer.add(recurBtn);
        rows.add(rowTimer);

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
        row2.add("📅 Bugungi vazifalar");
        row2.add("⏰ Muddati o'tganlar");

        KeyboardRow row3 = new KeyboardRow();
        row3.add("🏆 Mening maqomim");
        row3.add("🔍 Qidiruv");
        row3.add("ℹ️ Yordam");

        keyboard.add(row1);
        keyboard.add(row2);
        keyboard.add(row3);
        keyboardMarkup.setKeyboard(keyboard);
        return keyboardMarkup;
    }

    private void handleInlineQuery(org.telegram.telegrambots.meta.api.objects.inlinequery.InlineQuery inlineQuery) {
        if (inlineQuery == null || inlineQuery.getFrom() == null) {
            return;
        }
        Long tgUserId = inlineQuery.getFrom().getId();
        String queryText = inlineQuery.getQuery() != null ? inlineQuery.getQuery().trim().toLowerCase() : "";

        Optional<User> userOpt = userRepository.findByTelegramChatId(tgUserId);
        List<org.telegram.telegrambots.meta.api.objects.inlinequery.result.InlineQueryResult> results = new ArrayList<>();

        if (userOpt.isPresent()) {
            User user = userOpt.get();
            List<Task> activeTasks = taskRepository.findActiveTasksByUserId(user.getId());
            int count = 0;
            for (Task t : activeTasks) {
                if (t == null) continue;
                String code = t.getPublicId() != null ? t.getPublicId() : t.getId();
                String title = t.getTitle() != null ? t.getTitle() : "";
                if (!queryText.isEmpty()
                        && !title.toLowerCase().contains(queryText)
                        && !code.toLowerCase().contains(queryText)) {
                    continue;
                }

                org.telegram.telegrambots.meta.api.objects.inlinequery.inputmessagecontent.InputTextMessageContent msgContent =
                        new org.telegram.telegrambots.meta.api.objects.inlinequery.inputmessagecontent.InputTextMessageContent();
                String prio = t.getPriority() != null ? t.getPriority().name() : "MEDIUM";
                String dueStr = t.getDueDate() != null ? ("\n📅 Muddat: <b>" + t.getDueDate() + "</b>") : "";
                msgContent.setMessageText("📌 <b>[" + TelegramUtil.escapeHtml(code) + "] " + TelegramUtil.escapeHtml(title) + "</b>\n"
                        + "⚡ Muhimlik: <b>" + prio + "</b>" + dueStr);
                msgContent.setParseMode("HTML");

                org.telegram.telegrambots.meta.api.objects.inlinequery.result.InlineQueryResultArticle article =
                        new org.telegram.telegrambots.meta.api.objects.inlinequery.result.InlineQueryResultArticle();
                article.setId(t.getId());
                article.setTitle("[" + code + "] " + title);
                article.setDescription("Muhimlik: " + prio + (t.getDueDate() != null ? " | Muddat: " + t.getDueDate() : ""));
                article.setInputMessageContent(msgContent);
                results.add(article);

                if (++count >= 25) {
                    break;
                }
            }
        }

        org.telegram.telegrambots.meta.api.methods.AnswerInlineQuery answer =
                new org.telegram.telegrambots.meta.api.methods.AnswerInlineQuery();
        answer.setInlineQueryId(inlineQuery.getId());
        answer.setResults(results);
        answer.setCacheTime(5);
        answer.setIsPersonal(true);
        try {
            execute(answer);
        } catch (Exception e) {
            log.debug("InlineQuery javob qaytarishda xatolik: {}", e.getMessage());
        }
    }

    private boolean isGroupCommandOrHashtag(String text) {
        String lower = text.toLowerCase();
        return lower.startsWith("/link_group")
                || lower.startsWith("/unlink_group")
                || lower.startsWith("/task")
                || lower.startsWith("#task")
                || lower.startsWith("/leaderboard")
                || lower.startsWith("/group_stats");
    }

    private void handleGroupMessage(org.telegram.telegrambots.meta.api.objects.Message message) {
        Long groupChatId = message.getChatId();
        Integer topicId = message.getMessageThreadId();
        String rawText = message.getText() != null ? message.getText().trim() : "";
        Long senderTgId = message.getFrom() != null ? message.getFrom().getId() : null;

        // Strip @BotUsername from command if present
        String cmdText = rawText;
        if (botUsername != null && !botUsername.isBlank()) {
            cmdText = cmdText.replaceAll("(?i)@" + java.util.regex.Pattern.quote(botUsername), "");
        }

        if (cmdText.startsWith("/link_group")) {
            if (senderTgId == null) return;
            Optional<User> userOpt = userRepository.findByTelegramChatId(senderTgId);
            if (userOpt.isEmpty()) {
                sendGroupReply(groupChatId, topicId, "⚠️ Avval botning shaxsiy chatida (/start) akkauntingizni ulang.");
                return;
            }
            User user = userOpt.get();
            String arg = cmdText.length() > 11 ? cmdText.substring(11).trim() : "";
            if (arg.isEmpty()) {
                sendGroupReply(groupChatId, topicId, "ℹ️ Guruhni loyihaga ulash uchun: <code>/link_group LOYIHA_PREFIKSI_YOKI_ID</code> (masalan: <code>/link_group LOY</code>)");
                return;
            }
            Optional<Workspace> wsOpt = workspaceRepository.findByKeyPrefixIgnoreCase(arg)
                    .or(() -> workspaceRepository.findById(arg));
            if (wsOpt.isEmpty()) {
                sendGroupReply(groupChatId, topicId, "❌ Loyiha topilmadi: <b>" + TelegramUtil.escapeHtml(arg) + "</b>");
                return;
            }
            Workspace ws = wsOpt.get();
            if (!authorizationService.isOwnerOrAdmin(ws.getId(), user.getId())) {
                sendGroupReply(groupChatId, topicId, "⚠️ Faqat loyiha egasi (Owner) yoki Admin guruhni ulay oladi.");
                return;
            }
            ws.setTelegramGroupChatId(groupChatId);
            ws.setTelegramTopicId(topicId);
            workspaceRepository.save(ws);
            sendGroupReply(groupChatId, topicId, "✅ Ushbu guruh <b>" + TelegramUtil.escapeHtml(ws.getTitle())
                    + "</b> [" + TelegramUtil.escapeHtml(ws.getKeyPrefix() != null ? ws.getKeyPrefix() : "WFM") + "] loyihasiga ulandi!\n\n"
                    + "• Guruhda vazifa ochish: <code>/task Vazifa nomi</code> yoki <code>#task Vazifa nomi</code>\n"
                    + "• Jamoa peshqadamlari: <code>/leaderboard</code>");
            return;
        }

        if (cmdText.startsWith("/unlink_group")) {
            Optional<Workspace> wsOpt = workspaceRepository.findByTelegramGroupChatId(groupChatId);
            if (wsOpt.isEmpty()) {
                sendGroupReply(groupChatId, topicId, "ℹ️ Ushbu guruh hech qanday loyihaga ulanmagan.");
                return;
            }
            Workspace ws = wsOpt.get();
            ws.setTelegramGroupChatId(null);
            ws.setTelegramTopicId(null);
            workspaceRepository.save(ws);
            sendGroupReply(groupChatId, topicId, "🔌 Guruh <b>" + TelegramUtil.escapeHtml(ws.getTitle()) + "</b> loyihasidan uzildi.");
            return;
        }

        Optional<Workspace> linkedWsOpt = workspaceRepository.findByTelegramGroupChatId(groupChatId);
        if (linkedWsOpt.isEmpty()) {
            sendGroupReply(groupChatId, topicId, "⚠️ Ushbu guruh hali loyihaga ulanmagan. Ulash uchun: <code>/link_group LOYIHA_PREFIKSI</code>");
            return;
        }
        Workspace ws = linkedWsOpt.get();

        if (cmdText.startsWith("/leaderboard") || cmdText.startsWith("/group_stats")) {
            List<BoardColumn> cols = columnRepository.findByWorkspaceIdOrderByOrderAsc(ws.getId());
            java.util.Set<String> doneIds = cols.stream().filter(c -> Boolean.TRUE.equals(c.getIsDone())).map(BoardColumn::getId).collect(java.util.stream.Collectors.toSet());
            List<Task> allTasks = taskRepository.findByWorkspaceIdWithDetails(ws.getId()).stream()
                    .filter(t -> !Boolean.TRUE.equals(t.getIsArchived()))
                    .toList();
            long total = allTasks.size();
            long done = allTasks.stream().filter(t -> doneIds.contains(t.getColumnId())).count();
            int pct = total > 0 ? (int) Math.round((done * 100.0) / total) : 0;

            Map<String, Long> doneByMember = new java.util.HashMap<>();
            Map<String, String> memberNames = new java.util.HashMap<>();
            for (Task t : allTasks) {
                if (t.getAssignees() == null) continue;
                boolean isDone = doneIds.contains(t.getColumnId());
                for (User u : t.getAssignees()) {
                    memberNames.put(u.getId(), u.getFullName() != null ? u.getFullName() : u.getName());
                    doneByMember.putIfAbsent(u.getId(), 0L);
                    if (isDone) {
                        doneByMember.put(u.getId(), doneByMember.get(u.getId()) + 1L);
                    }
                }
            }

            StringBuilder sb = new StringBuilder();
            sb.append("🏆 <b>").append(TelegramUtil.escapeHtml(ws.getTitle())).append(" — Choyxona Leaderboard</b>\n\n");
            sb.append("📊 Umumiy bajarilish: <b>").append(done).append(" / ").append(total).append(" (").append(pct).append("%)</b>\n\n");
            if (doneByMember.isEmpty()) {
                sb.append("Hozircha biriktirilgan vazifalar yo'q.");
            } else {
                List<Map.Entry<String, Long>> sorted = doneByMember.entrySet().stream()
                        .sorted((a, b) -> Long.compare(b.getValue(), a.getValue()))
                        .limit(10)
                        .toList();
                int rank = 1;
                for (Map.Entry<String, Long> entry : sorted) {
                    String medal = rank == 1 ? "🥇" : (rank == 2 ? "🥈" : (rank == 3 ? "🥉" : "🔹"));
                    String nick = UserDto.calculateStatusNickname(entry.getValue());
                    sb.append(medal).append(" <b>").append(TelegramUtil.escapeHtml(memberNames.get(entry.getKey()))).append("</b> — ")
                      .append(entry.getValue()).append(" ta bajarilgan (<i>").append(TelegramUtil.escapeHtml(nick)).append("</i>)\n");
                    rank++;
                }
            }
            sendGroupReply(groupChatId, topicId, sb.toString());
            return;
        }

        if (cmdText.startsWith("/task") || cmdText.toLowerCase().startsWith("#task")) {
            if (senderTgId == null) return;
            Optional<User> senderOpt = userRepository.findByTelegramChatId(senderTgId);
            if (senderOpt.isEmpty()) {
                sendGroupReply(groupChatId, topicId, "⚠️ Vazifa yaratish uchun avval botga shaxsiy chatda /start bosing.");
                return;
            }
            User creator = senderOpt.get();
            String title = cmdText.replaceFirst("(?i)^(/task|#task)\\s*", "").trim();
            if (title.isEmpty() && message.getReplyToMessage() != null && message.getReplyToMessage().getText() != null) {
                title = message.getReplyToMessage().getText().trim();
            }
            if (title.isEmpty()) {
                sendGroupReply(groupChatId, topicId, "ℹ️ Vazifa nomini yozing: <code>/task Dizaynni tekshirish</code> yoki xabarga reply qilib <code>/task</code> deb yozing.");
                return;
            }
            if (title.length() > 255) {
                title = title.substring(0, 255);
            }
            List<BoardColumn> cols = columnRepository.findByWorkspaceIdOrderByOrderAsc(ws.getId());
            if (cols.isEmpty()) return;
            BoardColumn targetCol = cols.stream().filter(c -> !Boolean.TRUE.equals(c.getIsDone())).findFirst().orElse(cols.get(0));

            User assignee = creator;
            if (message.getReplyToMessage() != null && message.getReplyToMessage().getFrom() != null) {
                Long repliedTgId = message.getReplyToMessage().getFrom().getId();
                Optional<User> repliedUserOpt = userRepository.findByTelegramChatId(repliedTgId);
                if (repliedUserOpt.isPresent()) {
                    assignee = repliedUserOpt.get();
                }
            }

            String publicId = generateSequentialPublicId(ws.getId(), ws);
            String maxRank = taskRepository.findMaxLexoRankByColumnId(targetCol.getId());
            Task task = Task.builder()
                    .workspaceId(ws.getId())
                    .columnId(targetCol.getId())
                    .publicId(publicId)
                    .title(title)
                    .priority(Priority.MEDIUM)
                    .issueType(IssueType.TASK)
                    .lexoRank(LexoRankUtil.getMiddleSafe(maxRank, null))
                    .build();
            task.getAssignees().add(assignee);
            Task saved = taskRepository.save(task);

            if (webSocketNotifier != null) {
                webSocketNotifier.notifyWorkspace(ws.getId(), WebSocketEvent.builder()
                        .type("TASK_CREATED")
                        .workspaceId(ws.getId())
                        .data(TaskDto.fromEntity(saved))
                        .build());
            }

            String assigneeName = assignee.getFullName() != null ? assignee.getFullName() : assignee.getName();
            sendGroupReply(groupChatId, topicId, "📌 <b>Guruhdan yangi vazifa yaratildi!</b>\n"
                    + "🔑 Kod: <b>" + TelegramUtil.escapeHtml(saved.getPublicId()) + "</b>\n"
                    + "📋 Vazifa: <b>" + TelegramUtil.escapeHtml(saved.getTitle()) + "</b>\n"
                    + "👤 Mas'ul: <b>" + TelegramUtil.escapeHtml(assigneeName) + "</b>");
        }
    }

    private void sendGroupReply(Long chatId, Integer messageThreadId, String text) {
        SendMessage msg = new SendMessage();
        msg.setChatId(String.valueOf(chatId));
        if (messageThreadId != null) {
            msg.setMessageThreadId(messageThreadId);
        }
        msg.setText(text);
        msg.setParseMode("HTML");
        executeWithRetry(msg, chatId, 0);
    }

    private void handleTimerCommand(Long chatId) {
        Optional<User> userOpt = userRepository.findByTelegramChatId(chatId);
        if (userOpt.isEmpty()) {
            sendMessage(chatId, "Akkauntingiz ulanmagan. Iltimos, web ilovadan ulang.");
            return;
        }
        if (focusTimerService == null) {
            sendMessage(chatId, "⏱ Taymer xizmati vaqtincha mavjud emas.");
            return;
        }
        User user = userOpt.get();
        Optional<com.taskcenter.dto.FocusTimerDto> activeOpt = focusTimerService.getActiveTimer(user);
        if (activeOpt.isEmpty()) {
            sendMessage(chatId, "🍅 <b>Sizda hozircha faol Pomodoro / Fokus taymeri yo'q.</b>\n\n"
                    + "Istalgan vazifa kartochkasini ochib (/my_tasks yoki /tasks_today) <b>🍅 Fokus (25m)</b> tugmasini bosing!");
            return;
        }
        com.taskcenter.dto.FocusTimerDto tmr = activeOpt.get();
        long mins = tmr.getElapsedSeconds() != null ? tmr.getElapsedSeconds() / 60 : 0;
        long secs = tmr.getElapsedSeconds() != null ? tmr.getElapsedSeconds() % 60 : 0;
        String code = tmr.getTaskPublicId() != null ? "[" + tmr.getTaskPublicId() + "] " : "";
        String msg = "🍅 <b>Faol Pomodoro / Fokus Taymeri</b>\n\n"
                + "📋 Vazifa: <b>" + TelegramUtil.escapeHtml(code + tmr.getTaskTitle()) + "</b>\n"
                + "⏱ O'tgan vaqt: <b>" + String.format("%02d:%02d", mins, secs) + "</b> (Maqsad: " + tmr.getTargetMinutes() + " daq)";

        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        InlineKeyboardButton stopBtn = new InlineKeyboardButton();
        stopBtn.setText("⏹ To'xtatish va Worklogga yozish");
        stopBtn.setCallbackData("TIMER_STOP");
        markup.setKeyboard(List.of(List.of(stopBtn)));
        sendMessageWithInlineKeyboard(chatId, msg, markup);
    }

    private void handleTimerStartCallback(CallbackQuery callbackQuery, Long chatId, User user, String taskId) {
        if (focusTimerService == null) {
            answerCallback(callbackQuery, "Taymer xizmati mavjud emas");
            return;
        }
        try {
            com.taskcenter.dto.FocusTimerDto dto = focusTimerService.startTimer(taskId, 25, user);
            answerCallback(callbackQuery, "🍅 25 daqiqalik Fokus taymeri boshlandi!");
            String code = dto.getTaskPublicId() != null ? "[" + dto.getTaskPublicId() + "] " : "";
            InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
            InlineKeyboardButton stopBtn = new InlineKeyboardButton();
            stopBtn.setText("⏹ Taymerni to'xtatish va yozish");
            stopBtn.setCallbackData("TIMER_STOP");
            markup.setKeyboard(List.of(List.of(stopBtn)));
            sendMessageWithInlineKeyboard(chatId, "🍅 <b>Pomodoro Fokus taymeri yoqildi!</b>\n📋 Vazifa: <b>"
                    + TelegramUtil.escapeHtml(code + dto.getTaskTitle()) + "</b>\n⏱ Maqsad: <b>25 daqiqa</b>", markup);
        } catch (Exception e) {
            answerCallback(callbackQuery, "⚠️ " + e.getMessage());
        }
    }

    private void handleTimerStopCallback(CallbackQuery callbackQuery, Long chatId, User user) {
        if (focusTimerService == null) {
            answerCallback(callbackQuery, "Taymer xizmati mavjud emas");
            return;
        }
        try {
            com.taskcenter.dto.FocusTimerDto dto = focusTimerService.stopTimer(user);
            long mins = dto.getElapsedSeconds() != null ? Math.max(1L, Math.round(dto.getElapsedSeconds() / 60.0)) : 1L;
            answerCallback(callbackQuery, "⏱ Taymer to'xtatildi: +" + dto.getLoggedHoursAdded() + " soat yozildi!");
            sendMessage(chatId, "✅ <b>Fokus sessiyasi yakunlandi!</b>\n"
                    + "📋 Vazifa: <b>" + TelegramUtil.escapeHtml(dto.getTaskTitle()) + "</b>\n"
                    + "⏱ Davomiyligi: <b>" + mins + " daqiqa (" + dto.getLoggedHoursAdded() + " soat)</b> avtomatik worklogga qo'shildi.");
        } catch (Exception e) {
            answerCallback(callbackQuery, "⚠️ " + e.getMessage());
        }
    }

    private void handleTaskRecurrenceCycleCallback(CallbackQuery callbackQuery, Long chatId, User user, String taskId) {
        Optional<Task> taskOpt = taskRepository.findById(taskId);
        if (taskOpt.isEmpty()) {
            answerCallback(callbackQuery, "Vazifa topilmadi");
            return;
        }
        Task task = taskOpt.get();
        try {
            authorizationService.checkCanEdit(task.getWorkspaceId(), user);
        } catch (Exception e) {
            answerCallback(callbackQuery, "⚠️ Ruxsat yo'q");
            return;
        }
        com.taskcenter.model.RecurrenceRule current = task.getRecurrenceRule() != null
                ? task.getRecurrenceRule()
                : com.taskcenter.model.RecurrenceRule.NONE;
        com.taskcenter.model.RecurrenceRule next = switch (current) {
            case NONE -> com.taskcenter.model.RecurrenceRule.DAILY;
            case DAILY -> com.taskcenter.model.RecurrenceRule.WEEKDAYS;
            case WEEKDAYS -> com.taskcenter.model.RecurrenceRule.WEEKLY;
            case WEEKLY -> com.taskcenter.model.RecurrenceRule.MONTHLY;
            case MONTHLY -> com.taskcenter.model.RecurrenceRule.NONE;
        };
        task.setRecurrenceRule(next);
        task.setRecurrenceInterval(1);
        task.setNextOccurrenceDate(RecurringTaskService.computeNextOccurrence(
                task.getDueDate() != null ? task.getDueDate() : LocalDate.now(clock), next, 1));
        taskRepository.save(task);
        answerCallback(callbackQuery, "🔁 Takrorlanish: " + next.name());
        editTaskViewMessage(callbackQuery, chatId, task);
    }

    private void handleCalendarCommand(Long chatId) {
        Optional<User> userOpt = userRepository.findByTelegramChatId(chatId);
        if (userOpt.isEmpty()) {
            sendMessage(chatId, "Akkauntingiz ulanmagan. Iltimos, web ilovadan ulang.");
            return;
        }
        if (calendarAndReportService == null) {
            sendMessage(chatId, "Kalendar xizmati vaqtincha mavjud emas.");
            return;
        }
        com.taskcenter.dto.CalendarLinkDto link = calendarAndReportService.getOrCreateCalendarLink(userOpt.get(), false);
        String msg = "📆 <b>Kalendar Sinxronizatsiyasi (iCal / Google / Apple Calendar)</b>\n\n"
                + "Barcha muddatli vazifalaringiz telefoningiz yoki kompyuteringiz kalendarida avtomatik ko'rinib turishi uchun quyidagi havoladan foydalaning:\n\n"
                + "🔗 <b>iCal (.ics) havola:</b>\n<code>" + TelegramUtil.escapeHtml(link.getIcsUrl()) + "</code>\n\n"
                + "🍏 <b>Apple / Outlook (webcal):</b>\n<code>" + TelegramUtil.escapeHtml(link.getWebcalUrl()) + "</code>";
        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        InlineKeyboardButton gcalBtn = new InlineKeyboardButton();
        gcalBtn.setText("📅 Google Calendar'ga qo'shish");
        gcalBtn.setUrl(link.getGoogleCalendarUrl());
        markup.setKeyboard(List.of(List.of(gcalBtn)));
        sendMessageWithInlineKeyboard(chatId, msg, markup);
    }

    private void handleReportCommand(Long chatId) {
        Optional<User> userOpt = userRepository.findByTelegramChatId(chatId);
        if (userOpt.isEmpty()) {
            sendMessage(chatId, "Akkauntingiz ulanmagan. Iltimos, web ilovadan ulang.");
            return;
        }
        User user = userOpt.get();
        List<Workspace> workspaces = workspaceRepository.findByOwnerIdOrMemberUserId(user.getId());
        if (workspaces.isEmpty()) {
            sendMessage(chatId, "Sizda hali faol loyihalar yo'q.");
            return;
        }
        StringBuilder sb = new StringBuilder("📄 <b>Kunlik PDF Hisobotlar</b>\n\n");
        sb.append("Loyihangiz bo'yicha to'liq PDF hisobotni Mini App'ning <b>Analitika (Tahlil)</b> bo'limidan 1-klik bilan yuklab olishingiz mumkin:\n\n");
        for (Workspace ws : workspaces) {
            sb.append("• 🏢 <b>").append(TelegramUtil.escapeHtml(ws.getTitle())).append("</b> (<code>/api/workspaces/")
              .append(ws.getId()).append("/export/daily-report.pdf</code>)\n");
        }
        handleAppCommand(chatId);
        sendMessage(chatId, sb.toString());
    }
}
