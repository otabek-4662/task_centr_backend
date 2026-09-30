package com.taskcenter.service;

import com.taskcenter.exception.ForbiddenException;
import com.taskcenter.model.BoardColumn;
import com.taskcenter.model.Task;
import com.taskcenter.model.User;
import com.taskcenter.model.Workspace;
import com.taskcenter.repository.ColumnRepository;
import com.taskcenter.repository.TaskRepository;
import com.taskcenter.repository.UserRepository;
import com.taskcenter.repository.WorkspaceRepository;
import com.taskcenter.util.TelegramUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.task.SyncTaskExecutor;
import org.springframework.core.task.TaskExecutor;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Chat;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.ResponseParameters;
import org.telegram.telegrambots.meta.exceptions.TelegramApiRequestException;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TelegramBotServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private TaskRepository taskRepository;
    @Mock
    private ColumnRepository columnRepository;
    @Mock
    private WorkspaceRepository workspaceRepository;
    @Mock
    private WorkspaceAuthorizationService authorizationService;

    private TaskExecutor taskExecutor = new SyncTaskExecutor();
    private TelegramBotService telegramBotService;

    @BeforeEach
    void setUp() {
        telegramBotService = spy(new TelegramBotService(
                "dummy-token",
                "dummy-bot",
                userRepository,
                taskRepository,
                columnRepository,
                workspaceRepository,
                authorizationService,
                taskExecutor
        ));

        try {
            lenient().doReturn(null).when(telegramBotService).execute(any(SendMessage.class));
        } catch (Exception e) {
            // ignore
        }
    }

    @Test
    void onUpdateReceived_start_expiredToken_fails() throws Exception {
        Update update = new Update();
        Message message = mock(Message.class);
        Chat chat = new Chat();
        chat.setType("private");
        when(message.getChat()).thenReturn(chat);
        when(message.hasText()).thenReturn(true);
        when(message.getText()).thenReturn("/start EXPIRED_TOKEN");
        when(message.getChatId()).thenReturn(12345L);
        update.setMessage(message);

        User user = User.builder()
                .id("u1")
                .telegramLinkToken("EXPIRED_TOKEN")
                .telegramLinkTokenExpiresAt(LocalDateTime.now().minusMinutes(5)) // muddati o'tgan
                .build();

        when(userRepository.findByTelegramLinkToken("EXPIRED_TOKEN")).thenReturn(Optional.of(user));

        telegramBotService.onUpdateReceived(update);

        verify(userRepository, never()).save(any());
        
        ArgumentCaptor<SendMessage> captor = ArgumentCaptor.forClass(SendMessage.class);
        verify(telegramBotService).execute(captor.capture());
        assertThat(captor.getValue().getText()).contains("muddati eskirgan");
    }

    @Test
    void onUpdateReceived_start_rateLimit_blocksAfter5Attempts() throws Exception {
        Update update = new Update();
        Message message = mock(Message.class);
        Chat chat = new Chat();
        chat.setType("private");
        when(message.getChat()).thenReturn(chat);
        when(message.hasText()).thenReturn(true);
        when(message.getText()).thenReturn("/start BAD_TOKEN");
        when(message.getChatId()).thenReturn(999L);
        update.setMessage(message);

        when(userRepository.findByTelegramLinkToken("BAD_TOKEN")).thenReturn(Optional.empty());

        for (int i = 0; i < 6; i++) {
            telegramBotService.onUpdateReceived(update);
        }

        ArgumentCaptor<SendMessage> captor = ArgumentCaptor.forClass(SendMessage.class);
        verify(telegramBotService, times(6)).execute(captor.capture());
        
        assertThat(captor.getAllValues().get(5).getText()).contains("juda ko'p marotaba");
    }

    @Test
    void onUpdateReceived_callback_unlinkedChat_fails() throws Exception {
        Update update = new Update();
        CallbackQuery callbackQuery = new CallbackQuery();
        callbackQuery.setData("TASK_VIEW_123");
        Message message = mock(Message.class);
        Chat chat = new Chat();
        chat.setType("private");
        when(message.getChat()).thenReturn(chat);
        when(message.getChatId()).thenReturn(111L);
        callbackQuery.setMessage(message);
        update.setCallbackQuery(callbackQuery);

        when(userRepository.findByTelegramChatId(111L)).thenReturn(Optional.empty());

        telegramBotService.onUpdateReceived(update);

        ArgumentCaptor<SendMessage> captor = ArgumentCaptor.forClass(SendMessage.class);
        verify(telegramBotService).execute(captor.capture());
        assertThat(captor.getValue().getText()).contains("hali ulanmagan");
    }

    @Test
    void onUpdateReceived_callback_unauthorizedUser_fails() throws Exception {
        Update update = new Update();
        CallbackQuery callbackQuery = new CallbackQuery();
        callbackQuery.setData("TASK_VIEW_task1");
        Message message = mock(Message.class);
        Chat chat = new Chat();
        chat.setType("private");
        when(message.getChat()).thenReturn(chat);
        when(message.getChatId()).thenReturn(222L);
        callbackQuery.setMessage(message);
        update.setCallbackQuery(callbackQuery);

        User user = User.builder().id("u1").telegramChatId(222L).build();
        when(userRepository.findByTelegramChatId(222L)).thenReturn(Optional.of(user));

        Task task = Task.builder().id("task1").workspaceId("ws1").build();
        when(taskRepository.findById("task1")).thenReturn(Optional.of(task));

        doThrow(new RuntimeException("Ruxsat yo'q")).when(authorizationService).checkAccess("ws1", user);

        telegramBotService.onUpdateReceived(update);

        ArgumentCaptor<SendMessage> captor = ArgumentCaptor.forClass(SendMessage.class);
        verify(telegramBotService).execute(captor.capture());
        assertThat(captor.getValue().getText()).contains("ruxsatingiz yo'q");
    }

    @Test
    void sendMessage_403Error_clearsChatId() throws Exception {
        Long chatId = 123L;
        User user = User.builder().id("u1").telegramChatId(chatId).build();
        when(userRepository.findByTelegramChatId(chatId)).thenReturn(Optional.of(user));

        TelegramApiRequestException exception = 
                new TelegramApiRequestException("Forbidden: bot was blocked by the user");
        
        doThrow(exception).when(telegramBotService).execute(any(SendMessage.class));

        telegramBotService.sendMessage(chatId, "Hello");

        verify(userRepository).save(argThat(u -> u.getTelegramChatId() == null));
    }

    @Test
    void sendMessage_OtherError_keepsChatId() throws Exception {
        Long chatId = 456L;
        org.telegram.telegrambots.meta.exceptions.TelegramApiException exception = 
                new org.telegram.telegrambots.meta.exceptions.TelegramApiException("Some other network error");
        
        doThrow(exception).when(telegramBotService).execute(any(SendMessage.class));

        telegramBotService.sendMessage(chatId, "Hello");

        verify(userRepository, never()).save(any());
        verify(userRepository, never()).findByTelegramChatId(any());
    }

    @Test
    void onUpdateReceived_nonPrivateChat_isIgnored() throws Exception {
        Update update = new Update();
        Message message = mock(Message.class);
        Chat chat = new Chat();
        chat.setType("group");
        when(message.getChat()).thenReturn(chat);
        when(message.hasText()).thenReturn(true);
        when(message.getChatId()).thenReturn(333L);
        update.setMessage(message);

        telegramBotService.onUpdateReceived(update);

        ArgumentCaptor<SendMessage> captor = ArgumentCaptor.forClass(SendMessage.class);
        verify(telegramBotService).execute(captor.capture());
        assertThat(captor.getValue().getText()).contains("faqat shaxsiy chatda ishlaydi");
    }

    @Test
    void onUpdateReceived_callback_groupChat_isIgnored() throws Exception {
        Update update = new Update();
        CallbackQuery callbackQuery = new CallbackQuery();
        callbackQuery.setData("TASK_VIEW_123");
        Message message = mock(Message.class);
        Chat chat = new Chat();
        chat.setType("group");
        when(message.getChat()).thenReturn(chat);
        callbackQuery.setMessage(message);
        update.setCallbackQuery(callbackQuery);

        telegramBotService.onUpdateReceived(update);

        verify(telegramBotService, never()).sendMessage(any(), any());
        verify(userRepository, never()).findByTelegramChatId(any());
    }

    @Test
    void onUpdateReceived_callback_viewTask_success_escapesHtml() throws Exception {
        Update update = new Update();
        CallbackQuery callbackQuery = new CallbackQuery();
        callbackQuery.setData("TASK_VIEW_task1");
        Message message = mock(Message.class);
        Chat chat = new Chat();
        chat.setType("private");
        when(message.getChat()).thenReturn(chat);
        when(message.getChatId()).thenReturn(222L);
        callbackQuery.setMessage(message);
        update.setCallbackQuery(callbackQuery);

        User user = User.builder().id("u1").telegramChatId(222L).build();
        when(userRepository.findByTelegramChatId(222L)).thenReturn(Optional.of(user));

        Task task = Task.builder()
                .id("task1")
                .workspaceId("ws1")
                .title("Task <name> & _desc")
                .description("Bad <script>")
                .priority(com.taskcenter.model.Priority.HIGH)
                .columnId("col1")
                .build();
        when(taskRepository.findById("task1")).thenReturn(Optional.of(task));
        doReturn(new com.taskcenter.model.Workspace()).when(authorizationService).checkAccess("ws1", user);

        telegramBotService.onUpdateReceived(update);

        ArgumentCaptor<SendMessage> captor = ArgumentCaptor.forClass(SendMessage.class);
        verify(telegramBotService).execute(captor.capture());
        String text = captor.getValue().getText();
        assertThat(text).contains("Task &lt;name&gt; &amp; _desc");
        assertThat(text).contains("Bad &lt;script&gt;");
        assertThat(captor.getValue().getParseMode()).isEqualTo("HTML");
    }

    @Test
    void sendMessage_429Error_retriesOnce() throws Exception {
        Long chatId = 777L;
        TelegramApiRequestException exception = mock(TelegramApiRequestException.class);
        when(exception.getErrorCode()).thenReturn(429);
        ResponseParameters params = new ResponseParameters();
        params.setRetryAfter(1); // 1 sec
        when(exception.getParameters()).thenReturn(params);
        
        doThrow(exception).doReturn(null).when(telegramBotService).execute(any(SendMessage.class));

        telegramBotService.sendMessage(chatId, "Retry test");

        verify(telegramBotService, timeout(2000).times(2)).execute(any(SendMessage.class));
    }

    @Test
    void onUpdateReceived_unlinkCommand_showsConfirmation() throws Exception {
        Update update = new Update();
        Message message = mock(Message.class);
        Chat chat = new Chat();
        chat.setType("private");
        when(message.getChat()).thenReturn(chat);
        when(message.hasText()).thenReturn(true);
        when(message.getText()).thenReturn("/unlink");
        when(message.getChatId()).thenReturn(555L);
        update.setMessage(message);

        User user = User.builder().id("u1").telegramChatId(555L).build();
        when(userRepository.findByTelegramChatId(555L)).thenReturn(Optional.of(user));

        telegramBotService.onUpdateReceived(update);

        ArgumentCaptor<SendMessage> captor = ArgumentCaptor.forClass(SendMessage.class);
        verify(telegramBotService).execute(captor.capture());
        assertThat(captor.getValue().getText()).contains("uzmoqchimisiz");
        assertThat(captor.getValue().getReplyMarkup()).isNotNull();
    }

    @Test
    void initCommands_success() throws Exception {
        // execute is mocked to return null normally, so this shouldn't throw
        telegramBotService.initCommands();
        // Just verify it doesn't throw and log doesn't fail
    }

    @Test
    void initCommands_failsSilently() throws Exception {
        doThrow(new RuntimeException("API error")).when(telegramBotService).execute(any(org.telegram.telegrambots.meta.api.methods.commands.SetMyCommands.class));
        // Should not throw exception, should just catch and log
        telegramBotService.initCommands();
    }

    @Test
    void handleTelegramMessageEvent_worksWithoutTransaction() throws Exception {
        TelegramMessageEvent event = new TelegramMessageEvent(12345L, "Test fallback execution");
        
        telegramBotService.handleTelegramMessageEvent(event);
        
        ArgumentCaptor<SendMessage> captor = ArgumentCaptor.forClass(SendMessage.class);
        verify(telegramBotService).execute(captor.capture());
        assertThat(captor.getValue().getChatId()).isEqualTo("12345");
        assertThat(captor.getValue().getText()).isEqualTo("Test fallback execution");
    }

    @Test
    void callbackData_length_isWithin64Bytes_withUuid() {
        String uuid = UUID.randomUUID().toString(); // 36 characters
        assertThat(uuid).hasSize(36);

        // TASK_VIEW from TelegramUtil
        String taskViewUtil = TelegramUtil.createTaskViewKeyboard(uuid, "Test Task").getKeyboard().get(0).get(0).getCallbackData();
        assertThat(taskViewUtil.getBytes(StandardCharsets.UTF_8).length).isLessThanOrEqualTo(64);

        // TASK_VIEW from Bot / Scheduler
        String taskView = "TASK_VIEW_" + uuid;
        assertThat(taskView.getBytes(StandardCharsets.UTF_8).length).isLessThanOrEqualTo(64);

        // TASK_STATUS_SELECT
        String statusSelect = "TASK_STATUS_SELECT_" + uuid;
        assertThat(statusSelect.getBytes(StandardCharsets.UTF_8).length).isLessThanOrEqualTo(64);

        // TASK_STATUS_CHANGE with column order number (e.g. 1, 10, 100)
        String statusChange = "TASK_STATUS_CHANGE_" + uuid + "_1";
        assertThat(statusChange.getBytes(StandardCharsets.UTF_8).length).isLessThanOrEqualTo(64);

        String statusChangeLargeOrder = "TASK_STATUS_CHANGE_" + uuid + "_99999";
        assertThat(statusChangeLargeOrder.getBytes(StandardCharsets.UTF_8).length).isLessThanOrEqualTo(64);

        // UNLINK buttons
        String unlinkYes = "UNLINK_YES_" + uuid;
        assertThat(unlinkYes.getBytes(StandardCharsets.UTF_8).length).isLessThanOrEqualTo(64);

        String unlinkNo = "UNLINK_NO";
        assertThat(unlinkNo.getBytes(StandardCharsets.UTF_8).length).isLessThanOrEqualTo(64);
    }

    @Test
    void onUpdateReceived_callback_statusChange_success() throws Exception {
        Long chatId = 601L;
        String taskId = UUID.randomUUID().toString();
        String wsId = "ws-1";

        User user = User.builder().id("u1").telegramChatId(chatId).build();
        when(userRepository.findByTelegramChatId(chatId)).thenReturn(Optional.of(user));

        Task task = Task.builder()
                .id(taskId)
                .workspaceId(wsId)
                .columnId("col-1")
                .title("Test Task")
                .build();
        when(taskRepository.findById(taskId)).thenReturn(Optional.of(task));

        Workspace ws = Workspace.builder().id(wsId).title("Test WS").build();
        when(authorizationService.checkAccess(wsId, user)).thenReturn(ws);
        when(authorizationService.checkCanEdit(wsId, user)).thenReturn(ws);

        BoardColumn col1 = BoardColumn.builder().id("col-1").workspaceId(wsId).title("To Do").order(1).build();
        BoardColumn col2 = BoardColumn.builder().id("col-2").workspaceId(wsId).title("Done").order(2).build();
        when(columnRepository.findByWorkspaceIdOrderByOrderAsc(wsId)).thenReturn(List.of(col1, col2));

        Update update = new Update();
        CallbackQuery callbackQuery = new CallbackQuery();
        callbackQuery.setData("TASK_STATUS_CHANGE_" + taskId + "_2");
        Message message = mock(Message.class);
        Chat chat = new Chat();
        chat.setType("private");
        when(message.getChat()).thenReturn(chat);
        when(message.getChatId()).thenReturn(chatId);
        callbackQuery.setMessage(message);
        update.setCallbackQuery(callbackQuery);

        telegramBotService.onUpdateReceived(update);

        assertThat(task.getColumnId()).isEqualTo("col-2");
        verify(taskRepository).save(task);

        ArgumentCaptor<SendMessage> captor = ArgumentCaptor.forClass(SendMessage.class);
        verify(telegramBotService).execute(captor.capture());
        assertThat(captor.getValue().getText()).contains("muvaffaqiyatli o'zgartirildi");
        assertThat(captor.getValue().getText()).contains("Done");
    }

    @Test
    void onUpdateReceived_callback_statusChange_columnOrderOutOfRange_rejected() throws Exception {
        Long chatId = 602L;
        String taskId = UUID.randomUUID().toString();
        String wsId = "ws-1";

        User user = User.builder().id("u1").telegramChatId(chatId).build();
        when(userRepository.findByTelegramChatId(chatId)).thenReturn(Optional.of(user));

        Task task = Task.builder()
                .id(taskId)
                .workspaceId(wsId)
                .columnId("col-1")
                .title("Test Task")
                .build();
        when(taskRepository.findById(taskId)).thenReturn(Optional.of(task));

        Workspace ws = Workspace.builder().id(wsId).title("Test WS").build();
        when(authorizationService.checkAccess(wsId, user)).thenReturn(ws);
        when(authorizationService.checkCanEdit(wsId, user)).thenReturn(ws);

        BoardColumn col1 = BoardColumn.builder().id("col-1").workspaceId(wsId).title("To Do").order(1).build();
        when(columnRepository.findByWorkspaceIdOrderByOrderAsc(wsId)).thenReturn(List.of(col1));

        Update update = new Update();
        CallbackQuery callbackQuery = new CallbackQuery();
        callbackQuery.setData("TASK_STATUS_CHANGE_" + taskId + "_99");
        Message message = mock(Message.class);
        Chat chat = new Chat();
        chat.setType("private");
        when(message.getChat()).thenReturn(chat);
        when(message.getChatId()).thenReturn(chatId);
        callbackQuery.setMessage(message);
        update.setCallbackQuery(callbackQuery);

        telegramBotService.onUpdateReceived(update);

        assertThat(task.getColumnId()).isEqualTo("col-1");
        verify(taskRepository, never()).save(any());

        ArgumentCaptor<SendMessage> captor = ArgumentCaptor.forClass(SendMessage.class);
        verify(telegramBotService).execute(captor.capture());
        assertThat(captor.getValue().getText()).contains("Yangi ustun topilmadi");
    }

    @Test
    void onUpdateReceived_callback_statusChange_columnFromOtherWorkspace_rejected() throws Exception {
        Long chatId = 603L;
        String taskId = UUID.randomUUID().toString();
        String wsId = "ws-1";

        User user = User.builder().id("u1").telegramChatId(chatId).build();
        when(userRepository.findByTelegramChatId(chatId)).thenReturn(Optional.of(user));

        Task task = Task.builder()
                .id(taskId)
                .workspaceId(wsId)
                .columnId("col-1")
                .title("Test Task")
                .build();
        when(taskRepository.findById(taskId)).thenReturn(Optional.of(task));

        Workspace ws = Workspace.builder().id(wsId).title("Test WS").build();
        when(authorizationService.checkAccess(wsId, user)).thenReturn(ws);
        when(authorizationService.checkCanEdit(wsId, user)).thenReturn(ws);

        BoardColumn otherWsCol = BoardColumn.builder().id("col-other").workspaceId("ws-other").title("Other WS Col").order(2).build();
        when(columnRepository.findByWorkspaceIdOrderByOrderAsc(wsId)).thenReturn(List.of(otherWsCol));

        Update update = new Update();
        CallbackQuery callbackQuery = new CallbackQuery();
        callbackQuery.setData("TASK_STATUS_CHANGE_" + taskId + "_2");
        Message message = mock(Message.class);
        Chat chat = new Chat();
        chat.setType("private");
        when(message.getChat()).thenReturn(chat);
        when(message.getChatId()).thenReturn(chatId);
        callbackQuery.setMessage(message);
        update.setCallbackQuery(callbackQuery);

        telegramBotService.onUpdateReceived(update);

        assertThat(task.getColumnId()).isEqualTo("col-1");
        verify(taskRepository, never()).save(any());

        ArgumentCaptor<SendMessage> captor = ArgumentCaptor.forClass(SendMessage.class);
        verify(telegramBotService).execute(captor.capture());
        assertThat(captor.getValue().getText()).contains("boshqa loyihaga tegishli");
    }

    @Test
    void onUpdateReceived_callback_statusChange_oldFormat_handledGracefully() throws Exception {
        Long chatId = 604L;
        String taskId = UUID.randomUUID().toString();
        String oldColUuid = UUID.randomUUID().toString();

        User user = User.builder().id("u1").telegramChatId(chatId).build();
        when(userRepository.findByTelegramChatId(chatId)).thenReturn(Optional.of(user));

        Update update = new Update();
        CallbackQuery callbackQuery = new CallbackQuery();
        callbackQuery.setData("TASK_STATUS_CHANGE_" + taskId + "_" + oldColUuid);
        Message message = mock(Message.class);
        Chat chat = new Chat();
        chat.setType("private");
        when(message.getChat()).thenReturn(chat);
        when(message.getChatId()).thenReturn(chatId);
        callbackQuery.setMessage(message);
        update.setCallbackQuery(callbackQuery);

        telegramBotService.onUpdateReceived(update);

        verify(taskRepository, never()).save(any());

        ArgumentCaptor<SendMessage> captor = ArgumentCaptor.forClass(SendMessage.class);
        verify(telegramBotService).execute(captor.capture());
        assertThat(captor.getValue().getText()).contains("Bu tugma eskirgan, xabarni qayta oching");
    }

    @Test
    void onUpdateReceived_callback_unknownFormat_handledGracefully() throws Exception {
        Long chatId = 605L;

        User user = User.builder().id("u1").telegramChatId(chatId).build();
        when(userRepository.findByTelegramChatId(chatId)).thenReturn(Optional.of(user));

        Update update = new Update();
        CallbackQuery callbackQuery = new CallbackQuery();
        callbackQuery.setData("UNKNOWN_FORMAT_CALLBACK");
        Message message = mock(Message.class);
        Chat chat = new Chat();
        chat.setType("private");
        when(message.getChat()).thenReturn(chat);
        when(message.getChatId()).thenReturn(chatId);
        callbackQuery.setMessage(message);
        update.setCallbackQuery(callbackQuery);

        telegramBotService.onUpdateReceived(update);

        ArgumentCaptor<SendMessage> captor = ArgumentCaptor.forClass(SendMessage.class);
        verify(telegramBotService).execute(captor.capture());
        assertThat(captor.getValue().getText()).contains("Bu tugma eskirgan, xabarni qayta oching");
    }

    @Test
    void onUpdateReceived_callback_statusChange_viewerRole_cannotChangeStatus() throws Exception {
        Long chatId = 606L;
        String taskId = UUID.randomUUID().toString();
        String wsId = "ws-1";

        User user = User.builder().id("u1").telegramChatId(chatId).build();
        when(userRepository.findByTelegramChatId(chatId)).thenReturn(Optional.of(user));

        Task task = Task.builder()
                .id(taskId)
                .workspaceId(wsId)
                .columnId("col-1")
                .title("Test Task")
                .build();
        when(taskRepository.findById(taskId)).thenReturn(Optional.of(task));

        Workspace ws = Workspace.builder().id(wsId).title("Test WS").build();
        when(authorizationService.checkAccess(wsId, user)).thenReturn(ws);
        doThrow(new ForbiddenException("Kuzatuvchi (Viewer) roli ma'lumotlarni o'zgartira olmaydi"))
                .when(authorizationService).checkCanEdit(wsId, user);

        Update update = new Update();
        CallbackQuery callbackQuery = new CallbackQuery();
        callbackQuery.setData("TASK_STATUS_CHANGE_" + taskId + "_2");
        Message message = mock(Message.class);
        Chat chat = new Chat();
        chat.setType("private");
        when(message.getChat()).thenReturn(chat);
        when(message.getChatId()).thenReturn(chatId);
        callbackQuery.setMessage(message);
        update.setCallbackQuery(callbackQuery);

        telegramBotService.onUpdateReceived(update);

        assertThat(task.getColumnId()).isEqualTo("col-1");
        verify(taskRepository, never()).save(any());

        ArgumentCaptor<SendMessage> captor = ArgumentCaptor.forClass(SendMessage.class);
        verify(telegramBotService).execute(captor.capture());
        assertThat(captor.getValue().getText()).contains("⚠️ Ruxsat yo'q.");
    }
}
