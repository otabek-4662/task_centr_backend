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

@Slf4j
@Service
@org.springframework.boot.autoconfigure.condition.ConditionalOnProperty(name = "telegram.bot.token")
public class TelegramBotService extends TelegramLongPollingBot {

    private final String botUsername;
    private final UserRepository userRepository;
    private final TaskRepository taskRepository;
    private final ColumnRepository columnRepository;
    private final WorkspaceRepository workspaceRepository;

    public TelegramBotService(
            @Value("${telegram.bot.token:}") String botToken,
            @Value("${telegram.bot.username:}") String botUsername,
            UserRepository userRepository,
            TaskRepository taskRepository,
            ColumnRepository columnRepository,
            WorkspaceRepository workspaceRepository) {
        super(botToken);
        this.botUsername = botUsername;
        this.userRepository = userRepository;
        this.taskRepository = taskRepository;
        this.columnRepository = columnRepository;
        this.workspaceRepository = workspaceRepository;
    }

    @Override
    public String getBotUsername() {
        return botUsername;
    }

    @Override
    @Transactional
    public void onUpdateReceived(Update update) {
        if (update.hasCallbackQuery()) {
            handleCallbackQuery(update.getCallbackQuery());
        } else if (update.hasMessage() && update.getMessage().hasText()) {
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
            String token = parts[1].trim();
            Optional<User> userOpt = userRepository.findByTelegramLinkToken(token);
            if (userOpt.isPresent()) {
                User user = userOpt.get();
                user.setTelegramChatId(chatId);
                user.setTelegramLinkToken(null);
                userRepository.save(user);

                String welcomeMsg = "🎉 Davraga xush kelibsiz, " + (user.getFullName() != null ? user.getFullName() : user.getName()) + "!\n\n"
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
                sendMessageWithMainKeyboard(chatId, "Assalomu alaykum, " + user.getName() + "! 'Choylashamiz' botiga xush kelibsiz. Nima qilamiz?");
            } else {
                String help = "☕️ Assalomu alaykum! 'Choylashamiz' tizimining rasmiy botiga xush kelibsiz.\n\n"
                        + "Telegram orqali bildirishnomalarni olish va vazifalaringizni boshqarish uchun akkauntingizni ulang:\n"
                        + "1. Web ilovadagi 'Hujram' (Profil) sahifasiga kiring.\n"
                        + "2. 'Telegramni ulash' tugmasini bosing.\n"
                        + "3. Botga yuboriladigan havolani oching yoki berilgan 6 xonali kodni `/start <kod>` ko'rinishida yuboring.";
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

        String msg = "👤 Foydalanuvchi: " + (user.getFullName() != null ? user.getFullName() : user.getName()) + "\n"
                + "🏆 Maqomingiz: " + statusNickname + "\n"
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
            user.setTelegramChatId(null);
            userRepository.save(user);
            sendMessage(chatId, "Telegram akkauntingiz tizimdan muvaffaqiyatli uzildi. Qayta ulash uchun web ilovadan token oling.");
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

        try {
            execute(message);
        } catch (TelegramApiException e) {
            log.error("Telegram xabar yuborishda xatolik", e);
        }
    }

    private void handleCallbackQuery(CallbackQuery callbackQuery) {
        String callData = callbackQuery.getData();
        Long chatId = callbackQuery.getMessage().getChatId();

        if (callData.startsWith("TASK_VIEW_")) {
            String taskId = callData.substring(10);
            Optional<Task> taskOpt = taskRepository.findById(taskId);

            if (taskOpt.isPresent()) {
                Task task = taskOpt.get();
                Optional<BoardColumn> colOpt = columnRepository.findById(task.getColumnId());
                String colName = colOpt.isPresent() ? colOpt.get().getTitle() : "Noma'lum";

                String workspaceName = "Mavjud emas";
                if (task.getWorkspaceId() != null) {
                    Optional<Workspace> wsOpt = workspaceRepository.findById(task.getWorkspaceId());
                    if (wsOpt.isPresent()) {
                        workspaceName = wsOpt.get().getTitle();
                    }
                }

                String details = "📋 Bosh og'riq: " + task.getTitle() + "\n"
                        + "🏢 G'alva (Loyiha): " + workspaceName + "\n"
                        + "📊 Holati (Ustun): " + colName + "\n"
                        + "⚠️ Muhimligi: " + (task.getPriority() != null ? task.getPriority() : "O'rtacha") + "\n"
                        + (task.getDescription() != null && !task.getDescription().isBlank() ? "📝 Tafsilot: " + task.getDescription() : "");

                SendMessage message = new SendMessage();
                message.setChatId(String.valueOf(chatId));
                message.setText(details);

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

                try {
                    execute(message);
                } catch (TelegramApiException e) {
                    log.error("Telegram xabar yuborishda xatolik", e);
                }
            } else {
                sendMessage(chatId, "Bosh og'riq (vazifa) topilmadi.");
            }
        } else if (callData.startsWith("TASK_STATUS_SELECT_")) {
            String taskId = callData.substring(19);
            Optional<Task> taskOpt = taskRepository.findById(taskId);

            if (taskOpt.isPresent()) {
                Task task = taskOpt.get();
                List<BoardColumn> columns = columnRepository.findByWorkspaceIdOrderByOrderAsc(task.getWorkspaceId());

                SendMessage message = new SendMessage();
                message.setChatId(String.valueOf(chatId));
                message.setText("Ushbu vazifani qaysi ustunga o'tkazmoqchisiz?");

                InlineKeyboardMarkup markupInline = new InlineKeyboardMarkup();
                List<List<InlineKeyboardButton>> rowsInline = new ArrayList<>();

                for (BoardColumn col : columns) {
                    List<InlineKeyboardButton> rowInline = new ArrayList<>();
                    InlineKeyboardButton btn = new InlineKeyboardButton();
                    btn.setText(col.getTitle());
                    btn.setCallbackData("TASK_STATUS_CHANGE_" + taskId + "_" + col.getId());
                    rowInline.add(btn);
                    rowsInline.add(rowInline);
                }

                markupInline.setKeyboard(rowsInline);
                message.setReplyMarkup(markupInline);

                try {
                    execute(message);
                } catch (TelegramApiException e) {
                    log.error("Telegram xabar yuborishda xatolik", e);
                }
            } else {
                sendMessage(chatId, "Vazifa topilmadi.");
            }
        } else if (callData.startsWith("TASK_STATUS_CHANGE_")) {
            String remainder = callData.substring(19);
            int underscoreIndex = remainder.indexOf('_');
            if (underscoreIndex != -1) {
                String taskId = remainder.substring(0, underscoreIndex);
                String newColId = remainder.substring(underscoreIndex + 1);

                Optional<Task> taskOpt = taskRepository.findById(taskId);
                if (taskOpt.isPresent()) {
                    Task task = taskOpt.get();
                    task.setColumnId(newColId);
                    taskRepository.save(task);

                    Optional<BoardColumn> newColOpt = columnRepository.findById(newColId);
                    String colName = newColOpt.isPresent() ? newColOpt.get().getTitle() : "Noma'lum";

                    sendMessage(chatId, "✅ Vazifa holati muvaffaqiyatli o'zgartirildi: " + colName);
                } else {
                    sendMessage(chatId, "Vazifa topilmadi.");
                }
            }
        }
    }

    public void sendMessage(Long chatId, String text) {
        if (chatId == null) return;
        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setText(text);
        try {
            execute(message);
        } catch (TelegramApiException e) {
            log.error("Telegram xabar yuborishda xatolik", e);
        }
    }

    public void sendMessageWithMainKeyboard(Long chatId, String text) {
        if (chatId == null) return;
        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setText(text);
        message.setReplyMarkup(getMainKeyboard());
        try {
            execute(message);
        } catch (TelegramApiException e) {
            log.error("Telegram xabar yuborishda xatolik", e);
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
