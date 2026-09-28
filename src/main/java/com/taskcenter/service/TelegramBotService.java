package com.taskcenter.service;

import com.taskcenter.model.Task;
import com.taskcenter.model.User;
import com.taskcenter.repository.TaskRepository;
import com.taskcenter.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import com.taskcenter.model.BoardColumn;
import com.taskcenter.repository.ColumnRepository;

@Service
@org.springframework.boot.autoconfigure.condition.ConditionalOnProperty(name = "telegram.bot.token")
public class TelegramBotService extends TelegramLongPollingBot {

    private final String botUsername;
    private final UserRepository userRepository;
    private final TaskRepository taskRepository;
    private final ColumnRepository columnRepository;

    public TelegramBotService(
            @Value("${telegram.bot.token:}") String botToken,
            @Value("${telegram.bot.username:}") String botUsername,
            UserRepository userRepository,
            TaskRepository taskRepository,
            ColumnRepository columnRepository) {
        super(botToken);
        this.botUsername = botUsername;
        this.userRepository = userRepository;
        this.taskRepository = taskRepository;
        this.columnRepository = columnRepository;
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
            String text = update.getMessage().getText();
            Long chatId = update.getMessage().getChatId();

            if (text.startsWith("/start")) {
                String[] parts = text.split(" ");
                if (parts.length > 1) {
                    String token = parts[1];
                    Optional<User> userOpt = userRepository.findByTelegramLinkToken(token);
                    if (userOpt.isPresent()) {
                        User user = userOpt.get();
                        user.setTelegramChatId(chatId);
                        user.setTelegramLinkToken(null);
                        userRepository.save(user);
                        sendMessage(chatId, "Akkauntingiz muvaffaqiyatli ulandi!");
                    } else {
                        sendMessage(chatId, "Noto'g'ri yoki eskirgan token.");
                    }
                } else {
                    sendMessage(chatId, "Xush kelibsiz! Iltimos, tizim orqali akkauntingizni ulang.");
                }
            } else if (text.equals("/my_tasks")) {
                Optional<User> userOpt = userRepository.findByTelegramChatId(chatId);
                if (userOpt.isPresent()) {
                    List<Task> tasks = taskRepository.findActiveTasksByUserId(userOpt.get().getId());
                    if (tasks.isEmpty()) {
                        sendMessage(chatId, "Sizda faol vazifalar yo'q.");
                    } else {
                        sendTasksList(chatId, tasks);
                    }
                } else {
                    sendMessage(chatId, "Sizning akkauntingiz ulanmagan. Iltimos, tizim orqali ulang.");
                }
            }
        }
    }

    private void sendTasksList(Long chatId, List<Task> tasks) {
        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setText("Sizning faol vazifalaringiz:");

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
            e.printStackTrace();
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

                String details = "📋 Vazifa: " + task.getTitle() +
                        "\n📊 Holati (Ustun): " + colName +
                        "\n⚠️ Prioritet: " + task.getPriority() +
                        (task.getDescription() != null ? "\n📝 Ta'rif: " + task.getDescription() : "");
                
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
                    e.printStackTrace();
                }
            } else {
                sendMessage(chatId, "Vazifa topilmadi.");
            }
        } else if (callData.startsWith("TASK_STATUS_SELECT_")) {
            String taskId = callData.substring(19);
            Optional<Task> taskOpt = taskRepository.findById(taskId);
            
            if (taskOpt.isPresent()) {
                Task task = taskOpt.get();
                List<BoardColumn> columns = columnRepository.findByWorkspaceIdOrderByOrderAsc(task.getWorkspaceId());
                
                SendMessage message = new SendMessage();
                message.setChatId(String.valueOf(chatId));
                message.setText("Vazifa holatini (ustunni) tanlang:");

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
                    e.printStackTrace();
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
            e.printStackTrace();
        }
    }
}
