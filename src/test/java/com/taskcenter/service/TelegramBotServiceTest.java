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
import com.taskcenter.model.TaskActivityType;
import com.taskcenter.repository.TelegramReminderLogRepository;
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
import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Chat;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.ResponseParameters;
import org.telegram.telegrambots.meta.exceptions.TelegramApiRequestException;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
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
    @Mock
    private TaskActivityService activityService;
    @Mock
    private TelegramNotificationService telegramNotificationService;
    @Mock
    private TelegramReminderLogRepository telegramReminderLogRepository;
    @Mock
    private WebSocketNotifier webSocketNotifier;
    @Mock
    private com.taskcenter.repository.WorkspaceInvitationRepository invitationRepository;
    @Mock
    private com.taskcenter.repository.WorkspaceMemberRepository memberRepository;

    private static final ZoneId ZONE = ZoneId.of("Asia/Tashkent");
    private final Clock clock = Clock.fixed(
            LocalDate.of(2026, 10, 1).atStartOfDay(ZONE).toInstant(), ZONE);
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
                taskExecutor,
                clock,
                activityService,
                telegramNotificationService,
                telegramReminderLogRepository,
                webSocketNotifier,
                "https://test.url",
                invitationRepository,
                memberRepository
        ));

        try {
            lenient().doReturn(null).when(telegramBotService).execute(any(SendMessage.class));
            lenient().doReturn(true).when(telegramBotService).execute(any(AnswerCallbackQuery.class));
            lenient().doReturn(null).when(telegramBotService).execute(any(EditMessageText.class));
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
        String taskViewUtil = TelegramUtil.createTaskViewKeyboard(uuid, "Test Task", "url").getKeyboard().get(0).get(0).getCallbackData();
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

        // TASK_DONE
        String taskDone = "TASK_DONE_" + uuid;
        assertThat(taskDone.getBytes(StandardCharsets.UTF_8).length).isLessThanOrEqualTo(64);

        // TASK_SNOOZE
        String taskSnooze = "TASK_SNOOZE_" + uuid;
        assertThat(taskSnooze.getBytes(StandardCharsets.UTF_8).length).isLessThanOrEqualTo(64);
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
        assertThat(captor.getValue().getText()).contains("boshqa g'alvaga tegishli");
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

    // --- TASK_DONE Tests ---

    @Test
    void onUpdateReceived_callback_taskDone_success() throws Exception {
        Long chatId = 700L;
        String taskId = UUID.randomUUID().toString();
        String wsId = "ws-done";

        User user = User.builder().id("u1").telegramChatId(chatId).build();
        when(userRepository.findByTelegramChatId(chatId)).thenReturn(Optional.of(user));

        Task task = Task.builder()
                .id(taskId)
                .workspaceId(wsId)
                .columnId("col-1")
                .title("Test Done Task")
                .lexoRank("aaa")
                .build();
        task.getAssignees().add(user);
        when(taskRepository.findById(taskId)).thenReturn(Optional.of(task));

        Workspace ws = Workspace.builder().id(wsId).title("Test WS").ownerId("owner1").build();
        when(authorizationService.checkCanEdit(wsId, user)).thenReturn(ws);

        BoardColumn col1 = BoardColumn.builder().id("col-1").workspaceId(wsId).title("To Do").order(1).isDone(false).build();
        BoardColumn doneCol = BoardColumn.builder().id("col-done").workspaceId(wsId).title("Done").order(2).isDone(true).build();
        when(columnRepository.findByWorkspaceIdOrderByOrderAsc(wsId)).thenReturn(List.of(col1, doneCol));
        when(columnRepository.findById("col-done")).thenReturn(Optional.of(doneCol));
        when(workspaceRepository.findById(wsId)).thenReturn(Optional.of(ws));

        Update update = new Update();
        CallbackQuery callbackQuery = new CallbackQuery();
        callbackQuery.setId("cb-700");
        callbackQuery.setData("TASK_DONE_" + taskId);
        Message message = mock(Message.class);
        Chat chat = new Chat();
        chat.setType("private");
        when(message.getChat()).thenReturn(chat);
        when(message.getChatId()).thenReturn(chatId);
        when(message.getMessageId()).thenReturn(100);
        callbackQuery.setMessage(message);
        update.setCallbackQuery(callbackQuery);

        telegramBotService.onUpdateReceived(update);

        assertThat(task.getColumnId()).isEqualTo("col-done");
        verify(taskRepository).save(task);
        verify(activityService).logActivity(eq(taskId), eq(user), eq(TaskActivityType.STATUS_UPDATED),
                eq("columnId"), eq("col-1"), eq("col-done"));
        verify(telegramNotificationService).sendTaskMovedNotification(eq(task), eq(user), eq("To Do"), eq("Done"));
    }

    @Test
    void onUpdateReceived_callback_taskDone_alreadyDone() throws Exception {
        Long chatId = 701L;
        String taskId = UUID.randomUUID().toString();
        String wsId = "ws-done";

        User user = User.builder().id("u1").telegramChatId(chatId).build();
        when(userRepository.findByTelegramChatId(chatId)).thenReturn(Optional.of(user));

        Task task = Task.builder()
                .id(taskId)
                .workspaceId(wsId)
                .columnId("col-done")
                .title("Already Done Task")
                .build();
        task.getAssignees().add(user);
        when(taskRepository.findById(taskId)).thenReturn(Optional.of(task));

        Workspace ws = Workspace.builder().id(wsId).title("Test WS").ownerId("owner1").build();
        when(authorizationService.checkCanEdit(wsId, user)).thenReturn(ws);

        BoardColumn doneCol = BoardColumn.builder().id("col-done").workspaceId(wsId).title("Done").order(2).isDone(true).build();
        when(columnRepository.findByWorkspaceIdOrderByOrderAsc(wsId)).thenReturn(List.of(doneCol));

        Update update = new Update();
        CallbackQuery callbackQuery = new CallbackQuery();
        callbackQuery.setId("cb-701");
        callbackQuery.setData("TASK_DONE_" + taskId);
        Message message = mock(Message.class);
        Chat chat = new Chat();
        chat.setType("private");
        when(message.getChat()).thenReturn(chat);
        when(message.getChatId()).thenReturn(chatId);
        callbackQuery.setMessage(message);
        update.setCallbackQuery(callbackQuery);

        telegramBotService.onUpdateReceived(update);

        assertThat(task.getColumnId()).isEqualTo("col-done");
        verify(taskRepository, never()).save(any());

        ArgumentCaptor<AnswerCallbackQuery> answerCaptor = ArgumentCaptor.forClass(AnswerCallbackQuery.class);
        verify(telegramBotService).execute(answerCaptor.capture());
        assertThat(answerCaptor.getValue().getText()).contains("Allaqachon bajarilgan");
    }

    @Test
    void onUpdateReceived_callback_taskDone_noDoneColumn() throws Exception {
        Long chatId = 702L;
        String taskId = UUID.randomUUID().toString();
        String wsId = "ws-done";

        User user = User.builder().id("u1").telegramChatId(chatId).build();
        when(userRepository.findByTelegramChatId(chatId)).thenReturn(Optional.of(user));

        Task task = Task.builder()
                .id(taskId)
                .workspaceId(wsId)
                .columnId("col-1")
                .title("No Done Column Task")
                .build();
        task.getAssignees().add(user);
        when(taskRepository.findById(taskId)).thenReturn(Optional.of(task));

        Workspace ws = Workspace.builder().id(wsId).title("Test WS").ownerId("owner1").build();
        when(authorizationService.checkCanEdit(wsId, user)).thenReturn(ws);

        BoardColumn col1 = BoardColumn.builder().id("col-1").workspaceId(wsId).title("To Do").order(1).isDone(false).build();
        BoardColumn col2 = BoardColumn.builder().id("col-2").workspaceId(wsId).title("In Progress").order(2).isDone(false).build();
        when(columnRepository.findByWorkspaceIdOrderByOrderAsc(wsId)).thenReturn(List.of(col1, col2));

        Update update = new Update();
        CallbackQuery callbackQuery = new CallbackQuery();
        callbackQuery.setId("cb-702");
        callbackQuery.setData("TASK_DONE_" + taskId);
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

        ArgumentCaptor<AnswerCallbackQuery> answerCaptor = ArgumentCaptor.forClass(AnswerCallbackQuery.class);
        verify(telegramBotService).execute(answerCaptor.capture());
        assertThat(answerCaptor.getValue().getText()).contains("bajarilgan' ustun belgilanmagan");
    }

    @Test
    void onUpdateReceived_callback_taskDone_viewerRole_rejected() throws Exception {
        Long chatId = 703L;
        String taskId = UUID.randomUUID().toString();
        String wsId = "ws-done";

        User user = User.builder().id("u-viewer").telegramChatId(chatId).build();
        when(userRepository.findByTelegramChatId(chatId)).thenReturn(Optional.of(user));

        Task task = Task.builder()
                .id(taskId)
                .workspaceId(wsId)
                .columnId("col-1")
                .title("Viewer Task")
                .build();
        when(taskRepository.findById(taskId)).thenReturn(Optional.of(task));

        doThrow(new ForbiddenException("Kuzatuvchi (Viewer) roli ma'lumotlarni o'zgartira olmaydi"))
                .when(authorizationService).checkCanEdit(wsId, user);

        Update update = new Update();
        CallbackQuery callbackQuery = new CallbackQuery();
        callbackQuery.setId("cb-703");
        callbackQuery.setData("TASK_DONE_" + taskId);
        Message message = mock(Message.class);
        Chat chat = new Chat();
        chat.setType("private");
        when(message.getChat()).thenReturn(chat);
        when(message.getChatId()).thenReturn(chatId);
        callbackQuery.setMessage(message);
        update.setCallbackQuery(callbackQuery);

        telegramBotService.onUpdateReceived(update);

        verify(taskRepository, never()).save(any());

        ArgumentCaptor<AnswerCallbackQuery> answerCaptor = ArgumentCaptor.forClass(AnswerCallbackQuery.class);
        verify(telegramBotService).execute(answerCaptor.capture());
        assertThat(answerCaptor.getValue().getText()).contains("Ruxsat yo'q");
    }

    @Test
    void onUpdateReceived_callback_taskDone_notMember_rejected() throws Exception {
        Long chatId = 704L;
        String taskId = UUID.randomUUID().toString();
        String wsId = "ws-done";

        User user = User.builder().id("u-outsider").telegramChatId(chatId).build();
        when(userRepository.findByTelegramChatId(chatId)).thenReturn(Optional.of(user));

        Task task = Task.builder()
                .id(taskId)
                .workspaceId(wsId)
                .columnId("col-1")
                .title("Outsider Task")
                .build();
        when(taskRepository.findById(taskId)).thenReturn(Optional.of(task));

        doThrow(new ForbiddenException("A'zo emassiz"))
                .when(authorizationService).checkCanEdit(wsId, user);

        Update update = new Update();
        CallbackQuery callbackQuery = new CallbackQuery();
        callbackQuery.setId("cb-704");
        callbackQuery.setData("TASK_DONE_" + taskId);
        Message message = mock(Message.class);
        Chat chat = new Chat();
        chat.setType("private");
        when(message.getChat()).thenReturn(chat);
        when(message.getChatId()).thenReturn(chatId);
        callbackQuery.setMessage(message);
        update.setCallbackQuery(callbackQuery);

        telegramBotService.onUpdateReceived(update);

        verify(taskRepository, never()).save(any());

        ArgumentCaptor<AnswerCallbackQuery> answerCaptor = ArgumentCaptor.forClass(AnswerCallbackQuery.class);
        verify(telegramBotService).execute(answerCaptor.capture());
        assertThat(answerCaptor.getValue().getText()).contains("Ruxsat yo'q");
    }

    // --- TASK_SNOOZE Tests ---

    @Test
    void onUpdateReceived_callback_taskSnooze_success_changesDateAndClearsLogs() throws Exception {
        Long chatId = 710L;
        String taskId = UUID.randomUUID().toString();
        String wsId = "ws-snooze";

        User user = User.builder().id("u1").telegramChatId(chatId).build();
        when(userRepository.findByTelegramChatId(chatId)).thenReturn(Optional.of(user));

        Task task = Task.builder()
                .id(taskId)
                .workspaceId(wsId)
                .columnId("col-1")
                .title("Snooze Task")
                .dueDate(LocalDate.of(2026, 10, 1))
                .build();
        task.getAssignees().add(user);
        when(taskRepository.findById(taskId)).thenReturn(Optional.of(task));

        Workspace ws = Workspace.builder().id(wsId).title("Test WS").ownerId("owner1").build();
        when(authorizationService.checkCanEdit(wsId, user)).thenReturn(ws);
        when(columnRepository.findById("col-1")).thenReturn(Optional.of(
                BoardColumn.builder().id("col-1").workspaceId(wsId).title("To Do").order(1).build()));
        when(workspaceRepository.findById(wsId)).thenReturn(Optional.of(ws));

        Update update = new Update();
        CallbackQuery callbackQuery = new CallbackQuery();
        callbackQuery.setId("cb-710");
        callbackQuery.setData("TASK_SNOOZE_" + taskId);
        Message message = mock(Message.class);
        Chat chat = new Chat();
        chat.setType("private");
        when(message.getChat()).thenReturn(chat);
        when(message.getChatId()).thenReturn(chatId);
        when(message.getMessageId()).thenReturn(200);
        callbackQuery.setMessage(message);
        update.setCallbackQuery(callbackQuery);

        telegramBotService.onUpdateReceived(update);

        // tomorrow = 2026-10-02 (clock is fixed at 2026-10-01)
        assertThat(task.getDueDate()).isEqualTo(LocalDate.of(2026, 10, 2));
        verify(taskRepository).save(task);
        verify(telegramReminderLogRepository).deleteByTaskId(taskId);
        verify(activityService).logActivity(eq(taskId), eq(user), eq(TaskActivityType.DUE_DATE_SNOOZED),
                eq("dueDate"), eq("2026-10-01"), eq("2026-10-02"));
    }

    @Test
    void onUpdateReceived_callback_taskSnooze_laterDueDate_unchanged() throws Exception {
        Long chatId = 711L;
        String taskId = UUID.randomUUID().toString();
        String wsId = "ws-snooze";

        User user = User.builder().id("u1").telegramChatId(chatId).build();
        when(userRepository.findByTelegramChatId(chatId)).thenReturn(Optional.of(user));

        Task task = Task.builder()
                .id(taskId)
                .workspaceId(wsId)
                .columnId("col-1")
                .title("Later Snooze Task")
                .dueDate(LocalDate.of(2026, 12, 25))
                .build();
        task.getAssignees().add(user);
        when(taskRepository.findById(taskId)).thenReturn(Optional.of(task));

        Workspace ws = Workspace.builder().id(wsId).title("Test WS").ownerId("owner1").build();
        when(authorizationService.checkCanEdit(wsId, user)).thenReturn(ws);

        Update update = new Update();
        CallbackQuery callbackQuery = new CallbackQuery();
        callbackQuery.setId("cb-711");
        callbackQuery.setData("TASK_SNOOZE_" + taskId);
        Message message = mock(Message.class);
        Chat chat = new Chat();
        chat.setType("private");
        when(message.getChat()).thenReturn(chat);
        when(message.getChatId()).thenReturn(chatId);
        callbackQuery.setMessage(message);
        update.setCallbackQuery(callbackQuery);

        telegramBotService.onUpdateReceived(update);

        assertThat(task.getDueDate()).isEqualTo(LocalDate.of(2026, 12, 25));
        verify(taskRepository, never()).save(any());

        ArgumentCaptor<AnswerCallbackQuery> answerCaptor = ArgumentCaptor.forClass(AnswerCallbackQuery.class);
        verify(telegramBotService).execute(answerCaptor.capture());
        assertThat(answerCaptor.getValue().getText()).contains("ertadan keyin");
    }

    // --- Group chat filter for new buttons ---

    @Test
    void onUpdateReceived_callback_taskDone_groupChat_ignored() throws Exception {
        Update update = new Update();
        CallbackQuery callbackQuery = new CallbackQuery();
        callbackQuery.setData("TASK_DONE_" + UUID.randomUUID());
        Message message = mock(Message.class);
        Chat chat = new Chat();
        chat.setType("group");
        when(message.getChat()).thenReturn(chat);
        callbackQuery.setMessage(message);
        update.setCallbackQuery(callbackQuery);

        telegramBotService.onUpdateReceived(update);

        verify(userRepository, never()).findByTelegramChatId(any());
        verify(taskRepository, never()).findById(any(String.class));
    }

    @Test
    void onUpdateReceived_callback_taskSnooze_groupChat_ignored() throws Exception {
        Update update = new Update();
        CallbackQuery callbackQuery = new CallbackQuery();
        callbackQuery.setData("TASK_SNOOZE_" + UUID.randomUUID());
        Message message = mock(Message.class);
        Chat chat = new Chat();
        chat.setType("group");
        when(message.getChat()).thenReturn(chat);
        callbackQuery.setMessage(message);
        update.setCallbackQuery(callbackQuery);

        telegramBotService.onUpdateReceived(update);

        verify(userRepository, never()).findByTelegramChatId(any());
        verify(taskRepository, never()).findById(any(String.class));
    }

    @Test
    void onUpdateReceived_callback_taskDone_deletedTask_rejected() throws Exception {
        Long chatId = 705L;
        String taskId = UUID.randomUUID().toString();
        String wsId = "ws-done";

        User user = User.builder().id("u1").telegramChatId(chatId).build();
        when(userRepository.findByTelegramChatId(chatId)).thenReturn(Optional.of(user));

        Task task = Task.builder()
                .id(taskId)
                .workspaceId(wsId)
                .columnId("col-1")
                .title("Deleted Task")
                .deletedAt(LocalDateTime.now())
                .build();
        when(taskRepository.findById(taskId)).thenReturn(Optional.of(task));

        Update update = new Update();
        CallbackQuery callbackQuery = new CallbackQuery();
        callbackQuery.setId("cb-705");
        callbackQuery.setData("TASK_DONE_" + taskId);
        Message message = mock(Message.class);
        Chat chat = new Chat();
        chat.setType("private");
        when(message.getChat()).thenReturn(chat);
        when(message.getChatId()).thenReturn(chatId);
        callbackQuery.setMessage(message);
        update.setCallbackQuery(callbackQuery);

        telegramBotService.onUpdateReceived(update);

        verify(taskRepository, never()).save(any());
        verify(authorizationService, never()).checkCanEdit(any(), any());

        ArgumentCaptor<AnswerCallbackQuery> answerCaptor = ArgumentCaptor.forClass(AnswerCallbackQuery.class);
        verify(telegramBotService).execute(answerCaptor.capture());
        assertThat(answerCaptor.getValue().getText()).contains("arxivlangan yoki o'chirilgan");
    }

    @Test
    void onUpdateReceived_callback_taskSnooze_deletedTask_rejected() throws Exception {
        Long chatId = 712L;
        String taskId = UUID.randomUUID().toString();
        String wsId = "ws-snooze";

        User user = User.builder().id("u1").telegramChatId(chatId).build();
        when(userRepository.findByTelegramChatId(chatId)).thenReturn(Optional.of(user));

        Task task = Task.builder()
                .id(taskId)
                .workspaceId(wsId)
                .columnId("col-1")
                .title("Deleted Snooze Task")
                .dueDate(LocalDate.of(2026, 10, 1))
                .deletedAt(LocalDateTime.now())
                .build();
        when(taskRepository.findById(taskId)).thenReturn(Optional.of(task));

        Update update = new Update();
        CallbackQuery callbackQuery = new CallbackQuery();
        callbackQuery.setId("cb-712");
        callbackQuery.setData("TASK_SNOOZE_" + taskId);
        Message message = mock(Message.class);
        Chat chat = new Chat();
        chat.setType("private");
        when(message.getChat()).thenReturn(chat);
        when(message.getChatId()).thenReturn(chatId);
        callbackQuery.setMessage(message);
        update.setCallbackQuery(callbackQuery);

        telegramBotService.onUpdateReceived(update);

        verify(taskRepository, never()).save(any());
        verify(authorizationService, never()).checkCanEdit(any(), any());

        ArgumentCaptor<AnswerCallbackQuery> answerCaptor = ArgumentCaptor.forClass(AnswerCallbackQuery.class);
        verify(telegramBotService).execute(answerCaptor.capture());
        assertThat(answerCaptor.getValue().getText()).contains("arxivlangan yoki o'chirilgan");
    }

    @Test
    void onUpdateReceived_callback_taskSnooze_viewerRole_rejected() throws Exception {
        Long chatId = 713L;
        String taskId = UUID.randomUUID().toString();
        String wsId = "ws-snooze";

        User user = User.builder().id("u-viewer").telegramChatId(chatId).build();
        when(userRepository.findByTelegramChatId(chatId)).thenReturn(Optional.of(user));

        Task task = Task.builder()
                .id(taskId)
                .workspaceId(wsId)
                .columnId("col-1")
                .title("Viewer Snooze Task")
                .dueDate(LocalDate.of(2026, 10, 1))
                .build();
        when(taskRepository.findById(taskId)).thenReturn(Optional.of(task));

        doThrow(new ForbiddenException("Kuzatuvchi (Viewer) roli ma'lumotlarni o'zgartira olmaydi"))
                .when(authorizationService).checkCanEdit(wsId, user);

        Update update = new Update();
        CallbackQuery callbackQuery = new CallbackQuery();
        callbackQuery.setId("cb-713");
        callbackQuery.setData("TASK_SNOOZE_" + taskId);
        Message message = mock(Message.class);
        Chat chat = new Chat();
        chat.setType("private");
        when(message.getChat()).thenReturn(chat);
        when(message.getChatId()).thenReturn(chatId);
        callbackQuery.setMessage(message);
        update.setCallbackQuery(callbackQuery);

        telegramBotService.onUpdateReceived(update);

        verify(taskRepository, never()).save(any());

        ArgumentCaptor<AnswerCallbackQuery> answerCaptor = ArgumentCaptor.forClass(AnswerCallbackQuery.class);
        verify(telegramBotService).execute(answerCaptor.capture());
        assertThat(answerCaptor.getValue().getText()).contains("Ruxsat yo'q");
    }

    @Test
    void onUpdateReceived_callback_taskSnooze_notMember_rejected() throws Exception {
        Long chatId = 714L;
        String taskId = UUID.randomUUID().toString();
        String wsId = "ws-snooze";

        User user = User.builder().id("u-outsider").telegramChatId(chatId).build();
        when(userRepository.findByTelegramChatId(chatId)).thenReturn(Optional.of(user));

        Task task = Task.builder()
                .id(taskId)
                .workspaceId(wsId)
                .columnId("col-1")
                .title("Outsider Snooze Task")
                .dueDate(LocalDate.of(2026, 10, 1))
                .build();
        when(taskRepository.findById(taskId)).thenReturn(Optional.of(task));

        doThrow(new ForbiddenException("A'zo emassiz"))
                .when(authorizationService).checkCanEdit(wsId, user);

        Update update = new Update();
        CallbackQuery callbackQuery = new CallbackQuery();
        callbackQuery.setId("cb-714");
        callbackQuery.setData("TASK_SNOOZE_" + taskId);
        Message message = mock(Message.class);
        Chat chat = new Chat();
        chat.setType("private");
        when(message.getChat()).thenReturn(chat);
        when(message.getChatId()).thenReturn(chatId);
        callbackQuery.setMessage(message);
        update.setCallbackQuery(callbackQuery);

        telegramBotService.onUpdateReceived(update);

        verify(taskRepository, never()).save(any());

        ArgumentCaptor<AnswerCallbackQuery> answerCaptor = ArgumentCaptor.forClass(AnswerCallbackQuery.class);
        verify(telegramBotService).execute(answerCaptor.capture());
        assertThat(answerCaptor.getValue().getText()).contains("Ruxsat yo'q");
    }

    @Test
    void onUpdateReceived_callback_taskDone_unlinkedChat_rejected() throws Exception {
        Long chatId = 715L;
        String taskId = UUID.randomUUID().toString();

        when(userRepository.findByTelegramChatId(chatId)).thenReturn(Optional.empty());

        Update update = new Update();
        CallbackQuery callbackQuery = new CallbackQuery();
        callbackQuery.setId("cb-715");
        callbackQuery.setData("TASK_DONE_" + taskId);
        Message message = mock(Message.class);
        Chat chat = new Chat();
        chat.setType("private");
        when(message.getChat()).thenReturn(chat);
        when(message.getChatId()).thenReturn(chatId);
        callbackQuery.setMessage(message);
        update.setCallbackQuery(callbackQuery);

        telegramBotService.onUpdateReceived(update);

        verify(taskRepository, never()).findById(any(String.class));
        ArgumentCaptor<SendMessage> captor = ArgumentCaptor.forClass(SendMessage.class);
        verify(telegramBotService).execute(captor.capture());
        assertThat(captor.getValue().getText()).contains("hali ulanmagan");
    }

    @Test
    void onUpdateReceived_callback_taskSnooze_unlinkedChat_rejected() throws Exception {
        Long chatId = 716L;
        String taskId = UUID.randomUUID().toString();

        when(userRepository.findByTelegramChatId(chatId)).thenReturn(Optional.empty());

        Update update = new Update();
        CallbackQuery callbackQuery = new CallbackQuery();
        callbackQuery.setId("cb-716");
        callbackQuery.setData("TASK_SNOOZE_" + taskId);
        Message message = mock(Message.class);
        Chat chat = new Chat();
        chat.setType("private");
        when(message.getChat()).thenReturn(chat);
        when(message.getChatId()).thenReturn(chatId);
        callbackQuery.setMessage(message);
        update.setCallbackQuery(callbackQuery);

        telegramBotService.onUpdateReceived(update);

        verify(taskRepository, never()).findById(any(String.class));
        ArgumentCaptor<SendMessage> captor = ArgumentCaptor.forClass(SendMessage.class);
        verify(telegramBotService).execute(captor.capture());
        assertThat(captor.getValue().getText()).contains("hali ulanmagan");
    }

    @Test
    void onUpdateReceived_callback_taskSnooze_tashkent0100_snoozesToNextDay() throws Exception {
        // Create a separate service with a clock fixed at UTC 20:00 (which is 01:00 the next day in Tashkent)
        java.time.Instant utc20 = java.time.Instant.parse("2026-10-01T20:00:00Z");
        Clock customClock = Clock.fixed(utc20, ZoneId.of("Asia/Tashkent"));
        
        TelegramBotService customService = spy(new TelegramBotService(
                "dummy", "dummy", userRepository, taskRepository, columnRepository,
                workspaceRepository, authorizationService, taskExecutor, customClock,
                activityService, telegramNotificationService, telegramReminderLogRepository,
                webSocketNotifier, "https://test.url"
        ));
        
        try {
            lenient().doReturn(null).when(customService).execute(any(SendMessage.class));
            lenient().doReturn(true).when(customService).execute(any(AnswerCallbackQuery.class));
            lenient().doReturn(null).when(customService).execute(any(EditMessageText.class));
        } catch (Exception e) {}
        
        Long chatId = 717L;
        String taskId = "task-tashkent-tz";
        
        User user = User.builder().id("u1").telegramChatId(chatId).build();
        when(userRepository.findByTelegramChatId(chatId)).thenReturn(Optional.of(user));
        
        Task task = Task.builder()
                .id(taskId)
                .workspaceId("ws-1")
                .columnId("col-1")
                .dueDate(LocalDate.of(2026, 10, 1))
                .build();
        task.getAssignees().add(user);
        when(taskRepository.findById(taskId)).thenReturn(Optional.of(task));
        when(authorizationService.checkCanEdit("ws-1", user)).thenReturn(new Workspace());
        
        Update update = new Update();
        CallbackQuery callbackQuery = new CallbackQuery();
        callbackQuery.setId("cb-717");
        callbackQuery.setData("TASK_SNOOZE_" + taskId);
        Message message = mock(Message.class);
        Chat chat = new Chat();
        chat.setType("private");
        when(message.getChat()).thenReturn(chat);
        when(message.getChatId()).thenReturn(chatId);
        callbackQuery.setMessage(message);
        update.setCallbackQuery(callbackQuery);
        
        customService.onUpdateReceived(update);
        
        ArgumentCaptor<Task> taskCaptor = ArgumentCaptor.forClass(Task.class);
        verify(taskRepository).save(taskCaptor.capture());
        
        // At UTC 2026-10-01T20:00:00, Tashkent is 2026-10-02T01:00:00
        // Plus 1 day snooze -> 2026-10-03
        assertThat(taskCaptor.getValue().getDueDate()).isEqualTo(LocalDate.of(2026, 10, 3));
    }

    // --- STATS Tests ---

    @Test
    void onUpdateReceived_statsCommand_showsFullDashboard() throws Exception {
        Long chatId = 801L;
        User user = User.builder().id("u1").name("bek").fullName("Bekmurod").telegramChatId(chatId).build();
        when(userRepository.findByTelegramChatId(chatId)).thenReturn(Optional.of(user));

        com.taskcenter.dto.UserTaskStatsProjection stats = mock(com.taskcenter.dto.UserTaskStatsProjection.class);
        when(stats.getTotalAssigned()).thenReturn(20L);
        when(stats.getCompletedCount()).thenReturn(15L);
        when(stats.getActiveCount()).thenReturn(5L);
        when(stats.getOverdueCount()).thenReturn(2L);
        when(stats.getDueTodayCount()).thenReturn(3L);

        when(taskRepository.getUserTaskStats("u1", LocalDate.of(2026, 10, 1))).thenReturn(stats);

        Update update = new Update();
        Message message = mock(Message.class);
        Chat chat = new Chat();
        chat.setType("private");
        when(message.getChat()).thenReturn(chat);
        when(message.hasText()).thenReturn(true);
        when(message.getText()).thenReturn("/stats");
        when(message.getChatId()).thenReturn(chatId);
        update.setMessage(message);

        telegramBotService.onUpdateReceived(update);

        ArgumentCaptor<SendMessage> captor = ArgumentCaptor.forClass(SendMessage.class);
        verify(telegramBotService).execute(captor.capture());
        String text = captor.getValue().getText();

        assertThat(text).contains("Shaxsiy unumdorlik statistikasi");
        assertThat(text).contains("Bekmurod");
        assertThat(text).contains("O'zimizdan");
        assertThat(text).contains("Jami biriktirilgan: <b>20 ta</b>");
        assertThat(text).contains("Faol (jarayonda): <b>5 ta</b>");
        assertThat(text).contains("Bajarilgan: <b>15 ta</b> (Samaradorlik: <b>75%</b>)");
        assertThat(text).contains("Muddati o'tgan: <b>2 ta</b>");
        assertThat(text).contains("Bugun kutilayotgan: <b>3 ta</b>");
        assertThat(captor.getValue().getReplyMarkup()).isNotNull();
    }

    @Test
    void onUpdateReceived_callback_statsRefresh_editsMessage() throws Exception {
        Long chatId = 802L;
        User user = User.builder().id("u1").name("bek").telegramChatId(chatId).build();
        when(userRepository.findByTelegramChatId(chatId)).thenReturn(Optional.of(user));

        com.taskcenter.dto.UserTaskStatsProjection stats = mock(com.taskcenter.dto.UserTaskStatsProjection.class);
        when(stats.getTotalAssigned()).thenReturn(5L);
        when(stats.getCompletedCount()).thenReturn(5L);
        when(stats.getActiveCount()).thenReturn(0L);
        when(stats.getOverdueCount()).thenReturn(0L);
        when(stats.getDueTodayCount()).thenReturn(0L);

        when(taskRepository.getUserTaskStats("u1", LocalDate.of(2026, 10, 1))).thenReturn(stats);

        Update update = new Update();
        CallbackQuery callbackQuery = new CallbackQuery();
        callbackQuery.setId("cb-802");
        callbackQuery.setData("STATS_REFRESH");
        Message message = mock(Message.class);
        Chat chat = new Chat();
        chat.setType("private");
        when(message.getChat()).thenReturn(chat);
        when(message.getChatId()).thenReturn(chatId);
        when(message.getMessageId()).thenReturn(555);
        callbackQuery.setMessage(message);
        update.setCallbackQuery(callbackQuery);

        telegramBotService.onUpdateReceived(update);

        ArgumentCaptor<EditMessageText> editCaptor = ArgumentCaptor.forClass(EditMessageText.class);
        verify(telegramBotService).execute(editCaptor.capture());
        assertThat(editCaptor.getValue().getText()).contains("Shaxsiy unumdorlik statistikasi");
    }

    @Test
    void onUpdateReceived_overdueCommand_empty_showsNiceMessage() throws Exception {
        Long chatId = 803L;
        User user = User.builder().id("u1").telegramChatId(chatId).build();
        when(userRepository.findByTelegramChatId(chatId)).thenReturn(Optional.of(user));
        when(taskRepository.findOverdueTasksByUserId("u1", LocalDate.of(2026, 10, 1))).thenReturn(List.of());

        Update update = new Update();
        Message message = mock(Message.class);
        Chat chat = new Chat();
        chat.setType("private");
        when(message.getChat()).thenReturn(chat);
        when(message.hasText()).thenReturn(true);
        when(message.getText()).thenReturn("/overdue");
        when(message.getChatId()).thenReturn(chatId);
        update.setMessage(message);

        telegramBotService.onUpdateReceived(update);

        ArgumentCaptor<SendMessage> captor = ArgumentCaptor.forClass(SendMessage.class);
        verify(telegramBotService).execute(captor.capture());
        assertThat(captor.getValue().getText()).contains("birorta ham muddati o'tgan bosh og'riq yo'q");
    }

    @Test
    void onUpdateReceived_overdueCommand_withTasks_listsTasks() throws Exception {
        Long chatId = 804L;
        User user = User.builder().id("u1").telegramChatId(chatId).build();
        when(userRepository.findByTelegramChatId(chatId)).thenReturn(Optional.of(user));

        Task t1 = Task.builder().id("t1").title("Overdue Task 1").build();
        when(taskRepository.findOverdueTasksByUserId("u1", LocalDate.of(2026, 10, 1))).thenReturn(List.of(t1));

        Update update = new Update();
        Message message = mock(Message.class);
        Chat chat = new Chat();
        chat.setType("private");
        when(message.getChat()).thenReturn(chat);
        when(message.hasText()).thenReturn(true);
        when(message.getText()).thenReturn("/overdue");
        when(message.getChatId()).thenReturn(chatId);
        update.setMessage(message);

        telegramBotService.onUpdateReceived(update);

        ArgumentCaptor<SendMessage> captor = ArgumentCaptor.forClass(SendMessage.class);
        verify(telegramBotService).execute(captor.capture());
        assertThat(captor.getValue().getText()).contains("Muddati o'tgan vazifalar ro'yxati");
        assertThat(captor.getValue().getReplyMarkup()).isNotNull();
    }

    @Test
    void onUpdateReceived_tasksTodayCommand_listsTasks() throws Exception {
        Long chatId = 805L;
        User user = User.builder().id("u1").telegramChatId(chatId).build();
        when(userRepository.findByTelegramChatId(chatId)).thenReturn(Optional.of(user));

        Task t1 = Task.builder().id("t1").title("Today Task 1").build();
        when(taskRepository.findDueTodayTasksByUserId("u1", LocalDate.of(2026, 10, 1))).thenReturn(List.of(t1));

        Update update = new Update();
        Message message = mock(Message.class);
        Chat chat = new Chat();
        chat.setType("private");
        when(message.getChat()).thenReturn(chat);
        when(message.hasText()).thenReturn(true);
        when(message.getText()).thenReturn("/tasks_today");
        when(message.getChatId()).thenReturn(chatId);
        update.setMessage(message);

        telegramBotService.onUpdateReceived(update);

        ArgumentCaptor<SendMessage> captor = ArgumentCaptor.forClass(SendMessage.class);
        verify(telegramBotService).execute(captor.capture());
        assertThat(captor.getValue().getText()).contains("Bugun bajarilishi kerak bo'lgan vazifalar");
    }

    // --- CREATE_TASK Tests ---

    @Test
    void onUpdateReceived_createTask_quickMode_singleWorkspace_createsTask() throws Exception {
        Long chatId = 806L;
        User user = User.builder().id("u1").name("bek").telegramChatId(chatId).build();
        when(userRepository.findByTelegramChatId(chatId)).thenReturn(Optional.of(user));

        Workspace ws1 = Workspace.builder().id("ws1").title("Asosiy Loyiha").build();
        when(workspaceRepository.findByOwnerIdOrMemberUserId("u1")).thenReturn(List.of(ws1));
        when(workspaceRepository.findById("ws1")).thenReturn(Optional.of(ws1));
        when(authorizationService.checkCanEdit("ws1", user)).thenReturn(ws1);

        BoardColumn col1 = BoardColumn.builder().id("col1").workspaceId("ws1").title("To Do").order(1).isDone(false).build();
        when(columnRepository.findByWorkspaceIdOrderByOrderAsc("ws1")).thenReturn(List.of(col1));
        when(taskRepository.findMaxLexoRankByColumnId("col1")).thenReturn(null);

        when(taskRepository.save(any(Task.class))).thenAnswer(inv -> {
            Task t = inv.getArgument(0);
            t.setId("new-task-id");
            return t;
        });

        Update update = new Update();
        Message message = mock(Message.class);
        Chat chat = new Chat();
        chat.setType("private");
        when(message.getChat()).thenReturn(chat);
        when(message.hasText()).thenReturn(true);
        when(message.getText()).thenReturn("/create_task Tezkor vazifa");
        when(message.getChatId()).thenReturn(chatId);
        update.setMessage(message);

        telegramBotService.onUpdateReceived(update);

        ArgumentCaptor<Task> taskCaptor = ArgumentCaptor.forClass(Task.class);
        verify(taskRepository).save(taskCaptor.capture());
        Task createdTask = taskCaptor.getValue();
        assertThat(createdTask.getTitle()).isEqualTo("Tezkor vazifa");
        assertThat(createdTask.getColumnId()).isEqualTo("col1");
        assertThat(createdTask.getPriority()).isEqualTo(com.taskcenter.model.Priority.MEDIUM);
        assertThat(createdTask.getAssignees()).contains(user);

        verify(activityService).logActivity(eq("new-task-id"), eq(user), eq(TaskActivityType.TASK_CREATED), eq("task"), isNull(), eq("Tezkor vazifa"));
        verify(webSocketNotifier).notifyWorkspace(eq("ws1"), any());

        ArgumentCaptor<SendMessage> captor = ArgumentCaptor.forClass(SendMessage.class);
        verify(telegramBotService).execute(captor.capture());
        assertThat(captor.getValue().getText()).contains("Yangi vazifa muvaffaqiyatli yaratildi");
        assertThat(captor.getValue().getText()).contains("Tezkor vazifa");
    }

    @Test
    void onUpdateReceived_createTask_quickMode_multipleWorkspaces_promptsWorkspaceSelection() throws Exception {
        Long chatId = 807L;
        User user = User.builder().id("u1").telegramChatId(chatId).build();
        when(userRepository.findByTelegramChatId(chatId)).thenReturn(Optional.of(user));

        Workspace ws1 = Workspace.builder().id("ws1").title("Loyiha A").build();
        Workspace ws2 = Workspace.builder().id("ws2").title("Loyiha B").build();
        when(workspaceRepository.findByOwnerIdOrMemberUserId("u1")).thenReturn(List.of(ws1, ws2));

        Update update = new Update();
        Message message = mock(Message.class);
        Chat chat = new Chat();
        chat.setType("private");
        when(message.getChat()).thenReturn(chat);
        when(message.hasText()).thenReturn(true);
        when(message.getText()).thenReturn("/create_task Ko'p loyihali vazifa");
        when(message.getChatId()).thenReturn(chatId);
        update.setMessage(message);

        telegramBotService.onUpdateReceived(update);

        ArgumentCaptor<SendMessage> captor = ArgumentCaptor.forClass(SendMessage.class);
        verify(telegramBotService).execute(captor.capture());
        assertThat(captor.getValue().getText()).contains("qaysi g'alvaga qo'shamiz");
        assertThat(captor.getValue().getReplyMarkup()).isNotNull();
    }

    @Test
    void onUpdateReceived_createTask_wizardMode_stepByStep() throws Exception {
        Long chatId = 808L;
        User user = User.builder().id("u1").telegramChatId(chatId).build();
        when(userRepository.findByTelegramChatId(chatId)).thenReturn(Optional.of(user));

        Workspace ws1 = Workspace.builder().id("ws1").title("Loyiha 1").build();
        Workspace ws2 = Workspace.builder().id("ws2").title("Loyiha 2").build();
        when(workspaceRepository.findByOwnerIdOrMemberUserId("u1")).thenReturn(List.of(ws1, ws2));
        when(workspaceRepository.findById("ws1")).thenReturn(Optional.of(ws1));

        BoardColumn col1 = BoardColumn.builder().id("col1").workspaceId("ws1").title("To Do").order(1).isDone(false).build();
        when(columnRepository.findByWorkspaceIdOrderByOrderAsc("ws1")).thenReturn(List.of(col1));
        when(authorizationService.checkCanEdit("ws1", user)).thenReturn(ws1);
        when(taskRepository.save(any(Task.class))).thenAnswer(inv -> {
            Task t = inv.getArgument(0);
            t.setId("wizard-task-id");
            return t;
        });

        // Step 1: User sends /create_task
        Update u1 = new Update();
        Message m1 = mock(Message.class);
        Chat c1 = new Chat();
        c1.setType("private");
        when(m1.getChat()).thenReturn(c1);
        when(m1.hasText()).thenReturn(true);
        when(m1.getText()).thenReturn("/create_task");
        when(m1.getChatId()).thenReturn(chatId);
        u1.setMessage(m1);

        telegramBotService.onUpdateReceived(u1);

        // Step 2: User sends task title
        Update u2 = new Update();
        Message m2 = mock(Message.class);
        when(m2.getChat()).thenReturn(c1);
        when(m2.hasText()).thenReturn(true);
        when(m2.getText()).thenReturn("Interaktiv vazifa");
        when(m2.getChatId()).thenReturn(chatId);
        u2.setMessage(m2);

        telegramBotService.onUpdateReceived(u2);

        // Step 3: User picks workspace CT_WS_ws1
        Update u3 = new Update();
        CallbackQuery cb3 = new CallbackQuery();
        cb3.setId("cb-ws");
        cb3.setData("CT_WS_ws1");
        Message m3 = mock(Message.class);
        when(m3.getChat()).thenReturn(c1);
        when(m3.getChatId()).thenReturn(chatId);
        cb3.setMessage(m3);
        u3.setCallbackQuery(cb3);

        telegramBotService.onUpdateReceived(u3);

        // Step 4: User picks priority CT_PRIO_HIGH
        Update u4 = new Update();
        CallbackQuery cb4 = new CallbackQuery();
        cb4.setId("cb-prio");
        cb4.setData("CT_PRIO_HIGH");
        Message m4 = mock(Message.class);
        when(m4.getChat()).thenReturn(c1);
        when(m4.getChatId()).thenReturn(chatId);
        when(m4.getMessageId()).thenReturn(999);
        cb4.setMessage(m4);
        u4.setCallbackQuery(cb4);

        telegramBotService.onUpdateReceived(u4);

        ArgumentCaptor<Task> taskCaptor = ArgumentCaptor.forClass(Task.class);
        verify(taskRepository).save(taskCaptor.capture());
        Task saved = taskCaptor.getValue();
        assertThat(saved.getTitle()).isEqualTo("Interaktiv vazifa");
        assertThat(saved.getPriority()).isEqualTo(com.taskcenter.model.Priority.HIGH);
        assertThat(saved.getWorkspaceId()).isEqualTo("ws1");
    }

    @Test
    void onUpdateReceived_cancelCommand_cancelsActiveCreation() throws Exception {
        Long chatId = 809L;
        User user = User.builder().id("u1").telegramChatId(chatId).build();
        when(userRepository.findByTelegramChatId(chatId)).thenReturn(Optional.of(user));
        when(workspaceRepository.findByOwnerIdOrMemberUserId("u1")).thenReturn(List.of(Workspace.builder().id("w1").build()));

        // Start creation
        Update u1 = new Update();
        Message m1 = mock(Message.class);
        Chat c1 = new Chat();
        c1.setType("private");
        when(m1.getChat()).thenReturn(c1);
        when(m1.hasText()).thenReturn(true);
        when(m1.getText()).thenReturn("/create_task");
        when(m1.getChatId()).thenReturn(chatId);
        u1.setMessage(m1);
        telegramBotService.onUpdateReceived(u1);

        // Cancel
        Update u2 = new Update();
        Message m2 = mock(Message.class);
        when(m2.getChat()).thenReturn(c1);
        when(m2.hasText()).thenReturn(true);
        when(m2.getText()).thenReturn("/cancel");
        when(m2.getChatId()).thenReturn(chatId);
        u2.setMessage(m2);
        telegramBotService.onUpdateReceived(u2);

        ArgumentCaptor<SendMessage> captor = ArgumentCaptor.forClass(SendMessage.class);
        verify(telegramBotService, atLeastOnce()).execute(captor.capture());
        assertThat(captor.getValue().getText()).contains("Vazifa yaratish bekor qilindi");
    }

    @Test
    void onUpdateReceived_startTask_unlinked_showsHelp() throws Exception {
        Update update = new Update();
        Message message = mock(Message.class);
        Chat chat = new Chat();
        chat.setType("private");
        when(message.getChat()).thenReturn(chat);
        when(message.hasText()).thenReturn(true);
        when(message.getText()).thenReturn("/start task_123");
        when(message.getChatId()).thenReturn(801L);
        update.setMessage(message);

        when(userRepository.findByTelegramChatId(801L)).thenReturn(Optional.empty());

        telegramBotService.onUpdateReceived(update);

        ArgumentCaptor<SendMessage> captor = ArgumentCaptor.forClass(SendMessage.class);
        verify(telegramBotService).execute(captor.capture());
        assertThat(captor.getValue().getText()).contains("Telegram orqali bildirishnomalarni olish");
    }

    @Test
    void onUpdateReceived_startTask_noAccess_showsDenied() throws Exception {
        Update update = new Update();
        Message message = mock(Message.class);
        Chat chat = new Chat();
        chat.setType("private");
        when(message.getChat()).thenReturn(chat);
        when(message.hasText()).thenReturn(true);
        when(message.getText()).thenReturn("/start task_123");
        when(message.getChatId()).thenReturn(802L);
        update.setMessage(message);

        User user = User.builder().id("u1").build();
        when(userRepository.findByTelegramChatId(802L)).thenReturn(Optional.of(user));
        
        Task task = Task.builder().id("123").workspaceId("ws-1").title("Secret").build();
        when(taskRepository.findById("123")).thenReturn(Optional.of(task));

        doThrow(new RuntimeException("Denied")).when(authorizationService).checkAccess("ws-1", user);

        telegramBotService.onUpdateReceived(update);

        ArgumentCaptor<SendMessage> captor = ArgumentCaptor.forClass(SendMessage.class);
        verify(telegramBotService).execute(captor.capture());
        assertThat(captor.getValue().getText()).contains("Bu vazifaga ruxsatingiz yo'q");
    }

    @Test
    void onUpdateReceived_startTask_success_showsTask() throws Exception {
        Update update = new Update();
        Message message = mock(Message.class);
        Chat chat = new Chat();
        chat.setType("private");
        when(message.getChat()).thenReturn(chat);
        when(message.hasText()).thenReturn(true);
        when(message.getText()).thenReturn("/start task_123");
        when(message.getChatId()).thenReturn(803L);
        update.setMessage(message);

        User user = User.builder().id("u1").build();
        when(userRepository.findByTelegramChatId(803L)).thenReturn(Optional.of(user));
        
        Task task = Task.builder().id("123").workspaceId("ws-1").title("Secret Task").build();
        when(taskRepository.findById("123")).thenReturn(Optional.of(task));

        telegramBotService.onUpdateReceived(update);

        ArgumentCaptor<SendMessage> captor = ArgumentCaptor.forClass(SendMessage.class);
        verify(telegramBotService).execute(captor.capture());
        assertThat(captor.getValue().getText()).contains("Secret Task");
        assertThat(captor.getValue().getReplyMarkup()).isNotNull();
    }

    @Test
    void onUpdateReceived_myTasks_60Tasks_limitsTo80AndDoesNotFail() throws Exception {
        Update update = new Update();
        Message message = mock(Message.class);
        Chat chat = new Chat();
        chat.setType("private");
        when(message.getChat()).thenReturn(chat);
        when(message.hasText()).thenReturn(true);
        when(message.getText()).thenReturn("/my_tasks");
        when(message.getChatId()).thenReturn(901L);
        update.setMessage(message);

        User user = User.builder().id("u1").build();
        when(userRepository.findByTelegramChatId(901L)).thenReturn(Optional.of(user));

        List<Task> tasks = new java.util.ArrayList<>();
        for (int i = 0; i < 60; i++) {
            tasks.add(Task.builder().id("t" + i).title("Task " + i).build());
        }
        when(taskRepository.findActiveTasksByUserId(user.getId())).thenReturn(tasks);

        telegramBotService.onUpdateReceived(update);

        ArgumentCaptor<SendMessage> captor = ArgumentCaptor.forClass(SendMessage.class);
        verify(telegramBotService).execute(captor.capture());
        org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup markup = (org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup) captor.getValue().getReplyMarkup();
        assertThat(markup.getKeyboard()).hasSize(61); // 60 tasks + 1 web app button
    }

    @Test
    void onUpdateReceived_callback_viewTask_10000charDesc_truncatesProperly() throws Exception {
        Update update = new Update();
        CallbackQuery callbackQuery = new CallbackQuery();
        callbackQuery.setData("TASK_VIEW_taskLarge");
        Message message = mock(Message.class);
        Chat chat = new Chat();
        chat.setType("private");
        when(message.getChat()).thenReturn(chat);
        when(message.getChatId()).thenReturn(902L);
        callbackQuery.setMessage(message);
        update.setCallbackQuery(callbackQuery);

        User user = User.builder().id("u1").telegramChatId(902L).build();
        when(userRepository.findByTelegramChatId(902L)).thenReturn(Optional.of(user));

        String largeDesc = "A".repeat(10000);
        Task task = Task.builder()
                .id("taskLarge")
                .workspaceId("ws1")
                .title("T".repeat(300))
                .description(largeDesc)
                .priority(com.taskcenter.model.Priority.HIGH)
                .columnId("col1")
                .build();
        when(taskRepository.findById("taskLarge")).thenReturn(Optional.of(task));
        doReturn(new com.taskcenter.model.Workspace()).when(authorizationService).checkAccess("ws1", user);

        telegramBotService.onUpdateReceived(update);

        ArgumentCaptor<SendMessage> captor = ArgumentCaptor.forClass(SendMessage.class);
        verify(telegramBotService).execute(captor.capture());
        String text = captor.getValue().getText();
        assertThat(text.length()).isLessThanOrEqualTo(4000);
        assertThat(text).contains("...");
    }

    @Test
    void onUpdateReceived_callback_unknownFormat_callsAnswerCallbackQuery() throws Exception {
        Long chatId = 605L;

        User user = User.builder().id("u1").telegramChatId(chatId).build();
        when(userRepository.findByTelegramChatId(chatId)).thenReturn(Optional.of(user));

        Update update = new Update();
        CallbackQuery callbackQuery = new CallbackQuery();
        callbackQuery.setId("unknown123");
        callbackQuery.setData("UNKNOWN_FORMAT_CALLBACK");
        Message message = mock(Message.class);
        Chat chat = new Chat();
        chat.setType("private");
        when(message.getChat()).thenReturn(chat);
        when(message.getChatId()).thenReturn(chatId);
        callbackQuery.setMessage(message);
        update.setCallbackQuery(callbackQuery);

        telegramBotService.onUpdateReceived(update);

        ArgumentCaptor<AnswerCallbackQuery> answerCaptor = ArgumentCaptor.forClass(AnswerCallbackQuery.class);
        verify(telegramBotService, atLeastOnce()).execute(answerCaptor.capture());
    }

    @Test
    void onUpdateReceived_callback_taskDone_notAssigneeOrCreatorOrAdmin_rejected() throws Exception {
        Long chatId = 903L;
        String taskId = UUID.randomUUID().toString();
        String wsId = "ws-done";

        User user = User.builder().id("u1").telegramChatId(chatId).build();
        when(userRepository.findByTelegramChatId(chatId)).thenReturn(Optional.of(user));

        Task task = Task.builder()
                .id(taskId)
                .workspaceId(wsId)
                .columnId("col-1")
                .title("Test Task")
                .createdBy("otherUser")
                .build();
        task.getAssignees().add(User.builder().id("otherUser").build());
        when(taskRepository.findById(taskId)).thenReturn(Optional.of(task));

        Workspace ws = Workspace.builder().id(wsId).title("Test WS").ownerId("owner1").build();
        when(authorizationService.checkCanEdit(wsId, user)).thenReturn(ws);
        when(authorizationService.isOwnerOrAdmin(wsId, user.getId())).thenReturn(false);

        Update update = new Update();
        CallbackQuery callbackQuery = new CallbackQuery();
        callbackQuery.setId("cb-903");
        callbackQuery.setData("TASK_DONE_" + taskId);
        Message message = mock(Message.class);
        Chat chat = new Chat();
        chat.setType("private");
        when(message.getChat()).thenReturn(chat);
        when(message.getChatId()).thenReturn(chatId);
        callbackQuery.setMessage(message);
        update.setCallbackQuery(callbackQuery);

        telegramBotService.onUpdateReceived(update);

        ArgumentCaptor<AnswerCallbackQuery> answerCaptor = ArgumentCaptor.forClass(AnswerCallbackQuery.class);
        verify(telegramBotService, atLeastOnce()).execute(answerCaptor.capture());
        assertThat(answerCaptor.getAllValues().stream().map(AnswerCallbackQuery::getText).toList())
            .anyMatch(t -> t != null && t.contains("Bu vazifa sizga biriktirilmagan."));
    }

    @Test
    void onUpdateReceived_callback_taskDone_isAssignee_success() throws Exception {
        Long chatId = 904L;
        String taskId = UUID.randomUUID().toString();
        String wsId = "ws-done";

        User user = User.builder().id("u1").telegramChatId(chatId).build();
        when(userRepository.findByTelegramChatId(chatId)).thenReturn(Optional.of(user));

        Task task = Task.builder()
                .id(taskId)
                .workspaceId(wsId)
                .columnId("col-1")
                .title("Test Task")
                .createdBy("otherUser")
                .build();
        task.getAssignees().add(user);
        when(taskRepository.findById(taskId)).thenReturn(Optional.of(task));

        Workspace ws = Workspace.builder().id(wsId).title("Test WS").ownerId("owner1").build();
        when(authorizationService.checkCanEdit(wsId, user)).thenReturn(ws);
        when(authorizationService.isOwnerOrAdmin(wsId, user.getId())).thenReturn(false);

        BoardColumn col1 = BoardColumn.builder().id("col-1").workspaceId(wsId).title("To Do").order(1).isDone(false).build();
        BoardColumn doneCol = BoardColumn.builder().id("col-done").workspaceId(wsId).title("Done").order(2).isDone(true).build();
        when(columnRepository.findByWorkspaceIdOrderByOrderAsc(wsId)).thenReturn(List.of(col1, doneCol));
        when(columnRepository.findById("col-done")).thenReturn(Optional.of(doneCol));
        when(workspaceRepository.findById(wsId)).thenReturn(Optional.of(ws));

        Update update = new Update();
        CallbackQuery callbackQuery = new CallbackQuery();
        callbackQuery.setId("cb-904");
        callbackQuery.setData("TASK_DONE_" + taskId);
        Message message = mock(Message.class);
        Chat chat = new Chat();
        chat.setType("private");
        when(message.getChat()).thenReturn(chat);
        when(message.getChatId()).thenReturn(chatId);
        when(message.getMessageId()).thenReturn(100);
        callbackQuery.setMessage(message);
        update.setCallbackQuery(callbackQuery);

        telegramBotService.onUpdateReceived(update);

        verify(taskRepository).save(task);
        assertThat(task.getColumnId()).isEqualTo("col-done");
    }

    @Test
    void onUpdateReceived_callback_taskDone_isCreator_success() throws Exception {
        Long chatId = 905L;
        String taskId = UUID.randomUUID().toString();
        String wsId = "ws-done";

        User user = User.builder().id("u1").telegramChatId(chatId).build();
        when(userRepository.findByTelegramChatId(chatId)).thenReturn(Optional.of(user));

        Task task = Task.builder()
                .id(taskId)
                .workspaceId(wsId)
                .columnId("col-1")
                .title("Test Task")
                .createdBy("u1") // is creator
                .build();
        when(taskRepository.findById(taskId)).thenReturn(Optional.of(task));

        Workspace ws = Workspace.builder().id(wsId).title("Test WS").ownerId("owner1").build();
        when(authorizationService.checkCanEdit(wsId, user)).thenReturn(ws);
        when(authorizationService.isOwnerOrAdmin(wsId, user.getId())).thenReturn(false);

        BoardColumn col1 = BoardColumn.builder().id("col-1").workspaceId(wsId).title("To Do").order(1).isDone(false).build();
        BoardColumn doneCol = BoardColumn.builder().id("col-done").workspaceId(wsId).title("Done").order(2).isDone(true).build();
        when(columnRepository.findByWorkspaceIdOrderByOrderAsc(wsId)).thenReturn(List.of(col1, doneCol));
        when(columnRepository.findById("col-done")).thenReturn(Optional.of(doneCol));
        when(workspaceRepository.findById(wsId)).thenReturn(Optional.of(ws));

        Update update = new Update();
        CallbackQuery callbackQuery = new CallbackQuery();
        callbackQuery.setId("cb-905");
        callbackQuery.setData("TASK_DONE_" + taskId);
        Message message = mock(Message.class);
        Chat chat = new Chat();
        chat.setType("private");
        when(message.getChat()).thenReturn(chat);
        when(message.getChatId()).thenReturn(chatId);
        when(message.getMessageId()).thenReturn(100);
        callbackQuery.setMessage(message);
        update.setCallbackQuery(callbackQuery);

        telegramBotService.onUpdateReceived(update);

        verify(taskRepository).save(task);
        assertThat(task.getColumnId()).isEqualTo("col-done");
    }

    @Test
    void onUpdateReceived_callback_taskDone_isAdmin_success() throws Exception {
        Long chatId = 906L;
        String taskId = UUID.randomUUID().toString();
        String wsId = "ws-done";

        User user = User.builder().id("u1").telegramChatId(chatId).build();
        when(userRepository.findByTelegramChatId(chatId)).thenReturn(Optional.of(user));

        Task task = Task.builder()
                .id(taskId)
                .workspaceId(wsId)
                .columnId("col-1")
                .title("Test Task")
                .createdBy("otherUser")
                .build();
        when(taskRepository.findById(taskId)).thenReturn(Optional.of(task));

        Workspace ws = Workspace.builder().id(wsId).title("Test WS").ownerId("owner1").build();
        when(authorizationService.checkCanEdit(wsId, user)).thenReturn(ws);
        when(authorizationService.isOwnerOrAdmin(wsId, user.getId())).thenReturn(true); // is admin/owner

        BoardColumn col1 = BoardColumn.builder().id("col-1").workspaceId(wsId).title("To Do").order(1).isDone(false).build();
        BoardColumn doneCol = BoardColumn.builder().id("col-done").workspaceId(wsId).title("Done").order(2).isDone(true).build();
        when(columnRepository.findByWorkspaceIdOrderByOrderAsc(wsId)).thenReturn(List.of(col1, doneCol));
        when(columnRepository.findById("col-done")).thenReturn(Optional.of(doneCol));
        when(workspaceRepository.findById(wsId)).thenReturn(Optional.of(ws));

        Update update = new Update();
        CallbackQuery callbackQuery = new CallbackQuery();
        callbackQuery.setId("cb-906");
        callbackQuery.setData("TASK_DONE_" + taskId);
        Message message = mock(Message.class);
        Chat chat = new Chat();
        chat.setType("private");
        when(message.getChat()).thenReturn(chat);
        when(message.getChatId()).thenReturn(chatId);
        when(message.getMessageId()).thenReturn(100);
        callbackQuery.setMessage(message);
        update.setCallbackQuery(callbackQuery);

        telegramBotService.onUpdateReceived(update);

        verify(taskRepository).save(task);
        assertThat(task.getColumnId()).isEqualTo("col-done");
    }

    @Test
    void onUpdateReceived_start_withLinkToken_alreadyLinkedToDifferentChat_notifiesOldChat() throws Exception {
        Update update = new Update();
        Message message = mock(Message.class);
        Chat chat = new Chat();
        chat.setType("private");
        when(message.getChat()).thenReturn(chat);
        when(message.hasText()).thenReturn(true);
        when(message.getText()).thenReturn("/start VALID_TOKEN");
        when(message.getChatId()).thenReturn(1002L);
        update.setMessage(message);

        User user = User.builder()
                .id("u1")
                .telegramLinkToken("VALID_TOKEN")
                .telegramLinkTokenExpiresAt(LocalDateTime.now().plusMinutes(5))
                .telegramChatId(1001L) // old chat
                .build();

        when(userRepository.findByTelegramLinkToken("VALID_TOKEN")).thenReturn(Optional.of(user));
        when(userRepository.findByTelegramChatId(1002L)).thenReturn(Optional.empty());

        telegramBotService.onUpdateReceived(update);

        verify(userRepository).save(user);
        assertThat(user.getTelegramChatId()).isEqualTo(1002L);

        ArgumentCaptor<SendMessage> captor = ArgumentCaptor.forClass(SendMessage.class);
        verify(telegramBotService, atLeastOnce()).execute(captor.capture());
        
        boolean notifiedOldChat = captor.getAllValues().stream()
                .anyMatch(sm -> "1001".equals(sm.getChatId()) && sm.getText().contains("Akkauntingiz boshqa Telegram'ga ulandi."));
        assertThat(notifiedOldChat).isTrue();
    }

    @Test
    void onUpdateReceived_start_inviteLink_invitationNotFound() throws Exception {
        Update update = new Update();
        Message message = mock(Message.class);
        Chat chat = new Chat();
        chat.setType("private");
        when(message.getChat()).thenReturn(chat);
        when(message.hasText()).thenReturn(true);
        when(message.getText()).thenReturn("/start inv_not_found_123");
        when(message.getChatId()).thenReturn(999L);
        update.setMessage(message);

        when(invitationRepository.findById("not_found_123")).thenReturn(Optional.empty());

        telegramBotService.onUpdateReceived(update);

        ArgumentCaptor<SendMessage> captor = ArgumentCaptor.forClass(SendMessage.class);
        verify(telegramBotService).execute(captor.capture());
        assertThat(captor.getValue().getText()).contains("Taklifnoma topilmadi yoki bekor qilingan");
    }

    @Test
    void onUpdateReceived_start_inviteLink_showsInvitationDetailsAndButtons() throws Exception {
        Update update = new Update();
        Message message = mock(Message.class);
        Chat chat = new Chat();
        chat.setType("private");
        when(message.getChat()).thenReturn(chat);
        when(message.hasText()).thenReturn(true);
        when(message.getText()).thenReturn("/start inv_valid_invite");
        when(message.getChatId()).thenReturn(999L);
        update.setMessage(message);

        Workspace ws = Workspace.builder().id("ws1").title("Alpha Project").build();
        User inviter = User.builder().id("u_inv").name("John Doe").build();
        com.taskcenter.model.WorkspaceInvitation invitation = com.taskcenter.model.WorkspaceInvitation.builder()
                .id("valid_invite")
                .workspaceId("ws1")
                .senderId("u_inv")
                .role(com.taskcenter.model.WorkspaceRole.MEMBER)
                .status(com.taskcenter.model.InvitationStatus.PENDING)
                .build();

        when(invitationRepository.findById("valid_invite")).thenReturn(Optional.of(invitation));
        when(workspaceRepository.findById("ws1")).thenReturn(Optional.of(ws));
        when(userRepository.findById("u_inv")).thenReturn(Optional.of(inviter));

        telegramBotService.onUpdateReceived(update);

        ArgumentCaptor<SendMessage> captor = ArgumentCaptor.forClass(SendMessage.class);
        verify(telegramBotService).execute(captor.capture());
        SendMessage sent = captor.getValue();
        assertThat(sent.getText()).contains("Alpha Project");
        assertThat(sent.getText()).contains("John Doe");
        assertThat(sent.getReplyMarkup()).isNotNull();
    }

    @Test
    void onCallbackQuery_acceptInvitation_success() throws Exception {
        Update update = new Update();
        CallbackQuery callbackQuery = new CallbackQuery();
        callbackQuery.setData("ACCEPT_INV:inv_100");
        Message message = mock(Message.class);
        Chat chat = new Chat();
        chat.setType("private");
        when(message.getChat()).thenReturn(chat);
        when(message.getChatId()).thenReturn(777L);
        when(message.getMessageId()).thenReturn(50);
        callbackQuery.setMessage(message);
        update.setCallbackQuery(callbackQuery);

        User currentUser = User.builder()
                .id("u_invited")
                .email("user_123@taskcenter.local")
                .telegramChatId(777L)
                .build();
        Workspace ws = Workspace.builder().id("ws_test").title("Beta Team").build();
        com.taskcenter.model.WorkspaceInvitation invitation = com.taskcenter.model.WorkspaceInvitation.builder()
                .id("inv_100")
                .workspaceId("ws_test")
                .receiverEmail("real_email@example.com")
                .role(com.taskcenter.model.WorkspaceRole.MEMBER)
                .status(com.taskcenter.model.InvitationStatus.PENDING)
                .build();

        when(userRepository.findByTelegramChatId(777L)).thenReturn(Optional.of(currentUser));
        when(invitationRepository.findById("inv_100")).thenReturn(Optional.of(invitation));
        when(workspaceRepository.findById("ws_test")).thenReturn(Optional.of(ws));
        when(memberRepository.existsByWorkspaceIdAndUserId("ws_test", "u_invited")).thenReturn(false);

        telegramBotService.onUpdateReceived(update);

        assertThat(invitation.getStatus()).isEqualTo(com.taskcenter.model.InvitationStatus.ACCEPTED);
        assertThat(currentUser.getEmail()).isEqualTo("real_email@example.com");
        verify(memberRepository).save(any(com.taskcenter.model.WorkspaceMember.class));
        verify(invitationRepository).save(invitation);

        ArgumentCaptor<EditMessageText> editCaptor = ArgumentCaptor.forClass(EditMessageText.class);
        verify(telegramBotService).execute(editCaptor.capture());
        assertThat(editCaptor.getValue().getText()).contains("jamoasiga muvaffaqiyatli qo'shildingiz");
    }

    @Test
    void onCallbackQuery_rejectInvitation_success() throws Exception {
        Update update = new Update();
        CallbackQuery callbackQuery = new CallbackQuery();
        callbackQuery.setData("REJECT_INV:inv_200");
        Message message = mock(Message.class);
        Chat chat = new Chat();
        chat.setType("private");
        when(message.getChat()).thenReturn(chat);
        when(message.getChatId()).thenReturn(888L);
        when(message.getMessageId()).thenReturn(51);
        callbackQuery.setMessage(message);
        update.setCallbackQuery(callbackQuery);

        User currentUser = User.builder()
                .id("u_rejecting")
                .email("someone@example.com")
                .telegramChatId(888L)
                .build();
        com.taskcenter.model.WorkspaceInvitation invitation = com.taskcenter.model.WorkspaceInvitation.builder()
                .id("inv_200")
                .workspaceId("ws_reject")
                .receiverId("u_rejecting")
                .role(com.taskcenter.model.WorkspaceRole.MEMBER)
                .status(com.taskcenter.model.InvitationStatus.PENDING)
                .build();

        when(userRepository.findByTelegramChatId(888L)).thenReturn(Optional.of(currentUser));
        when(invitationRepository.findById("inv_200")).thenReturn(Optional.of(invitation));

        telegramBotService.onUpdateReceived(update);

        assertThat(invitation.getStatus()).isEqualTo(com.taskcenter.model.InvitationStatus.REJECTED);
        verify(invitationRepository).save(invitation);

        ArgumentCaptor<EditMessageText> editCaptor = ArgumentCaptor.forClass(EditMessageText.class);
        verify(telegramBotService).execute(editCaptor.capture());
        assertThat(editCaptor.getValue().getText()).contains("Taklifnoma rad etildi");
    }
}
