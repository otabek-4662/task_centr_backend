package com.taskcenter.repository;

import com.taskcenter.dto.TelegramReminderTaskDto;
import com.taskcenter.model.BoardColumn;
import com.taskcenter.model.Task;
import com.taskcenter.model.TelegramReminderLog;
import com.taskcenter.model.User;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@ActiveProfiles("test")
class TelegramReminderRepositoryTest {

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ColumnRepository columnRepository;

    @Autowired
    private TelegramReminderLogRepository reminderLogRepository;

    @Autowired
    private EntityManager em;

    private User validUser;
    private User noChatIdUser;
    private User disabledNotifyUser;
    private BoardColumn todoCol;
    private BoardColumn doneCol;

    @BeforeEach
    void setUp() {
        try {
            em.createNativeQuery("DELETE FROM telegram_reminder_log").executeUpdate();
            em.createNativeQuery("DELETE FROM task_assignees").executeUpdate();
            em.createNativeQuery("DELETE FROM tasks").executeUpdate();
            em.createNativeQuery("DELETE FROM board_columns").executeUpdate();
            em.createNativeQuery("DELETE FROM users").executeUpdate();
            em.flush();
        } catch (Exception ignored) {
        }

        validUser = userRepository.save(User.builder()
                .name("valid_user")
                .email("valid@taskcenter.local")
                .password("pass")
                .telegramChatId(111222333L)
                .telegramNotifyDeadlines(true)
                .build());

        noChatIdUser = userRepository.save(User.builder()
                .name("no_chat_user")
                .email("no_chat@taskcenter.local")
                .password("pass")
                .telegramChatId(null)
                .telegramNotifyDeadlines(true)
                .build());

        disabledNotifyUser = userRepository.save(User.builder()
                .name("disabled_notify_user")
                .email("disabled@taskcenter.local")
                .password("pass")
                .telegramChatId(444555666L)
                .telegramNotifyDeadlines(false)
                .build());

        todoCol = columnRepository.save(BoardColumn.builder()
                .workspaceId("ws-test")
                .title("Dushanbadan")
                .order(1)
                .isDefault(true)
                .isDone(false)
                .build());

        doneCol = columnRepository.save(BoardColumn.builder()
                .workspaceId("ws-test")
                .title("Done")
                .order(3)
                .isDefault(true)
                .isDone(true)
                .build());
    }

    @Test
    @DisplayName("Vaqt oralig'ida muddati tushadigan, o'chirilmagan, bajarilmagan task assignee'si bilan qaytishi kerak")
    void findTasksForReminder_returnsEligibleTasks() {
        LocalDate today = LocalDate.now();
        Task task = Task.builder()
                .workspaceId("ws-test")
                .columnId(todoCol.getId())
                .title("Eslatma vazifasi")
                .publicId("WFM-REM-1")
                .lexoRank("0000000001")
                .dueDate(today.plusDays(1))
                .assignees(Set.of(validUser))
                .build();
        taskRepository.save(task);

        em.flush();
        em.clear();

        List<TelegramReminderTaskDto> results = taskRepository.findTasksWithAssigneesForTelegramReminder(today, today.plusDays(2));

        assertThat(results).hasSize(1);
        TelegramReminderTaskDto found = results.get(0);
        assertThat(found.getTitle()).isEqualTo("Eslatma vazifasi");
        assertThat(found.getTaskId()).isEqualTo(task.getId());
        assertThat(found.getUserId()).isEqualTo(validUser.getId());
        assertThat(found.getTelegramChatId()).isEqualTo(111222333L);
        assertThat(found.getDueDate()).isEqualTo(today.plusDays(1));
        assertThat(found.getWorkspaceId()).isEqualTo("ws-test");
    }

    @Test
    @DisplayName("O'chirilgan (deleted_at IS NOT NULL) tasklar so'rovda qaytmasligi kerak")
    void findTasksForReminder_excludesDeletedTasks() {
        LocalDate today = LocalDate.now();
        Task deletedTask = Task.builder()
                .workspaceId("ws-test")
                .columnId(todoCol.getId())
                .title("O'chirilgan vazifa")
                .publicId("WFM-REM-DEL")
                .lexoRank("0000000002")
                .dueDate(today.plusDays(1))
                .deletedAt(LocalDateTime.now())
                .assignees(Set.of(validUser))
                .build();
        taskRepository.save(deletedTask);

        em.flush();
        em.clear();

        List<TelegramReminderTaskDto> results = taskRepository.findTasksWithAssigneesForTelegramReminder(today, today.plusDays(2));
        assertThat(results).isEmpty();
    }

    @Test
    @DisplayName("Bajarilgan (isDone = true ustunidagi) tasklar so'rovda qaytmasligi kerak")
    void findTasksForReminder_excludesDoneTasks() {
        LocalDate today = LocalDate.now();
        Task doneTask = Task.builder()
                .workspaceId("ws-test")
                .columnId(doneCol.getId())
                .title("Bajarilgan vazifa")
                .publicId("WFM-REM-DONE")
                .lexoRank("0000000003")
                .dueDate(today.plusDays(1))
                .assignees(Set.of(validUser))
                .build();
        taskRepository.save(doneTask);

        em.flush();
        em.clear();

        List<TelegramReminderTaskDto> results = taskRepository.findTasksWithAssigneesForTelegramReminder(today, today.plusDays(2));
        assertThat(results).isEmpty();
    }

    @Test
    @DisplayName("Ixtiyoriy is_done=true qilib belgilangan maxsus ustundagi task ham chiqmasligi kerak")
    void findTasksForReminder_excludesTasksInCustomIsDoneColumn() {
        LocalDate today = LocalDate.now();
        BoardColumn customDoneCol = columnRepository.save(BoardColumn.builder()
                .workspaceId("ws-test")
                .title("Muvaffaqiyatli topshirildi")
                .order(7)
                .isDefault(false)
                .isDone(true)
                .build());

        Task customDoneTask = Task.builder()
                .workspaceId("ws-test")
                .columnId(customDoneCol.getId())
                .title("Maxsus ustundagi bajarilgan vazifa")
                .publicId("WFM-REM-CUSTDONE")
                .lexoRank("0000000007")
                .dueDate(today.plusDays(1))
                .assignees(Set.of(validUser))
                .build();
        taskRepository.save(customDoneTask);

        em.flush();
        em.clear();

        List<TelegramReminderTaskDto> results = taskRepository.findTasksWithAssigneesForTelegramReminder(today, today.plusDays(2));
        assertThat(results).isEmpty();
    }

    @Test
    @DisplayName("TelegramChatId yo'q yoki eslatma sozlamasi o'chiq bo'lgan userlar chiqmasligi kerak")
    void findTasksForReminder_excludesUsersWithNoChatIdOrDisabledNotifications() {
        LocalDate today = LocalDate.now();

        Task taskNoChat = Task.builder()
                .workspaceId("ws-test")
                .columnId(todoCol.getId())
                .title("ChatId yo'q user vazifasi")
                .publicId("WFM-REM-NOCHAT")
                .lexoRank("0000000004")
                .dueDate(today.plusDays(1))
                .assignees(Set.of(noChatIdUser))
                .build();
        taskRepository.save(taskNoChat);

        Task taskDisabled = Task.builder()
                .workspaceId("ws-test")
                .columnId(todoCol.getId())
                .title("Sozlama o'chiq user vazifasi")
                .publicId("WFM-REM-DIS")
                .lexoRank("0000000005")
                .dueDate(today.plusDays(1))
                .assignees(Set.of(disabledNotifyUser))
                .build();
        taskRepository.save(taskDisabled);

        em.flush();
        em.clear();

        List<TelegramReminderTaskDto> results = taskRepository.findTasksWithAssigneesForTelegramReminder(today, today.plusDays(2));
        assertThat(results).isEmpty();
    }

    @Test
    @DisplayName("TelegramReminderLog unique constraint (task_id, user_id, type) takrorlanishni cheklashi kerak")
    void telegramReminderLog_uniqueConstraintEnforced() {
        TelegramReminderLog log1 = TelegramReminderLog.builder()
                .taskId("task-100")
                .userId("user-100")
                .type(TelegramReminderLog.ReminderType.H24)
                .build();
        reminderLogRepository.save(log1);
        em.flush();

        TelegramReminderLog logDuplicate = TelegramReminderLog.builder()
                .taskId("task-100")
                .userId("user-100")
                .type(TelegramReminderLog.ReminderType.H24)
                .build();

        assertThatThrownBy(() -> {
            reminderLogRepository.save(logDuplicate);
            em.flush();
        }).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("Muddat o'zgarganda loglarni tozalash metodi deleteByTaskId o'sha taskning yozuvlarini o'chirishi kerak")
    void telegramReminderLog_deleteByTaskId_deletesOnlyMatchingTaskLogs() {
        TelegramReminderLog log1 = TelegramReminderLog.builder()
                .taskId("task-A")
                .userId("user-1")
                .type(TelegramReminderLog.ReminderType.H24)
                .build();
        TelegramReminderLog log2 = TelegramReminderLog.builder()
                .taskId("task-A")
                .userId("user-1")
                .type(TelegramReminderLog.ReminderType.H1)
                .build();
        TelegramReminderLog log3 = TelegramReminderLog.builder()
                .taskId("task-B")
                .userId("user-1")
                .type(TelegramReminderLog.ReminderType.H24)
                .build();

        reminderLogRepository.saveAll(List.of(log1, log2, log3));
        em.flush();

        assertThat(reminderLogRepository.findByTaskId("task-A")).hasSize(2);
        assertThat(reminderLogRepository.findByTaskId("task-B")).hasSize(1);

        reminderLogRepository.deleteByTaskId("task-A");
        em.flush();
        em.clear();

        assertThat(reminderLogRepository.findByTaskId("task-A")).isEmpty();
        assertThat(reminderLogRepository.findByTaskId("task-B")).hasSize(1);
    }

    @Test
    @DisplayName("Task bazadan o'chirilganda telegram_reminder_log FK CASCADE orqali o'chirilishi kerak")
    void fkCascade_deletesReminderLogsOnTaskDelete() {
        try {
            em.createNativeQuery("ALTER TABLE telegram_reminder_log ADD CONSTRAINT fk_reminder_log_task_cascade FOREIGN KEY (task_id) REFERENCES tasks(id) ON DELETE CASCADE").executeUpdate();
            em.createNativeQuery("ALTER TABLE telegram_reminder_log ADD CONSTRAINT fk_reminder_log_user_cascade FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE").executeUpdate();
        } catch (Exception ignored) {
        }

        Task cascadeTask = taskRepository.save(Task.builder()
                .workspaceId("ws-test")
                .columnId(todoCol.getId())
                .title("FK Cascade vazifasi")
                .publicId("WFM-REM-CASCADE")
                .lexoRank("0000000099")
                .dueDate(LocalDate.now().plusDays(1))
                .build());

        TelegramReminderLog log = reminderLogRepository.save(TelegramReminderLog.builder()
                .taskId(cascadeTask.getId())
                .userId(validUser.getId())
                .type(TelegramReminderLog.ReminderType.H24)
                .build());

        em.flush();
        em.clear();

        assertThat(reminderLogRepository.findByTaskId(cascadeTask.getId())).hasSize(1);

        // Task bazadan o'chirilganda FK cascade orqali tegishli reminder_log ham o'chishi kerak
        em.createNativeQuery("DELETE FROM tasks WHERE id = :id").setParameter("id", cascadeTask.getId()).executeUpdate();
        em.flush();
        em.clear();

        assertThat(reminderLogRepository.findByTaskId(cascadeTask.getId())).isEmpty();
    }

    @Test
    @DisplayName("ReminderType EnumType.STRING orqali saqlanadi va eski H24/H1/OVERDUE yozuvlar xatosiz o'qiladi")
    void reminderType_backwardCompatibilityWithLegacyValues() {
        Task legacyTask = taskRepository.save(Task.builder()
                .workspaceId("ws-test")
                .columnId(todoCol.getId())
                .title("Legacy Task")
                .publicId("WFM-LEGACY-1")
                .lexoRank("0000000088")
                .dueDate(LocalDate.now().plusDays(1))
                .build());

        TelegramReminderLog logH24 = reminderLogRepository.save(TelegramReminderLog.builder()
                .taskId(legacyTask.getId())
                .userId(validUser.getId())
                .type(TelegramReminderLog.ReminderType.H24)
                .build());
        TelegramReminderLog logH1 = reminderLogRepository.save(TelegramReminderLog.builder()
                .taskId(legacyTask.getId())
                .userId(validUser.getId())
                .type(TelegramReminderLog.ReminderType.H1)
                .build());
        TelegramReminderLog logOverdue = reminderLogRepository.save(TelegramReminderLog.builder()
                .taskId(legacyTask.getId())
                .userId(validUser.getId())
                .type(TelegramReminderLog.ReminderType.OVERDUE)
                .build());

        em.flush();
        em.clear();

        // 1. Native SQL orqali tekshirish — DB da aynan string sifatida saqlanganmi
        Object dbTypeH24 = em.createNativeQuery("SELECT type FROM telegram_reminder_log WHERE id = :id")
                .setParameter("id", logH24.getId())
                .getSingleResult();
        assertThat(dbTypeH24).isEqualTo("H24");

        // 2. JPA orqali qayta o'qish — eski qiymatlar xatosiz ReminderType enumiga o'qiladimi
        TelegramReminderLog foundH24 = reminderLogRepository.findById(logH24.getId()).orElseThrow();
        TelegramReminderLog foundH1 = reminderLogRepository.findById(logH1.getId()).orElseThrow();
        TelegramReminderLog foundOverdue = reminderLogRepository.findById(logOverdue.getId()).orElseThrow();

        assertThat(foundH24.getType()).isEqualTo(TelegramReminderLog.ReminderType.H24);
        assertThat(foundH1.getType()).isEqualTo(TelegramReminderLog.ReminderType.H1);
        assertThat(foundOverdue.getType()).isEqualTo(TelegramReminderLog.ReminderType.OVERDUE);
    }

    @Test
    @DisplayName("findTasksForTelegramDigest: bugun va muddati o'tgan tasklar chiqishi, barcha filtrlar (o'chirilgan task/user, is_done, digest=false, chatId yo'q) to'g'ri ishlashi")
    void findTasksForTelegramDigest_filtersAndEligibleTasks() {
        LocalDate today = LocalDate.of(2026, 10, 1);

        // 1. Eligible: bugun muddati bo'lgan task
        Task taskDueToday = Task.builder()
                .workspaceId("ws-test")
                .columnId(todoCol.getId())
                .title("Bugungi vazifa")
                .publicId("WFM-DIG-TODAY")
                .lexoRank("0000000101")
                .dueDate(today)
                .assignees(Set.of(validUser))
                .build();

        // 2. Eligible: muddati o'tgan task (kecha)
        Task taskOverdue = Task.builder()
                .workspaceId("ws-test")
                .columnId(todoCol.getId())
                .title("Muddati o'tgan vazifa")
                .publicId("WFM-DIG-OVERDUE")
                .lexoRank("0000000102")
                .dueDate(today.minusDays(1))
                .assignees(Set.of(validUser))
                .build();

        // 3. Excluded: kelajakdagi task (ertaga)
        Task taskFuture = Task.builder()
                .workspaceId("ws-test")
                .columnId(todoCol.getId())
                .title("Kelajakdagi vazifa")
                .publicId("WFM-DIG-FUTURE")
                .lexoRank("0000000103")
                .dueDate(today.plusDays(1))
                .assignees(Set.of(validUser))
                .build();

        // 4. Excluded: o'chirilgan task (deleted_at IS NOT NULL)
        Task taskDeleted = Task.builder()
                .workspaceId("ws-test")
                .columnId(todoCol.getId())
                .title("O'chirilgan task")
                .publicId("WFM-DIG-DEL")
                .lexoRank("0000000104")
                .dueDate(today)
                .deletedAt(LocalDateTime.now())
                .assignees(Set.of(validUser))
                .build();

        // 5. Excluded: is_done = true ustunidagi task
        Task taskDone = Task.builder()
                .workspaceId("ws-test")
                .columnId(doneCol.getId())
                .title("Bajarilgan task")
                .publicId("WFM-DIG-DONE")
                .lexoRank("0000000105")
                .dueDate(today)
                .assignees(Set.of(validUser))
                .build();

        // 6. Excluded: o'chirilgan user
        User deletedUser = userRepository.save(User.builder()
                .name("deleted_digest_user")
                .email("deleted_digest@taskcenter.local")
                .password("pass")
                .telegramChatId(777888999L)
                .telegramDailyDigest(true)
                .deletedAt(LocalDateTime.now())
                .build());
        Task taskDeletedUser = Task.builder()
                .workspaceId("ws-test")
                .columnId(todoCol.getId())
                .title("O'chirilgan user taski")
                .publicId("WFM-DIG-DELUSER")
                .lexoRank("0000000106")
                .dueDate(today)
                .assignees(Set.of(deletedUser))
                .build();

        // 7. Excluded: telegramDailyDigest = false bo'lgan user
        User disabledDigestUser = userRepository.save(User.builder()
                .name("disabled_digest_user")
                .email("disabled_digest@taskcenter.local")
                .password("pass")
                .telegramChatId(555666777L)
                .telegramDailyDigest(false)
                .build());
        Task taskDisabledDigest = Task.builder()
                .workspaceId("ws-test")
                .columnId(todoCol.getId())
                .title("Digest o'chiq user taski")
                .publicId("WFM-DIG-NODIGEST")
                .lexoRank("0000000107")
                .dueDate(today)
                .assignees(Set.of(disabledDigestUser))
                .build();

        // 8. Excluded: telegramChatId yo'q user
        Task taskNoChat = Task.builder()
                .workspaceId("ws-test")
                .columnId(todoCol.getId())
                .title("ChatId yo'q user taski")
                .publicId("WFM-DIG-NOCHAT")
                .lexoRank("0000000108")
                .dueDate(today)
                .assignees(Set.of(noChatIdUser))
                .build();

        taskRepository.saveAll(List.of(
                taskDueToday, taskOverdue, taskFuture,
                taskDeleted, taskDone, taskDeletedUser,
                taskDisabledDigest, taskNoChat
        ));

        em.flush();
        em.clear();

        List<TelegramReminderTaskDto> results = taskRepository.findTasksForTelegramDigest(today);

        // Faqat bugungi va muddati o'tgan 2 ta task qaytishi kerak
        assertThat(results).hasSize(2);
        assertThat(results)
                .extracting(TelegramReminderTaskDto::getTitle)
                .containsExactlyInAnyOrder("Bugungi vazifa", "Muddati o'tgan vazifa");
    }
}
