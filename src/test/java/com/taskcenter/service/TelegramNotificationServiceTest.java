package com.taskcenter.service;

import com.taskcenter.model.Priority;
import com.taskcenter.model.Task;
import com.taskcenter.model.User;
import com.taskcenter.model.Workspace;
import com.taskcenter.repository.TaskRepository;
import com.taskcenter.repository.UserRepository;
import com.taskcenter.repository.WorkspaceMemberRepository;
import com.taskcenter.repository.WorkspaceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TelegramNotificationServiceTest {

    @Mock
    private ApplicationEventPublisher eventPublisher;
    @Mock
    private TaskRepository taskRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private WorkspaceMemberRepository workspaceMemberRepository;
    @Mock
    private WorkspaceRepository workspaceRepository;

    private Clock fixedClock;
    private TelegramNotificationService service;

    // Soat 14:00 ga o'rnatilgan clock (kunduzi)
    private final ZoneId zone = ZoneId.of("Asia/Tashkent");

    @BeforeEach
    void setUp() {
        Instant fixedInstant = Instant.parse("2026-09-30T09:00:00Z"); // 14:00 Tashkent
        fixedClock = Clock.fixed(fixedInstant, zone);

        service = new TelegramNotificationService(
                eventPublisher,
                taskRepository,
                userRepository,
                workspaceMemberRepository,
                workspaceRepository,
                fixedClock
        );
    }

    @Test
    @DisplayName("Izoh: assignee va watcherlarga boradi, actorga bormaydi, TASK_VIEW tugmasi mavjud")
    void sendComment_sendsToAssigneeAndWatchers_notToActor() {
        User actor = User.builder().id("actor").name("otabek").telegramChatId(100L).build();
        User assignee = User.builder().id("u1").name("ali").telegramChatId(101L).telegramNotifyComments(true).build();
        User watcher = User.builder().id("u2").name("vali").telegramChatId(102L).telegramNotifyComments(true).build();

        Task task = Task.builder()
                .id("t1")
                .title("Katta task <muhim>")
                .workspaceId("ws-1")
                .assignees(Set.of(assignee, actor))
                .watchers(Set.of(watcher))
                .build();

        when(taskRepository.findByIdWithAssigneesAndWatchers("t1")).thenReturn(Optional.of(task));

        service.sendCommentAndMentionNotifications(task, actor, "Yangi izoh <salom> & test");

        ArgumentCaptor<TelegramMessageEvent> captor = ArgumentCaptor.forClass(TelegramMessageEvent.class);
        verify(eventPublisher, times(2)).publishEvent(captor.capture());

        List<TelegramMessageEvent> events = captor.getAllValues();
        assertThat(events).extracting(TelegramMessageEvent::chatId)
                .containsExactlyInAnyOrder(101L, 102L)
                .doesNotContain(100L); // Actor ga ketmaydi!

        // Xabar matni va HTML escape tekshiruvi
        for (TelegramMessageEvent event : events) {
            assertThat(event.message()).contains("otabek izoh yozdi: Yangi izoh &lt;salom&gt; &amp; test");
            assertThat(event.keyboard()).isNotNull();
            assertThat(event.keyboard().getKeyboard().get(0).get(0).getCallbackData()).isEqualTo("TASK_VIEW_t1");
        }
    }

    @Test
    @DisplayName("Izoh: sozlama o'chiq bo'lsa yoki chatId bo'lmasa bormaydi")
    void sendComment_doesNotSendWhenDisabledOrNoChatId() {
        User actor = User.builder().id("actor").name("otabek").build();
        User disabledUser = User.builder().id("u1").name("ali").telegramChatId(101L).telegramNotifyComments(false).build();
        User noChatIdUser = User.builder().id("u2").name("vali").telegramChatId(null).telegramNotifyComments(true).build();

        Task task = Task.builder()
                .id("t1")
                .title("Task")
                .workspaceId("ws-1")
                .assignees(Set.of(disabledUser))
                .watchers(Set.of(noChatIdUser))
                .build();

        when(taskRepository.findByIdWithAssigneesAndWatchers("t1")).thenReturn(Optional.of(task));

        service.sendCommentAndMentionNotifications(task, actor, "Izoh matni");

        verifyNoInteractions(eventPublisher);
    }

    @Test
    @DisplayName("Izoh: sokin soatda yuborilmaydi (keyinga qoldirilmaydi)")
    void sendComment_doesNotSendDuringQuietHours() {
        User actor = User.builder().id("actor").name("otabek").build();
        // Hozirgi soat 14:00 (fixedClock), sokin soat: 12:00 dan 16:00 gacha
        User quietUser = User.builder()
                .id("u1")
                .name("ali")
                .telegramChatId(101L)
                .telegramNotifyComments(true)
                .telegramQuietStart(LocalTime.of(12, 0))
                .telegramQuietEnd(LocalTime.of(16, 0))
                .build();

        Task task = Task.builder()
                .id("t1")
                .title("Task")
                .workspaceId("ws-1")
                .assignees(Set.of(quietUser))
                .build();

        when(taskRepository.findByIdWithAssigneesAndWatchers("t1")).thenReturn(Optional.of(task));

        service.sendCommentAndMentionNotifications(task, actor, "Izoh");

        verifyNoInteractions(eventPublisher);
    }

    @Test
    @DisplayName("@mention: workspace a'zosiga boradi, begona yoki o'ziga bormaydi")
    void sendMention_sendsOnlyToWorkspaceMembers() {
        User actor = User.builder().id("actor").name("otabek").telegramChatId(100L).build();
        User memberUser = User.builder().id("m1").name("ali").telegramChatId(101L).telegramNotifyComments(true).build();
        User strangerUser = User.builder().id("s1").name("stranger").telegramChatId(102L).telegramNotifyComments(true).build();

        Task task = Task.builder()
                .id("t1")
                .title("Task")
                .workspaceId("ws-1")
                .build();

        when(taskRepository.findByIdWithAssigneesAndWatchers("t1")).thenReturn(Optional.of(task));
        when(userRepository.findByNameIn(any())).thenReturn(List.of(memberUser, strangerUser));
        when(workspaceMemberRepository.existsByWorkspaceIdAndUserId("ws-1", "m1")).thenReturn(true);
        when(workspaceMemberRepository.existsByWorkspaceIdAndUserId("ws-1", "s1")).thenReturn(false);
        when(workspaceRepository.findById("ws-1")).thenReturn(Optional.empty());

        service.sendCommentAndMentionNotifications(task, actor, "Salom @ali va @stranger va @otabek!");

        ArgumentCaptor<TelegramMessageEvent> captor = ArgumentCaptor.forClass(TelegramMessageEvent.class);
        verify(eventPublisher, times(1)).publishEvent(captor.capture());

        TelegramMessageEvent event = captor.getValue();
        assertThat(event.chatId()).isEqualTo(101L);
        assertThat(event.message()).contains("otabek sizni eslab o'tdi: Salom @ali va @stranger va @otabek!");
        assertThat(event.keyboard()).isNotNull();
    }

    @Test
    @DisplayName("Deduplication: bir user ham mention, ham watcher bo'lsa faqat BITTA xabar boradi (mention ustuvor)")
    void sendComment_deduplication_mentionHasPriority() {
        User actor = User.builder().id("actor").name("otabek").build();
        User user1 = User.builder().id("u1").name("ali").telegramChatId(101L).telegramNotifyComments(true).build();

        Task task = Task.builder()
                .id("t1")
                .title("Task 1")
                .workspaceId("ws-1")
                .watchers(Set.of(user1))
                .assignees(Set.of(user1))
                .build();

        when(taskRepository.findByIdWithAssigneesAndWatchers("t1")).thenReturn(Optional.of(task));
        when(userRepository.findByNameIn(any())).thenReturn(List.of(user1));
        when(workspaceMemberRepository.existsByWorkspaceIdAndUserId("ws-1", "u1")).thenReturn(true);

        service.sendCommentAndMentionNotifications(task, actor, "Salom @ali, buni tekshirib ko'r");

        ArgumentCaptor<TelegramMessageEvent> captor = ArgumentCaptor.forClass(TelegramMessageEvent.class);
        verify(eventPublisher, times(1)).publishEvent(captor.capture());

        TelegramMessageEvent event = captor.getValue();
        assertThat(event.chatId()).isEqualTo(101L);
        // Mention xabari ketishi kerak (izoh xabari emas)
        assertThat(event.message()).contains("sizni eslab o'tdi");
        assertThat(event.message()).doesNotContain("izoh yozdi");
    }

    @Test
    @DisplayName("Ustunga o'tish: watcher va assigneelarga boradi, actorga bormaydi, HTML escape va TASK_VIEW mavjud")
    void sendTaskMoved_sendsToWatchersAndAssignees() {
        User actor = User.builder().id("actor").name("otabek").telegramChatId(100L).build();
        User watcher = User.builder().id("u1").name("ali").telegramChatId(101L).telegramNotifyMoves(true).build();
        User disabledUser = User.builder().id("u2").name("vali").telegramChatId(102L).telegramNotifyMoves(false).build();

        Task task = Task.builder()
                .id("t1")
                .title("Integratsiya <API>")
                .workspaceId("ws-1")
                .watchers(Set.of(watcher, disabledUser, actor))
                .build();

        when(taskRepository.findByIdWithAssigneesAndWatchers("t1")).thenReturn(Optional.of(task));

        service.sendTaskMovedNotification(task, actor, "To Do <eski>", "In Progress <yangi>");

        ArgumentCaptor<TelegramMessageEvent> captor = ArgumentCaptor.forClass(TelegramMessageEvent.class);
        verify(eventPublisher, times(1)).publishEvent(captor.capture());

        TelegramMessageEvent event = captor.getValue();
        assertThat(event.chatId()).isEqualTo(101L);
        assertThat(event.message()).isEqualTo("Task 'Integratsiya &lt;API&gt;' To Do &lt;eski&gt; dan In Progress &lt;yangi&gt; ga o'tdi");
        assertThat(event.keyboard()).isNotNull();
        assertThat(event.keyboard().getKeyboard().get(0).get(0).getCallbackData()).isEqualTo("TASK_VIEW_t1");
    }

    @Test
    @DisplayName("Taklif: taklif olingan userga boradi, TASK_VIEW tugmasi BO'LMAYDI, sozlama tekshiriladi")
    void sendWorkspaceInvite_sendsWithoutTaskViewButton() {
        User actor = User.builder().id("actor").name("otabek").build();
        User receiver = User.builder()
                .id("r1")
                .name("ali")
                .telegramChatId(201L)
                .telegramNotifyInvites(true)
                .build();

        Workspace ws = Workspace.builder().id("ws-1").title("Super Loyiha <AI>").build();

        service.sendWorkspaceInviteNotification(ws, receiver, actor);

        ArgumentCaptor<TelegramMessageEvent> captor = ArgumentCaptor.forClass(TelegramMessageEvent.class);
        verify(eventPublisher, times(1)).publishEvent(captor.capture());

        TelegramMessageEvent event = captor.getValue();
        assertThat(event.chatId()).isEqualTo(201L);
        assertThat(event.message()).isEqualTo("Sizni 'Super Loyiha &lt;AI&gt;' ga taklif qilishdi");
        assertThat(event.keyboard()).isNull(); // TASK_VIEW tugmasi BO'LMASIN!
    }

    @Test
    @DisplayName("Taklif: telegramNotifyInvites o'chiq bo'lsa yuborilmaydi")
    void sendWorkspaceInvite_doesNotSendWhenDisabled() {
        User actor = User.builder().id("actor").name("otabek").build();
        User receiver = User.builder()
                .id("r1")
                .name("ali")
                .telegramChatId(201L)
                .telegramNotifyInvites(false)
                .build();

        Workspace ws = Workspace.builder().id("ws-1").title("Super Loyiha").build();

        service.sendWorkspaceInviteNotification(ws, receiver, actor);

        verifyNoInteractions(eventPublisher);
    }

    @Test
    @DisplayName("Task tayinlandi: boshqa user tayinlaganda va sozlama ochiq bo'lsa yuboriladi")
    void sendTaskAssigned_sendsWhenEnabledAndNotSelf() {
        User actor = User.builder().id("actor").name("otabek").build();
        User assignee = User.builder()
                .id("u1")
                .name("ali")
                .telegramChatId(101L)
                .telegramNotifyAssigned(true)
                .build();

        Task task = Task.builder()
                .id("t1")
                .title("Muhim vazifa <AI>")
                .priority(Priority.HIGH)
                .build();

        service.sendTaskAssignedNotification(task, assignee, actor);

        ArgumentCaptor<TelegramMessageEvent> captor = ArgumentCaptor.forClass(TelegramMessageEvent.class);
        verify(eventPublisher, times(1)).publishEvent(captor.capture());

        TelegramMessageEvent event = captor.getValue();
        assertThat(event.chatId()).isEqualTo(101L);
        assertThat(event.message()).contains("Muhim vazifa &lt;AI&gt;");
        assertThat(event.message()).contains("HIGH");
        assertThat(event.keyboard()).isNotNull();
        assertThat(event.keyboard().getKeyboard().get(0).get(0).getCallbackData()).isEqualTo("TASK_VIEW_t1");
    }

    @Test
    @DisplayName("Task tayinlandi: telegramNotifyAssigned o'chiq bo'lsa yuborilmaydi")
    void sendTaskAssigned_doesNotSendWhenDisabled() {
        User actor = User.builder().id("actor").name("otabek").build();
        User assignee = User.builder()
                .id("u1")
                .name("ali")
                .telegramChatId(101L)
                .telegramNotifyAssigned(false)
                .build();

        Task task = Task.builder()
                .id("t1")
                .title("Muhim vazifa")
                .priority(Priority.HIGH)
                .build();

        service.sendTaskAssignedNotification(task, assignee, actor);

        verifyNoInteractions(eventPublisher);
    }

    @Test
    @DisplayName("Task tayinlandi: actor o'zini o'zi tayinlaganda yuborilmaydi")
    void sendTaskAssigned_doesNotSendWhenSelfAssigned() {
        User actor = User.builder()
                .id("actor")
                .name("otabek")
                .telegramChatId(100L)
                .telegramNotifyAssigned(true)
                .build();

        Task task = Task.builder()
                .id("t1")
                .title("O'z vazifam")
                .priority(Priority.MEDIUM)
                .build();

        // Actor o'zini o'zi tayinlagan
        service.sendTaskAssignedNotification(task, actor, actor);

        verifyNoInteractions(eventPublisher);
    }

    @Test
    @DisplayName("Task tayinlandi: sokin soatda yuborilmaydi")
    void sendTaskAssigned_doesNotSendDuringQuietHours() {
        User actor = User.builder().id("actor").name("otabek").build();
        // Hozirgi soat 14:00 (fixedClock), sokin soat: 13:00 dan 15:00 gacha
        User quietUser = User.builder()
                .id("u1")
                .name("ali")
                .telegramChatId(101L)
                .telegramNotifyAssigned(true)
                .telegramQuietStart(LocalTime.of(13, 0))
                .telegramQuietEnd(LocalTime.of(15, 0))
                .build();

        Task task = Task.builder()
                .id("t1")
                .title("Sokin soatdagi vazifa")
                .priority(Priority.LOW)
                .build();

        service.sendTaskAssignedNotification(task, quietUser, actor);

        verifyNoInteractions(eventPublisher);
    }
}
