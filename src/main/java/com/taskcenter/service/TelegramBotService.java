package com.taskcenter.service;

import com.taskcenter.dto.UserDto;
import com.taskcenter.model.BoardColumn;
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
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import jakarta.annotation.PostConstruct;
import org.telegram.telegrambots.meta.api.methods.commands.SetMyCommands;
import org.telegram.telegrambots.meta.api.objects.commands.BotCommand;
import org.telegram.telegrambots.meta.api.objects.commands.scope.BotCommandScopeDefault;
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
            TaskExecutor taskExecutor) {
        super(botToken);
        this.botUsername = botUsername;
        this.userRepository = userRepository;
        this.taskRepository = taskRepository;
        this.columnRepository = columnRepository;
        this.workspaceRepository = workspaceRepository;
        this.authorizationService = authorizationService;
        this.taskExecutor = taskExecutor;
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
                new BotCommand("my_tasks", "Bosh og'riqlarim"),
                new BotCommand("status", "Mening maqomim"),
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
                handleStart(chatId, text);
            } else if (text.equals("/my_tasks") || text.equals("📌 Bosh og'riqlarim")) {
                handleMyTasks(chatId);
            } else if (text.equals("/status") || text.equals("🏆 Mening maqomim")) {
                handleStatus(chatId);
            } else if (text.equals("/help") || text.equals("ℹ️ Yordam")) {
                handleHelp(chatId);
            } else if (text.equals("/unlink")) {
                handleUnlink(chatId);
            } else {
                sendMessage(chatId, "Kechirasiz, bu buyruqni tushunmadim. Quyidagi menyu tugmalaridan foydalaning yoki /help buyrug'ini bering.");
            }
        }
    }

    private void handleStart(Long chatId, String text) {
        String[] parts = text.split(" ");
        if (parts.length > 1) {
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

            String token = parts[1].trim();
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
                        + "1. Web ilovadagi 'Hujram' (Profil) sahifasiga kiring.\n"
                        + "2. 'Telegramni ulash' tugmasini bosing.\n"
                        + "3. Botga yuboriladigan havolani oching yoki berilgan 6 xonali kodni <code>/start &lt;kod&gt;</code> ko'rinishida yuboring.";
                sendMessage(chatId, help);
            }
        }
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
        Optional<User> userOpt = userRepository.findByTelegramChatId(chatId);
        if (userOpt.isEmpty()) {
            sendMessage(chatId, "Akkauntingiz ulanmagan. Iltimos, web ilovadan ulang.");
            return;
        }

        User user = userOpt.get();
        long taskCount = taskRepository.countAssignedTasksByUserId(user.getId());
        String statusNickname = UserDto.calculateStatusNickname(taskCount);

        String nextRank;
        long tasksNeeded;
        if (taskCount < 1) {
            nextRank = "Do'konga chopuvchi";
            tasksNeeded = 1 - taskCount;
        } else if (taskCount < 11) {
            nextRank = "O'zimizdan";
            tasksNeeded = 11 - taskCount;
        } else if (taskCount < 26) {
            nextRank = "Ishonganimiz";
            tasksNeeded = 26 - taskCount;
        } else if (taskCount < 51) {
            nextRank = "Ko'cha ko'rgan";
            tasksNeeded = 51 - taskCount;
        } else if (taskCount < 101) {
            nextRank = "Katta uka";
            tasksNeeded = 101 - taskCount;
        } else if (taskCount < 201) {
            nextRank = "Katta aka";
            tasksNeeded = 201 - taskCount;
        } else {
            nextRank = "Eng yuqori cho'qqi!";
            tasksNeeded = 0;
        }

        String msg = "👤 Foydalanuvchi: <b>" + TelegramUtil.escapeHtml(user.getFullName() != null ? user.getFullName() : user.getName()) + "</b>\n"
                + "🏆 Maqomingiz: <b>" + TelegramUtil.escapeHtml(statusNickname) + "</b>\n"
                + "📌 Jami biriktirilgan vazifalar: " + taskCount + " ta\n";

        if (tasksNeeded > 0) {
            msg += "🚀 Keyingi daraja ('" + nextRank + "'): yana " + tasksNeeded + " ta vazifa qoldi!";
        } else {
            msg += "👑 Siz allaqachon eng oliy darajadasiz ('Katta aka')!";
        }

        sendMessage(chatId, msg);
    }

    private void handleHelp(Long chatId) {
        String help = "📌 Botdagi mavjud buyruqlar:\n\n"
                + "🔹 📌 Bosh og'riqlarim (/my_tasks) — Sizga biriktirilgan faol vazifalarni ko'rish va ularning holatini o'zgartirish\n"
                + "🔹 🏆 Mening maqomim (/status) — Maqomingiz va darajangiz haqida ma'lumot\n"
                + "🔹 ℹ️ Yordam (/help) — Ushbu qo'llanma\n"
                + "🔹 🚪 Akkauntni uzish (/unlink) — Telegram akkauntingizni tizimdan uzish";
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
        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setText("Sizning faol bosh og'riqlaringiz (vazifalar) ro'yxati:\n(Batafsil ko'rish uchun tanlang)");

        InlineKeyboardMarkup markupInline = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rowsInline = new ArrayList<>();

        for (Task t : tasks) {
            List<InlineKeyboardButton> rowInline = new ArrayList<>();
            InlineKeyboardButton button = new InlineKeyboardButton();
            button.setText("📌 " + t.getTitle());
            button.setCallbackData("TASK_VIEW_" + t.getId());
            rowInline.add(button);
            rowsInline.add(rowInline);
        }

        markupInline.setKeyboard(rowsInline);
        message.setReplyMarkup(markupInline);
        message.setParseMode("HTML");

        executeWithRetry(message, chatId, 0);
    }

    private void handleCallbackQuery(CallbackQuery callbackQuery) {
        String callData = callbackQuery.getData();
        Long chatId = callbackQuery.getMessage().getChatId();

        Optional<User> userOpt = userRepository.findByTelegramChatId(chatId);
        if (userOpt.isEmpty()) {
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
                    sendMessage(chatId, "⚠️ Ushbu vazifa bo'yicha ruxsatingiz yo'q (Loyihaga a'zo emassiz).");
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

                String details = "📋 Bosh og'riq: <b>" + TelegramUtil.escapeHtml(task.getTitle()) + "</b>\n"
                        + "🏢 G'alva (Loyiha): <b>" + TelegramUtil.escapeHtml(workspaceName) + "</b>\n"
                        + "📊 Holati (Ustun): <b>" + TelegramUtil.escapeHtml(colName) + "</b>\n"
                        + "⚠️ Muhimligi: <b>" + TelegramUtil.escapeHtml(task.getPriority() != null ? task.getPriority().name() : "O'rtacha") + "</b>\n"
                        + (task.getDescription() != null && !task.getDescription().isBlank() ? "📝 Tafsilot: " + TelegramUtil.escapeHtml(task.getDescription()) : "");

                SendMessage message = new SendMessage();
                message.setChatId(String.valueOf(chatId));
                message.setText(details);
                message.setParseMode("HTML");

                InlineKeyboardMarkup markupInline = new InlineKeyboardMarkup();
                List<List<InlineKeyboardButton>> rowsInline = new ArrayList<>();
                List<InlineKeyboardButton> rowInline = new ArrayList<>();

                InlineKeyboardButton statusBtn = new InlineKeyboardButton();
                statusBtn.setText("🔄 Holatni o'zgartirish");
                statusBtn.setCallbackData("TASK_STATUS_SELECT_" + task.getId());
                rowInline.add(statusBtn);
                rowsInline.add(rowInline);

                markupInline.setKeyboard(rowsInline);
                message.setReplyMarkup(markupInline);

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
                    sendMessage(chatId, "⚠️ Ushbu loyihaga a'zo emassiz, ruxsat yo'q.");
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
                sendMessage(chatId, "⚠️ Noto'g'ri ustun (boshqa loyihaga tegishli).");
                return;
            }

            task.setColumnId(targetCol.getId());
            taskRepository.save(task);
            sendMessage(chatId, "✅ Vazifa holati muvaffaqiyatli o'zgartirildi: " + targetCol.getTitle());
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

    private ReplyKeyboardMarkup getMainKeyboard() {
        ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();
        keyboardMarkup.setResizeKeyboard(true);
        keyboardMarkup.setSelective(true);

        List<KeyboardRow> keyboard = new ArrayList<>();
        KeyboardRow row1 = new KeyboardRow();
        row1.add("📌 Bosh og'riqlarim");
        row1.add("🏆 Mening maqomim");

        KeyboardRow row2 = new KeyboardRow();
        row2.add("ℹ️ Yordam");

        keyboard.add(row1);
        keyboard.add(row2);
        keyboardMarkup.setKeyboard(keyboard);
        return keyboardMarkup;
    }
}
