package com.taskcenter.service;

import com.taskcenter.dto.TelegramReminderTaskDto;
import com.taskcenter.model.TelegramReminderLog;
import com.taskcenter.model.User;
import com.taskcenter.model.Workspace;
import com.taskcenter.repository.TaskRepository;
import com.taskcenter.repository.TelegramReminderLogRepository;
import com.taskcenter.repository.UserRepository;
import com.taskcenter.repository.WorkspaceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TelegramSchedulerServiceTest {

    @Mock private TaskRepository taskRepository;
    @Mock private TelegramReminderLogRepository reminderLogRepository;
    @Mock private UserRepository userRepository;
    @Mock private WorkspaceRepository workspaceRepository;
    @Mock private ApplicationEventPublisher eventPublisher;

    private TelegramSchedulerService service;

    @BeforeEach
    void setUp() {
        service = new TelegramSchedulerService(
                taskRepository, reminderLogRepository,
                userRepository, workspaceRepository,
                eventPublisher, Clock.systemDefaultZone(), "https://test.url", true);
    }

    @Test
    void whenDeadlineDisabled_doesNotSendRemindersOrDigest() {
        TelegramSchedulerService disabledService = new TelegramSchedulerService(
                taskRepository, reminderLogRepository,
                userRepository, workspaceRepository,
                eventPublisher, Clock.systemDefaultZone(), "https://test.url", false);

        disabledService.sendDueTomorrowReminders(LocalDateTime.now());
        disabledService.sendDailyDigest(LocalDateTime.now());

        verifyNoInteractions(taskRepository);
    }

    // ═══════════════════ DUE_TOMORROW eslatma testlari ═══════════════════

    @Test
    void dueTomorrow_at2359_tomorrowIsNextDay() {
        // 23:59 Toshkent vaqtida "ertaga" keyingi kun (okt 1) bo'lishi kerak
        LocalDateTime now = LocalDateTime.of(2026, 9, 30, 23, 59);
        LocalDate expectedTomorrow = LocalDate.of(2026, 10, 1);

        TelegramReminderTaskDto task = taskDto("t1", "Task 1", "ws1", expectedTomorrow, "u1", 111L);
        when(taskRepository.findTasksWithAssigneesForTelegramReminder(expectedTomorrow, expectedTomorrow))
                .thenReturn(List.of(task));

        User user = User.builder().id("u1").telegramChatId(111L).build();
        when(userRepository.findByIdIn(Set.of("u1"))).thenReturn(List.of(user));
        when(reminderLogRepository.findByUserIdAndTypeAndTaskIdIn("u1", 
                TelegramReminderLog.ReminderType.DUE_TOMORROW, List.of("t1"))).thenReturn(List.of());

        service.sendDueTomorrowReminders(now);

        verify(taskRepository).findTasksWithAssigneesForTelegramReminder(expectedTomorrow, expectedTomorrow);
        ArgumentCaptor<TelegramMessageEvent> captor = ArgumentCaptor.forClass(TelegramMessageEvent.class);
        verify(eventPublisher).publishEvent(captor.capture());
        assertThat(captor.getValue().chatId()).isEqualTo(111L);
        assertThat(captor.getValue().message()).contains("Ertaga muddati");
    }

    @Test
    void dueTomorrow_at0001_tomorrowIsCorrectDay() {
        // 00:01 da "ertaga" 2-oktabr bo'lishi kerak
        LocalDateTime now = LocalDateTime.of(2026, 10, 1, 0, 1);
        LocalDate expectedTomorrow = LocalDate.of(2026, 10, 2);

        when(taskRepository.findTasksWithAssigneesForTelegramReminder(expectedTomorrow, expectedTomorrow))
                .thenReturn(List.of());

        service.sendDueTomorrowReminders(now);

        verify(taskRepository).findTasksWithAssigneesForTelegramReminder(expectedTomorrow, expectedTomorrow);
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    void dueTomorrow_duplicateRun_sendsOnlyOnce() {
        LocalDateTime now = LocalDateTime.of(2026, 9, 30, 18, 0);
        LocalDate tomorrow = LocalDate.of(2026, 10, 1);

        TelegramReminderTaskDto task = taskDto("t1", "Task 1", "ws1", tomorrow, "u1", 111L);
        when(taskRepository.findTasksWithAssigneesForTelegramReminder(tomorrow, tomorrow))
                .thenReturn(List.of(task));

        User user = User.builder().id("u1").telegramChatId(111L).build();
        when(userRepository.findByIdIn(Set.of("u1"))).thenReturn(List.of(user));

        // Birinchi ishlatishda mavjud emas, ikkinchisida bor
        when(reminderLogRepository.findByUserIdAndTypeAndTaskIdIn("u1", TelegramReminderLog.ReminderType.DUE_TOMORROW, List.of("t1")))
                .thenReturn(List.of())
                .thenReturn(List.of(TelegramReminderLog.builder().taskId("t1").build()));

        service.sendDueTomorrowReminders(now);
        service.sendDueTomorrowReminders(now);

        // Faqat 1 marta yuboriladi
        verify(eventPublisher, times(1)).publishEvent(any(TelegramMessageEvent.class));
        verify(reminderLogRepository, times(1)).saveAll(anyList());
    }

    @Test
    void dueTomorrow_quietHoursOvernight_doesNotBlock18() {
        // 22:00-08:00 sokin soat 18:00 da bloklamaydi
        LocalDateTime now = LocalDateTime.of(2026, 9, 30, 18, 0);
        LocalDate tomorrow = LocalDate.of(2026, 10, 1);

        TelegramReminderTaskDto task = taskDto("t1", "Task 1", "ws1", tomorrow, "u1", 111L);
        when(taskRepository.findTasksWithAssigneesForTelegramReminder(tomorrow, tomorrow))
                .thenReturn(List.of(task));

        User user = User.builder().id("u1").telegramChatId(111L)
                .telegramQuietStart(LocalTime.of(22, 0))
                .telegramQuietEnd(LocalTime.of(8, 0))
                .build();
        when(userRepository.findByIdIn(Set.of("u1"))).thenReturn(List.of(user));
        when(reminderLogRepository.findByUserIdAndTypeAndTaskIdIn(eq("u1"), any(), any())).thenReturn(List.of());

        service.sendDueTomorrowReminders(now);

        verify(eventPublisher).publishEvent(any(TelegramMessageEvent.class));
    }

    @Test
    void dueTomorrow_quietHoursBlocking_noSend() {
        // 17:00-19:00 sokin soat 18:00 da bloklaydi
        LocalDateTime now = LocalDateTime.of(2026, 9, 30, 18, 0);
        LocalDate tomorrow = LocalDate.of(2026, 10, 1);

        TelegramReminderTaskDto task = taskDto("t1", "Task 1", "ws1", tomorrow, "u1", 111L);
        when(taskRepository.findTasksWithAssigneesForTelegramReminder(tomorrow, tomorrow))
                .thenReturn(List.of(task));

        User user = User.builder().id("u1").telegramChatId(111L)
                .telegramQuietStart(LocalTime.of(17, 0))
                .telegramQuietEnd(LocalTime.of(19, 0))
                .build();
        when(userRepository.findByIdIn(Set.of("u1"))).thenReturn(List.of(user));

        service.sendDueTomorrowReminders(now);

        verify(eventPublisher, never()).publishEvent(any());
        verify(reminderLogRepository, never()).save(any());
    }

    @Test
    void dueTomorrow_deletedDoneDisabledTasks_queryReturnsEmpty_noSend() {
        // O'chirilgan task, bajarilgan (is_done), sozlamasi o'chiq user, chatId yo'q
        // Bular so'rov tomonidan filtrlanadi — bo'sh ro'yxat qaytadi
        LocalDateTime now = LocalDateTime.of(2026, 9, 30, 18, 0);
        LocalDate tomorrow = LocalDate.of(2026, 10, 1);

        when(taskRepository.findTasksWithAssigneesForTelegramReminder(tomorrow, tomorrow))
                .thenReturn(List.of());

        service.sendDueTomorrowReminders(now);

        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    void dueTomorrow_noChatId_skips() {
        LocalDateTime now = LocalDateTime.of(2026, 9, 30, 18, 0);
        LocalDate tomorrow = LocalDate.of(2026, 10, 1);

        TelegramReminderTaskDto task = taskDto("t1", "Task 1", "ws1", tomorrow, "u1", null);
        when(taskRepository.findTasksWithAssigneesForTelegramReminder(tomorrow, tomorrow))
                .thenReturn(List.of(task));

        User user = User.builder().id("u1").telegramChatId(null).build();
        when(userRepository.findByIdIn(Set.of("u1"))).thenReturn(List.of(user));

        service.sendDueTomorrowReminders(now);

        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    void dueTomorrow_oneUserFails_otherStillReceives() {
        LocalDateTime now = LocalDateTime.of(2026, 9, 30, 18, 0);
        LocalDate tomorrow = LocalDate.of(2026, 10, 1);

        TelegramReminderTaskDto task1 = taskDto("t1", "Task 1", "ws1", tomorrow, "u1", 111L);
        TelegramReminderTaskDto task2 = taskDto("t2", "Task 2", "ws1", tomorrow, "u2", 222L);
        when(taskRepository.findTasksWithAssigneesForTelegramReminder(tomorrow, tomorrow))
                .thenReturn(List.of(task1, task2));

        User user1 = User.builder().id("u1").telegramChatId(111L).build();
        User user2 = User.builder().id("u2").telegramChatId(222L).build();
        when(userRepository.findByIdIn(anySet())).thenReturn(List.of(user1, user2));

        // u1 uchun xato chiqaradi — outer catch ga tushadi
        when(reminderLogRepository.findByUserIdAndTypeAndTaskIdIn("u1", TelegramReminderLog.ReminderType.DUE_TOMORROW, List.of("t1")))
                .thenThrow(new RuntimeException("DB connection lost"));
        // u2 uchun muvaffaqiyat
        when(reminderLogRepository.findByUserIdAndTypeAndTaskIdIn("u2", TelegramReminderLog.ReminderType.DUE_TOMORROW, List.of("t2")))
                .thenReturn(List.of());

        service.sendDueTomorrowReminders(now);

        // u1 xato bo'lsa ham u2 ga xabar borishi kerak
        ArgumentCaptor<TelegramMessageEvent> captor = ArgumentCaptor.forClass(TelegramMessageEvent.class);
        verify(eventPublisher, atLeastOnce()).publishEvent(captor.capture());
        assertThat(captor.getAllValues()).extracting(TelegramMessageEvent::chatId).contains(222L);
    }

    // ═══════════════════ Digest testlari ═══════════════════

    @Test
    void digest_at0001_todayIsCorrect() {
        LocalDateTime now = LocalDateTime.of(2026, 10, 1, 0, 1);
        LocalDate expectedToday = LocalDate.of(2026, 10, 1);

        when(taskRepository.findTasksForTelegramDigest(expectedToday)).thenReturn(List.of());

        service.sendDailyDigest(now);

        verify(taskRepository).findTasksForTelegramDigest(expectedToday);
    }

    @Test
    void digest_at2359_todayIsCorrect() {
        LocalDateTime now = LocalDateTime.of(2026, 9, 30, 23, 59);
        LocalDate expectedToday = LocalDate.of(2026, 9, 30);

        when(taskRepository.findTasksForTelegramDigest(expectedToday)).thenReturn(List.of());

        service.sendDailyDigest(now);

        verify(taskRepository).findTasksForTelegramDigest(expectedToday);
    }

    @Test
    void digest_todayAndOverdueTasks_sendsCorrectMessage() {
        LocalDateTime now = LocalDateTime.of(2026, 9, 30, 8, 0);
        LocalDate today = LocalDate.of(2026, 9, 30);

        TelegramReminderTaskDto todayTask = taskDto("t1", "Bugungi vazifa", "ws1", today, "u1", 111L);
        TelegramReminderTaskDto overdueTask = taskDto("t2", "Kechikkan vazifa", "ws1",
                LocalDate.of(2026, 9, 28), "u1", 111L);

        when(taskRepository.findTasksForTelegramDigest(today))
                .thenReturn(List.of(todayTask, overdueTask));

        User user = User.builder().id("u1").telegramChatId(111L).build();
        when(userRepository.findByIdIn(Set.of("u1"))).thenReturn(List.of(user));

        Workspace ws = Workspace.builder().id("ws1").title("Loyiham").build();
        when(workspaceRepository.findAllById(Set.of("ws1"))).thenReturn(List.of(ws));

        service.sendDailyDigest(now);

        ArgumentCaptor<TelegramMessageEvent> captor = ArgumentCaptor.forClass(TelegramMessageEvent.class);
        verify(eventPublisher).publishEvent(captor.capture());

        TelegramMessageEvent event = captor.getValue();
        assertThat(event.chatId()).isEqualTo(111L);
        assertThat(event.message()).contains("Kunlik hisobot");
        assertThat(event.message()).contains("Bugun muddati: 1 ta");
        assertThat(event.message()).contains("Muddati o'tgan: 1 ta");
        assertThat(event.message()).contains("Bugungi vazifa");
        assertThat(event.message()).contains("Kechikkan vazifa");
        assertThat(event.message()).contains("Loyiham");

        // Inline tugmalar
        assertThat(event.keyboard()).isNotNull();
        assertThat(event.keyboard().getKeyboard()).hasSize(4);
    }

    @Test
    void digest_quietHoursBlocking_noSend() {
        LocalDateTime now = LocalDateTime.of(2026, 9, 30, 8, 0);
        LocalDate today = LocalDate.of(2026, 9, 30);

        TelegramReminderTaskDto task = taskDto("t1", "Task", "ws1", today, "u1", 111L);
        when(taskRepository.findTasksForTelegramDigest(today)).thenReturn(List.of(task));

        User user = User.builder().id("u1").telegramChatId(111L)
                .telegramQuietStart(LocalTime.of(7, 0))
                .telegramQuietEnd(LocalTime.of(9, 0))
                .build();
        when(userRepository.findByIdIn(Set.of("u1"))).thenReturn(List.of(user));

        service.sendDailyDigest(now);

        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    void digest_emptyTaskList_noSend() {
        LocalDateTime now = LocalDateTime.of(2026, 9, 30, 8, 0);
        when(taskRepository.findTasksForTelegramDigest(any())).thenReturn(List.of());

        service.sendDailyDigest(now);

        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    void digest_longList_truncatesTo4096AndMaxButtons() {
        LocalDateTime now = LocalDateTime.of(2026, 9, 30, 8, 0);
        LocalDate today = LocalDate.of(2026, 9, 30);

        // 100 ta uzun nomli task
        List<TelegramReminderTaskDto> tasks = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            tasks.add(taskDto("t" + i,
                    "Bu juda uzun vazifa nomi bolip kop joy egallaydi test task raqami #" + i,
                    "ws1", today, "u1", 111L));
        }
        when(taskRepository.findTasksForTelegramDigest(today)).thenReturn(tasks);

        User user = User.builder().id("u1").telegramChatId(111L).build();
        when(userRepository.findByIdIn(Set.of("u1"))).thenReturn(List.of(user));

        Workspace ws = Workspace.builder().id("ws1").title("Loyiha").build();
        when(workspaceRepository.findAllById(Set.of("ws1"))).thenReturn(List.of(ws));

        service.sendDailyDigest(now);

        ArgumentCaptor<TelegramMessageEvent> captor = ArgumentCaptor.forClass(TelegramMessageEvent.class);
        verify(eventPublisher).publishEvent(captor.capture());

        TelegramMessageEvent event = captor.getValue();
        assertThat(event.message().length()).isLessThanOrEqualTo(4096);
        assertThat(event.message()).contains("va yana");
        // Max 10 inline tugma
        assertThat(event.keyboard().getKeyboard()).hasSize(20);
    }

    @Test
    void digest_noChatId_skips() {
        LocalDateTime now = LocalDateTime.of(2026, 9, 30, 8, 0);
        LocalDate today = LocalDate.of(2026, 9, 30);

        TelegramReminderTaskDto task = taskDto("t1", "Task", "ws1", today, "u1", null);
        when(taskRepository.findTasksForTelegramDigest(today)).thenReturn(List.of(task));

        User user = User.builder().id("u1").telegramChatId(null).build();
        when(userRepository.findByIdIn(Set.of("u1"))).thenReturn(List.of(user));

        service.sendDailyDigest(now);

        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    void digest_settingDisabledUser_queryFilters() {
        // telegramDailyDigest = false userlar so'rov tomonidan filtrlanadi
        LocalDateTime now = LocalDateTime.of(2026, 9, 30, 8, 0);
        when(taskRepository.findTasksForTelegramDigest(any())).thenReturn(List.of());

        service.sendDailyDigest(now);

        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    void digest_at0800_whenQuietEndIs0800_digestIsSent() {
        // quiet_start=22:00, quiet_end=08:00 bo'lganda, aynan 08:00 da sokin soat tugagan
        // (end exclusive, [start, end) oraliq). Shuning uchun 08:00 dagi digest yuborilishi kerak.
        LocalDateTime now = LocalDateTime.of(2026, 9, 30, 8, 0);
        LocalDate today = LocalDate.of(2026, 9, 30);

        TelegramReminderTaskDto task = taskDto("t1", "Task 1", "ws1", today, "u1", 111L);
        when(taskRepository.findTasksForTelegramDigest(today)).thenReturn(List.of(task));

        User user = User.builder()
                .id("u1")
                .telegramChatId(111L)
                .telegramQuietStart(LocalTime.of(22, 0))
                .telegramQuietEnd(LocalTime.of(8, 0))
                .build();
        when(userRepository.findByIdIn(Set.of("u1"))).thenReturn(List.of(user));
        when(workspaceRepository.findAllById(Set.of("ws1"))).thenReturn(List.of());

        service.sendDailyDigest(now);

        ArgumentCaptor<TelegramMessageEvent> captor = ArgumentCaptor.forClass(TelegramMessageEvent.class);
        verify(eventPublisher).publishEvent(captor.capture());
        assertThat(captor.getValue().chatId()).isEqualTo(111L);
        assertThat(captor.getValue().message()).contains("Kunlik hisobot");
    }

    // ═══════════════════ Quiet hours testlari ═══════════════════

    @Test
    void isInQuietHours_overnightRange_22_to_08() {
        LocalTime start = LocalTime.of(22, 0);
        LocalTime end = LocalTime.of(8, 0);

        // 18:00 — sokin emas
        assertThat(TelegramSchedulerService.isInQuietHours(LocalTime.of(18, 0), start, end)).isFalse();
        // 23:00 — sokin
        assertThat(TelegramSchedulerService.isInQuietHours(LocalTime.of(23, 0), start, end)).isTrue();
        // 07:59 — sokin
        assertThat(TelegramSchedulerService.isInQuietHours(LocalTime.of(7, 59), start, end)).isTrue();
        // 08:00 — sokin emas (chegara, end exclusive)
        assertThat(TelegramSchedulerService.isInQuietHours(LocalTime.of(8, 0), start, end)).isFalse();
        // 22:00 — sokin (start inclusive)
        assertThat(TelegramSchedulerService.isInQuietHours(LocalTime.of(22, 0), start, end)).isTrue();
        // 00:00 — sokin
        assertThat(TelegramSchedulerService.isInQuietHours(LocalTime.of(0, 0), start, end)).isTrue();
    }

    @Test
    void isInQuietHours_sameDayRange_08_to_22() {
        LocalTime start = LocalTime.of(8, 0);
        LocalTime end = LocalTime.of(22, 0);

        assertThat(TelegramSchedulerService.isInQuietHours(LocalTime.of(12, 0), start, end)).isTrue();
        assertThat(TelegramSchedulerService.isInQuietHours(LocalTime.of(7, 59), start, end)).isFalse();
        assertThat(TelegramSchedulerService.isInQuietHours(LocalTime.of(22, 0), start, end)).isFalse();
        assertThat(TelegramSchedulerService.isInQuietHours(LocalTime.of(8, 0), start, end)).isTrue();
    }

    @Test
    void isInQuietHours_nullValues_noRestriction() {
        assertThat(TelegramSchedulerService.isInQuietHours(LocalTime.of(12, 0), null, null)).isFalse();
        assertThat(TelegramSchedulerService.isInQuietHours(LocalTime.of(12, 0), LocalTime.of(8, 0), null)).isFalse();
        assertThat(TelegramSchedulerService.isInQuietHours(LocalTime.of(12, 0), null, LocalTime.of(22, 0))).isFalse();
    }

    // ═══════════════════ Yordamchi metodlar ═══════════════════

    private TelegramReminderTaskDto taskDto(String taskId, String title, String wsId,
                                             LocalDate dueDate, String userId, Long chatId) {
        return new TelegramReminderTaskDto(taskId, title, wsId, dueDate, userId, chatId);
    }
}
