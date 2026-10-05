package com.taskcenter.service;

import com.taskcenter.dto.TaskCreateRequest;
import com.taskcenter.dto.TaskDto;
import com.taskcenter.dto.TaskUpdateRequest;
import com.taskcenter.exception.ForbiddenException;
import com.taskcenter.exception.ResourceNotFoundException;
import com.taskcenter.model.BoardColumn;
import com.taskcenter.model.Task;
import com.taskcenter.model.User;
import com.taskcenter.repository.ColumnRepository;
import com.taskcenter.repository.TaskRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TaskServiceTest {

    @Mock
    private TaskRepository taskRepository;
    @Mock
    private ColumnRepository columnRepository;
    @Mock
    private com.taskcenter.repository.SprintRepository sprintRepository;
    @Mock
    private WorkspaceAuthorizationService authorizationService;
    @Mock
    private TaskActivityService activityService;
    @Mock
    private WebSocketNotifier webSocketNotifier;
    @Mock
    private NotificationService notificationService;
    @Mock
    private com.taskcenter.repository.TelegramReminderLogRepository telegramReminderLogRepository;
    @Mock
    private TelegramNotificationService telegramNotificationService;
    @Mock
    private com.taskcenter.repository.UserRepository userRepository;

    @org.mockito.Spy
    private java.time.Clock clock = java.time.Clock.systemDefaultZone();

    @InjectMocks
    private TaskService taskService;

    private final User testUser = User.builder().id("user1").name("elshod").role(User.Role.USER).build();

    private User testUser() {
        return testUser;
    }

    private BoardColumn testColumn() {
        return BoardColumn.builder()
                .id("col1")
                .workspaceId("ws1")
                .title("To Do")
                .order(1)
                .createdAt(LocalDateTime.now())
                .build();
    }

    private Task testTask() {
        return Task.builder()
                .id("task1")
                .publicId("WFM-ABC12345")
                .workspaceId("ws1")
                .columnId("col1")
                .title("Test Task")
                .lexoRank("0000000001")
                .createdAt(LocalDateTime.now())
                .build();
    }

    // ===== createTask =====

    @Test
    void createTask_savesAndReturnsDto() {
        org.mockito.Mockito.lenient().when(authorizationService.checkCanEdit("ws1", testUser())).thenReturn(new com.taskcenter.model.Workspace());
        when(columnRepository.findById("col1")).thenReturn(Optional.of(testColumn()));
        when(taskRepository.findMaxLexoRankByColumnId("col1")).thenReturn("0000000001");
        when(taskRepository.save(any(Task.class))).thenAnswer(inv -> {
            Task t = inv.getArgument(0);
            t.setId("new-task");
            t.setPublicId("WFM-NEW");
            t.setCreatedAt(LocalDateTime.now());
            return t;
        });

        TaskCreateRequest req = new TaskCreateRequest();
        req.setTitle("New Task");
        req.setColumnId("col1");

        TaskDto result = taskService.createTask("ws1", req, testUser());

        assertThat(result.getTitle()).isEqualTo("New Task");
        assertThat(result.getPriority()).isEqualTo(com.taskcenter.model.Priority.MEDIUM);
        assertThat(result.getIssueType()).isEqualTo(com.taskcenter.model.IssueType.TASK);
        verify(taskRepository).save(any(Task.class));
    }

    @Test
    void createTask_withCustomPriorityAndDueDate_savesCorrectly() {
        org.mockito.Mockito.lenient().when(authorizationService.checkCanEdit("ws1", testUser())).thenReturn(new com.taskcenter.model.Workspace());
        when(columnRepository.findById("col1")).thenReturn(Optional.of(testColumn()));
        when(taskRepository.findMaxLexoRankByColumnId("col1")).thenReturn("0000000001");
        when(taskRepository.save(any(Task.class))).thenAnswer(inv -> {
            Task t = inv.getArgument(0);
            t.setId("new-task-2");
            t.setPublicId("WFM-NEW2");
            t.setCreatedAt(LocalDateTime.now());
            return t;
        });

        TaskCreateRequest req = new TaskCreateRequest();
        req.setTitle("Urgent Task");
        req.setColumnId("col1");
        req.setPriority(com.taskcenter.model.Priority.URGENT);
        req.setIssueType(com.taskcenter.model.IssueType.BUG);
        req.setDueDate(java.time.LocalDate.of(2026, 12, 31));
        req.setStoryPoints(8);

        TaskDto result = taskService.createTask("ws1", req, testUser());

        assertThat(result.getPriority()).isEqualTo(com.taskcenter.model.Priority.URGENT);
        assertThat(result.getIssueType()).isEqualTo(com.taskcenter.model.IssueType.BUG);
        assertThat(result.getDueDate()).isEqualTo(java.time.LocalDate.of(2026, 12, 31));
        assertThat(result.getStoryPoints()).isEqualTo(8);
    }

    @Test
    void createTask_withValidSprintId_assignsCorrectly() {
        org.mockito.Mockito.lenient().when(authorizationService.checkCanEdit("ws1", testUser())).thenReturn(new com.taskcenter.model.Workspace());
        when(columnRepository.findById("col1")).thenReturn(Optional.of(testColumn()));
        when(taskRepository.findMaxLexoRankByColumnId("col1")).thenReturn("0000000001");
        com.taskcenter.model.Sprint sprint = com.taskcenter.model.Sprint.builder()
                .id("sprint-1")
                .workspaceId("ws1")
                .name("Sprint 1")
                .status(com.taskcenter.model.SprintStatus.ACTIVE)
                .build();
        when(sprintRepository.findById("sprint-1")).thenReturn(Optional.of(sprint));
        when(taskRepository.save(any(Task.class))).thenAnswer(inv -> {
            Task t = inv.getArgument(0);
            t.setId("new-task-sprint");
            t.setPublicId("WFM-SP1");
            t.setCreatedAt(java.time.LocalDateTime.now());
            return t;
        });

        TaskCreateRequest req = new TaskCreateRequest();
        req.setTitle("Sprint Task");
        req.setColumnId("col1");
        req.setSprintId("sprint-1");

        TaskDto result = taskService.createTask("ws1", req, testUser());

        assertThat(result.getSprintId()).isEqualTo("sprint-1");
    }

    @Test
    void createTask_columnNotFound_throws() {
        org.mockito.Mockito.lenient().when(authorizationService.checkCanEdit("ws1", testUser())).thenReturn(new com.taskcenter.model.Workspace());
        when(columnRepository.findById("col999")).thenReturn(Optional.empty());

        TaskCreateRequest req = new TaskCreateRequest();
        req.setTitle("Task");
        req.setColumnId("col999");

        assertThatThrownBy(() -> taskService.createTask("ws1", req, testUser()))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("col999");
    }

    @Test
    void createTask_columnFromOtherWorkspace_throws() {
        org.mockito.Mockito.lenient().when(authorizationService.checkCanEdit("ws1", testUser())).thenReturn(new com.taskcenter.model.Workspace());
        BoardColumn otherCol = BoardColumn.builder()
                .id("col-other")
                .workspaceId("ws-other")
                .title("Other")
                .order(1)
                .build();
        when(columnRepository.findById("col-other")).thenReturn(Optional.of(otherCol));

        TaskCreateRequest req = new TaskCreateRequest();
        req.setTitle("Task");
        req.setColumnId("col-other");

        assertThatThrownBy(() -> taskService.createTask("ws1", req, testUser()))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("tegishli emas");
    }

    // ===== getTaskById =====

    @Test
    void getTaskById_returnsDto() {
        org.mockito.Mockito.lenient().when(authorizationService.checkAccess("ws1", testUser())).thenReturn(new com.taskcenter.model.Workspace());
        when(taskRepository.findByIdWithDetails("task1")).thenReturn(Optional.of(testTask()));

        TaskDto result = taskService.getTaskById("ws1", "task1", testUser());

        assertThat(result.getTitle()).isEqualTo("Test Task");
    }

    @Test
    void getTaskById_notFound_throws() {
        org.mockito.Mockito.lenient().when(authorizationService.checkAccess("ws1", testUser())).thenReturn(new com.taskcenter.model.Workspace());
        when(taskRepository.findByIdWithDetails("task999")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> taskService.getTaskById("ws1", "task999", testUser()))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void getTaskById_wrongWorkspace_throws() {
        org.mockito.Mockito.lenient().when(authorizationService.checkAccess("ws-other", testUser())).thenReturn(new com.taskcenter.model.Workspace());
        when(taskRepository.findByIdWithDetails("task1")).thenReturn(Optional.of(testTask()));

        assertThatThrownBy(() -> taskService.getTaskById("ws-other", "task1", testUser()))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("tegishli emas");
    }

    // ===== getTaskDirectById =====

    @Test
    void getTaskDirectById_returnsDto() {
        Task task = testTask();
        when(taskRepository.findByIdWithDetails("task1")).thenReturn(Optional.of(task));
        org.mockito.Mockito.lenient().when(authorizationService.checkAccess("ws1", testUser())).thenReturn(new com.taskcenter.model.Workspace());

        TaskDto result = taskService.getTaskDirectById("task1", testUser());

        assertThat(result.getTitle()).isEqualTo("Test Task");
        verify(authorizationService).checkAccess("ws1", testUser());
    }

    @Test
    void getTaskDirectById_notFound_throws() {
        when(taskRepository.findByIdWithDetails("task999")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> taskService.getTaskDirectById("task999", testUser()))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void getTaskDirectById_noAccess_throwsNotFound() {
        Task task = testTask();
        when(taskRepository.findByIdWithDetails("task1")).thenReturn(Optional.of(task));
        doThrow(new ForbiddenException("No access"))
                .when(authorizationService).checkAccess("ws1", testUser());

        assertThatThrownBy(() -> taskService.getTaskDirectById("task1", testUser()))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ===== updateTask =====

    @Test
    void updateTask_updatesFieldsAndSaves() {
        org.mockito.Mockito.lenient().when(authorizationService.checkCanEdit("ws1", testUser())).thenReturn(new com.taskcenter.model.Workspace());
        Task task = testTask();
        when(taskRepository.findByIdWithDetails("task1")).thenReturn(Optional.of(task));        org.mockito.Mockito.lenient().when(taskRepository.findWorkspaceIdById("task1")).thenReturn(Optional.ofNullable("ws1"));
        when(taskRepository.save(any(Task.class))).thenAnswer(inv -> inv.getArgument(0));

        TaskUpdateRequest req = new TaskUpdateRequest();
        req.setTitle("Updated Title");
        req.setDescription("New desc");
        req.setPriority(com.taskcenter.model.Priority.HIGH);
        req.setIssueType(com.taskcenter.model.IssueType.STORY);
        req.setDueDate(java.time.LocalDate.of(2026, 11, 15));
        req.setStoryPoints(5);

        TaskDto result = taskService.updateTask("ws1", "task1", req, testUser());

        assertThat(result.getTitle()).isEqualTo("Updated Title");
        assertThat(result.getDescription()).isEqualTo("New desc");
        assertThat(result.getPriority()).isEqualTo(com.taskcenter.model.Priority.HIGH);
        assertThat(result.getIssueType()).isEqualTo(com.taskcenter.model.IssueType.STORY);
        assertThat(result.getDueDate()).isEqualTo(java.time.LocalDate.of(2026, 11, 15));
        assertThat(result.getStoryPoints()).isEqualTo(5);
        verify(activityService).logActivity("task1", testUser(), com.taskcenter.model.TaskActivityType.STORY_POINTS_UPDATED, "storyPoints", "null", "5");
        verify(telegramReminderLogRepository).deleteByTaskId("task1");
    }

    @Test
    void updateTask_dueDateChangedToNull_clearsReminderLogs() {
        org.mockito.Mockito.lenient().when(authorizationService.checkCanEdit("ws1", testUser())).thenReturn(new com.taskcenter.model.Workspace());
        Task task = testTask();
        task.setDueDate(java.time.LocalDate.of(2026, 10, 20));
        when(taskRepository.findByIdWithDetails("task1")).thenReturn(Optional.of(task));
        when(taskRepository.save(any(Task.class))).thenAnswer(inv -> inv.getArgument(0));

        TaskUpdateRequest req = new TaskUpdateRequest();
        req.setClearDueDate(true);

        TaskDto result = taskService.updateTask("ws1", "task1", req, testUser());

        assertThat(result.getDueDate()).isNull();
        verify(telegramReminderLogRepository).deleteByTaskId("task1");
    }

    @Test
    void updateTask_clearDueDateNullOrFalse_preservesDueDate() {
        org.mockito.Mockito.lenient().when(authorizationService.checkCanEdit("ws1", testUser())).thenReturn(new com.taskcenter.model.Workspace());
        Task task = testTask();
        task.setDueDate(java.time.LocalDate.of(2026, 10, 20));
        when(taskRepository.findByIdWithDetails("task1")).thenReturn(Optional.of(task));
        when(taskRepository.save(any(Task.class))).thenAnswer(inv -> inv.getArgument(0));

        // When clearDueDate is not provided (null) and dueDate is null
        TaskUpdateRequest req = new TaskUpdateRequest();
        req.setTitle("Updated Title Only");

        TaskDto result = taskService.updateTask("ws1", "task1", req, testUser());

        assertThat(result.getDueDate()).isEqualTo(java.time.LocalDate.of(2026, 10, 20));
        verify(telegramReminderLogRepository, never()).deleteByTaskId("task1");

        // When clearDueDate is explicitly false and dueDate is null
        req.setClearDueDate(false);
        req.setDueDate(null);

        TaskDto result2 = taskService.updateTask("ws1", "task1", req, testUser());

        assertThat(result2.getDueDate()).isEqualTo(java.time.LocalDate.of(2026, 10, 20));
        verify(telegramReminderLogRepository, never()).deleteByTaskId("task1");
    }

    @Test
    void updateTask_moveToAnotherColumn() {
        org.mockito.Mockito.lenient().when(authorizationService.checkCanEdit("ws1", testUser())).thenReturn(new com.taskcenter.model.Workspace());
        Task task = testTask();
        BoardColumn newCol = BoardColumn.builder().id("col2").workspaceId("ws1").title("Done").order(2).build();
        when(taskRepository.findByIdWithDetails("task1")).thenReturn(Optional.of(task));        org.mockito.Mockito.lenient().when(taskRepository.findWorkspaceIdById("task1")).thenReturn(Optional.ofNullable("ws1"));
        when(columnRepository.findById("col2")).thenReturn(Optional.of(newCol));
        when(taskRepository.save(any(Task.class))).thenAnswer(inv -> inv.getArgument(0));

        TaskUpdateRequest req = new TaskUpdateRequest();
        req.setColumnId("col2");

        TaskDto result = taskService.updateTask("ws1", "task1", req, testUser());

        assertThat(task.getColumnId()).isEqualTo("col2");
        verify(telegramNotificationService).sendTaskMovedNotification(any(), eq(testUser()), any(), eq("Done"));
    }

    @Test
    void reorderTask_withinSameColumn_doesNotSendTelegramNotification() {
        org.mockito.Mockito.lenient().when(authorizationService.checkCanEdit("ws1", testUser())).thenReturn(new com.taskcenter.model.Workspace());
        Task task = testTask(); // columnId = "col1"
        when(taskRepository.findByIdWithDetails("task1")).thenReturn(Optional.of(task));
        when(taskRepository.save(any(Task.class))).thenAnswer(inv -> inv.getArgument(0));

        com.taskcenter.dto.TaskReorderRequest req = new com.taskcenter.dto.TaskReorderRequest();
        req.setColumnId("col1"); // ayni o'sha ustun
        req.setPrevRank("0000000001");
        req.setNextRank("0000000003");

        taskService.reorderTask("ws1", "task1", req, testUser());

        verify(telegramNotificationService, never()).sendTaskMovedNotification(any(), any(), any(), any());
    }

    @Test
    void reorderTask_toDifferentColumn_sendsTelegramNotification() {
        org.mockito.Mockito.lenient().when(authorizationService.checkCanEdit("ws1", testUser())).thenReturn(new com.taskcenter.model.Workspace());
        Task task = testTask(); // columnId = "col1"
        BoardColumn newCol = BoardColumn.builder().id("col2").workspaceId("ws1").title("In Progress").order(2).build();
        BoardColumn oldCol = BoardColumn.builder().id("col1").workspaceId("ws1").title("To Do").order(1).build();

        when(taskRepository.findByIdWithDetails("task1")).thenReturn(Optional.of(task));
        when(columnRepository.findById("col2")).thenReturn(Optional.of(newCol));
        when(columnRepository.findById("col1")).thenReturn(Optional.of(oldCol));
        when(taskRepository.save(any(Task.class))).thenAnswer(inv -> inv.getArgument(0));

        com.taskcenter.dto.TaskReorderRequest req = new com.taskcenter.dto.TaskReorderRequest();
        req.setColumnId("col2"); // boshqa ustun
        req.setPrevRank("0000000001");
        req.setNextRank("0000000003");

        taskService.reorderTask("ws1", "task1", req, testUser());

        assertThat(task.getColumnId()).isEqualTo("col2");
        verify(telegramNotificationService).sendTaskMovedNotification(any(), eq(testUser()), eq("To Do"), eq("In Progress"));
    }

    // ===== deleteTask =====

    @Test
    void deleteTask_deletesSuccessfully() {
        org.mockito.Mockito.lenient().when(authorizationService.checkCanEdit("ws1", testUser())).thenReturn(new com.taskcenter.model.Workspace());
        when(taskRepository.findByIdWithDetails("task1")).thenReturn(Optional.of(testTask()));

        taskService.deleteTask("ws1", "task1", testUser());

        verify(taskRepository).deleteById("task1");
    }

    @Test
    void deleteTask_noAccess_throwsForbidden() {
        User stranger = User.builder().id("stranger").name("other").role(User.Role.USER).build();
        doThrow(new ForbiddenException("Ruxsat yo'q"))
                .when(authorizationService).checkCanEdit("ws1", stranger);

        assertThatThrownBy(() -> taskService.deleteTask("ws1", "task1", stranger))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void getBoard_withSprintIdFilter_filtersCorrectly() {
        // Sprint bo'yicha filtr endi bazada (findBySprintId), servis esa tasklarni ustunlarga taqsimlaydi
        Task t1 = Task.builder().id("t1").title("Task 1").columnId("col1").sprintId("sprint-1").lexoRank("0000000002").build();
        Task t2 = Task.builder().id("t2").title("Task 2").columnId("col2").sprintId("sprint-1").lexoRank("0000000001").build();
        Task t3 = Task.builder().id("t3").title("Task 3").columnId("col1").sprintId("sprint-1").lexoRank("0000000001").build();
        BoardColumn col1 = BoardColumn.builder().id("col1").workspaceId("ws1").title("Todo").order(1).build();
        BoardColumn col2 = BoardColumn.builder().id("col2").workspaceId("ws1").title("Done").order(2).build();
        when(columnRepository.findByWorkspaceIdOrderByOrderAsc("ws1")).thenReturn(List.of(col1, col2));
        when(taskRepository.findBySprintId("sprint-1")).thenReturn(List.of(t1, t2, t3));

        List<com.taskcenter.dto.ColumnWithCardsDto> board = taskService.getBoard("ws1", "sprint-1", testUser());

        assertThat(board).hasSize(2);
        // col1 da t3 va t1 — lexoRank bo'yicha tartiblangan
        assertThat(board.get(0).getCards()).extracting("id").containsExactly("t3", "t1");
        assertThat(board.get(1).getCards()).extracting("id").containsExactly("t2");
    }

    @Test
    void reorderTask_updatesLexoRank() {
        org.mockito.Mockito.lenient().when(authorizationService.checkCanEdit("ws1", testUser())).thenReturn(new com.taskcenter.model.Workspace());
        Task task = testTask();
        when(taskRepository.findByIdWithDetails("task1")).thenReturn(Optional.of(task));        org.mockito.Mockito.lenient().when(taskRepository.findWorkspaceIdById("task1")).thenReturn(Optional.ofNullable("ws1"));
        when(taskRepository.save(any(Task.class))).thenAnswer(inv -> inv.getArgument(0));

        com.taskcenter.dto.TaskReorderRequest req = new com.taskcenter.dto.TaskReorderRequest();
        req.setPrevRank("0000000001");
        req.setNextRank("0000000003");

        TaskDto result = taskService.reorderTask("ws1", "task1", req, testUser());

        assertThat(result.getLexoRank()).isNotNull();
        verify(taskRepository).save(any(Task.class));
    }

    @Test
    void getTasksByWorkspace_withSearchFilter_normalizesAndCallsRepository() {
        org.mockito.Mockito.lenient().when(authorizationService.checkAccess(eq("ws1"), any(User.class)))
                .thenReturn(new com.taskcenter.model.Workspace());

        Task t1 = Task.builder().id("t1").title("Swagger documentation task").workspaceId("ws1").build();
        org.springframework.data.domain.Page<String> idPage = new org.springframework.data.domain.PageImpl<>(List.of("t1"));
        com.taskcenter.dto.TaskFilterRequest filter = new com.taskcenter.dto.TaskFilterRequest();
        filter.setSearch("  Swagger  ");
        when(taskRepository.findIdsByWorkspaceIdFiltered(eq("ws1"), any(com.taskcenter.dto.TaskFilterRequest.class), any(org.springframework.data.domain.Pageable.class)))
                .thenReturn(idPage);
        when(taskRepository.findByIdIn(List.of("t1"))).thenReturn(List.of(t1));

        org.springframework.data.domain.Page<TaskDto> result = taskService.getTasksByWorkspace("ws1", testUser(), filter, org.springframework.data.domain.PageRequest.of(0, 10));

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getTitle()).isEqualTo("Swagger documentation task");
        assertThat(filter.getSearch()).isEqualTo("Swagger");
    }

    @Test
    void getTasksByWorkspace_withQParameter_mapsToSearch() {
        org.mockito.Mockito.lenient().when(authorizationService.checkAccess(eq("ws1"), any(User.class)))
                .thenReturn(new com.taskcenter.model.Workspace());

        Task t1 = Task.builder().id("t1").title("Bug in login").workspaceId("ws1").build();
        org.springframework.data.domain.Page<String> idPage = new org.springframework.data.domain.PageImpl<>(List.of("t1"));
        com.taskcenter.dto.TaskFilterRequest filter = new com.taskcenter.dto.TaskFilterRequest();
        filter.setQ("Bug");
        when(taskRepository.findIdsByWorkspaceIdFiltered(eq("ws1"), any(com.taskcenter.dto.TaskFilterRequest.class), any(org.springframework.data.domain.Pageable.class)))
                .thenReturn(idPage);
        when(taskRepository.findByIdIn(List.of("t1"))).thenReturn(List.of(t1));

        org.springframework.data.domain.Page<TaskDto> result = taskService.getTasksByWorkspace("ws1", testUser(), filter, org.springframework.data.domain.PageRequest.of(0, 10));

        assertThat(result.getContent()).hasSize(1);
        assertThat(filter.getSearch()).isEqualTo("Bug");
    }

    @Test
    void getTasksByWorkspace_withNullFilter_createsDefaultFilter() {
        org.mockito.Mockito.lenient().when(authorizationService.checkAccess(eq("ws1"), any(User.class)))
                .thenReturn(new com.taskcenter.model.Workspace());

        org.springframework.data.domain.Page<String> idPage = new org.springframework.data.domain.PageImpl<>(List.of());
        when(taskRepository.findIdsByWorkspaceIdFiltered(eq("ws1"), any(com.taskcenter.dto.TaskFilterRequest.class), any(org.springframework.data.domain.Pageable.class)))
                .thenReturn(idPage);
        when(taskRepository.findByIdIn(List.of())).thenReturn(List.of());

        org.springframework.data.domain.Page<TaskDto> result = taskService.getTasksByWorkspace("ws1", testUser(), null, org.springframework.data.domain.PageRequest.of(0, 10));

        assertThat(result).isNotNull();
        verify(taskRepository).findIdsByWorkspaceIdFiltered(eq("ws1"), any(com.taskcenter.dto.TaskFilterRequest.class), any(org.springframework.data.domain.Pageable.class));
    }

    // === FIX #10 — TaskUpdateRequest: title="" validatsiyadan o'tmasligi kerak ===

    private static final jakarta.validation.Validator VALIDATOR =
            jakarta.validation.Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void taskUpdateRequest_emptyTitle_failsValidation() {
        // BUG: avval @Size(max=255) bilan bo'sh string o'tib ketardi
        // FIX: @Size(min=1, max=255) bo'sh stringni rad etadi
        com.taskcenter.dto.TaskUpdateRequest req = new com.taskcenter.dto.TaskUpdateRequest();
        req.setTitle(""); // bo'sh string

        var violations = VALIDATOR.validate(req);

        assertThat(violations).isNotEmpty();
        assertThat(violations.stream().map(v -> v.getPropertyPath().toString()))
                .contains("title");
        assertThat(violations.stream().map(v -> v.getMessage()).findFirst().get())
                .contains("bo'sh joylardan");
    }

    @Test
    void taskUpdateRequest_nullTitle_passesValidation() {
        // null = "o'zgartirmaydi" — bu PARTIAL UPDATE uchun to'g'ri
        com.taskcenter.dto.TaskUpdateRequest req = new com.taskcenter.dto.TaskUpdateRequest();
        req.setTitle(null); // o'zgartirmaslik uchun null

        var violations = VALIDATOR.validate(req);

        // title uchun violation bo'lmasligi kerak (null @Size da tekshirilmaydi)
        long titleViolations = violations.stream()
                .filter(v -> v.getPropertyPath().toString().equals("title"))
                .count();
        assertThat(titleViolations).isZero();
    }

    @Test
    void taskUpdateRequest_validTitle_passesValidation() {
        com.taskcenter.dto.TaskUpdateRequest req = new com.taskcenter.dto.TaskUpdateRequest();
        req.setTitle("To'g'ri sarlavha");

        var violations = VALIDATOR.validate(req);

        long titleViolations = violations.stream()
                .filter(v -> v.getPropertyPath().toString().equals("title"))
                .count();
        assertThat(titleViolations).isZero();
    }

    @Test
    void taskUpdateRequest_tooLongTitle_failsValidation() {
        com.taskcenter.dto.TaskUpdateRequest req = new com.taskcenter.dto.TaskUpdateRequest();
        req.setTitle("A".repeat(256)); // 256 belgi — maksimumdan 1 ta ko'p

        var violations = VALIDATOR.validate(req);

        assertThat(violations.stream().map(v -> v.getPropertyPath().toString()))
                .contains("title");
    }

    @Test
    void toggleAssignee_addsAssignee_callsTelegramNotificationService() {
        String workspaceId = "ws-1";
        String taskId = "t-1";
        String targetUserId = "user-2";
        User currentUser = testUser();

        Task task = Task.builder()
                .id(taskId)
                .workspaceId(workspaceId)
                .title("Test Task")
                .assignees(new java.util.HashSet<>())
                .build();

        User targetUser = User.builder().id(targetUserId).name("nodir").build();

        when(taskRepository.findByIdWithDetails(taskId)).thenReturn(Optional.of(task));
        when(userRepository.findById(targetUserId)).thenReturn(Optional.of(targetUser));
        when(taskRepository.save(any(Task.class))).thenAnswer(invocation -> invocation.getArgument(0));

        TaskDto result = taskService.toggleAssignee(workspaceId, taskId, targetUserId, currentUser);

        assertThat(result).isNotNull();
        verify(telegramNotificationService).sendTaskAssignedNotification(any(Task.class), eq(targetUser), eq(currentUser));
        verify(webSocketNotifier).notifyWorkspace(eq(workspaceId), any());
    }

    @Test
    void toggleAssignee_removesAssignee_doesNotCallTelegramNotificationService() {
        String workspaceId = "ws-1";
        String taskId = "t-1";
        String targetUserId = "user-2";
        User currentUser = testUser();

        User targetUser = User.builder().id(targetUserId).name("nodir").build();
        java.util.Set<User> assignees = new java.util.HashSet<>();
        assignees.add(targetUser);

        Task task = Task.builder()
                .id(taskId)
                .workspaceId(workspaceId)
                .title("Test Task")
                .assignees(assignees)
                .build();

        when(taskRepository.findByIdWithDetails(taskId)).thenReturn(Optional.of(task));
        when(userRepository.findById(targetUserId)).thenReturn(Optional.of(targetUser));
        when(taskRepository.save(any(Task.class))).thenAnswer(invocation -> invocation.getArgument(0));

        TaskDto result = taskService.toggleAssignee(workspaceId, taskId, targetUserId, currentUser);

        assertThat(result).isNotNull();
        verifyNoInteractions(telegramNotificationService);
        verify(webSocketNotifier).notifyWorkspace(eq(workspaceId), any());
    }
    @Test
    void cloneTask_WhenTitleTooLong_ShouldTruncate() {
        String longTitle = "A".repeat(250);
        Task originalTask = Task.builder()
                .workspaceId("ws-1")
                .columnId("col-1")
                .title(longTitle)
                .build();
        
        when(authorizationService.checkCanEdit(any(), any())).thenReturn(null);
        when(taskRepository.findByIdWithDetails("task-1")).thenReturn(Optional.of(originalTask));
        when(taskRepository.findMaxLexoRankByColumnId("col-1")).thenReturn("0|hzzzzz:");
        when(taskRepository.save(any(Task.class))).thenAnswer(i -> {
            Task t = i.getArgument(0);
            t.setId("new-id");
            return t;
        });
        
        TaskDto result = taskService.cloneTask("ws-1", "task-1", new User());
        
        assertThat(result.getTitle()).startsWith("(Copy) ");
        assertThat(result.getTitle().length()).isEqualTo(255);
    }
}
