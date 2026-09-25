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

    // ===== updateTask =====

    @Test
    void updateTask_updatesFieldsAndSaves() {
        org.mockito.Mockito.lenient().when(authorizationService.checkCanEdit("ws1", testUser())).thenReturn(new com.taskcenter.model.Workspace());
        Task task = testTask();
        when(taskRepository.findById("task1")).thenReturn(Optional.of(task));        org.mockito.Mockito.lenient().when(taskRepository.findWorkspaceIdById("task1")).thenReturn(Optional.ofNullable("ws1"));
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
    }

    @Test
    void updateTask_moveToAnotherColumn() {
        org.mockito.Mockito.lenient().when(authorizationService.checkCanEdit("ws1", testUser())).thenReturn(new com.taskcenter.model.Workspace());
        Task task = testTask();
        BoardColumn newCol = BoardColumn.builder().id("col2").workspaceId("ws1").title("Done").order(2).build();
        when(taskRepository.findById("task1")).thenReturn(Optional.of(task));        org.mockito.Mockito.lenient().when(taskRepository.findWorkspaceIdById("task1")).thenReturn(Optional.ofNullable("ws1"));
        when(columnRepository.findById("col2")).thenReturn(Optional.of(newCol));
        when(taskRepository.save(any(Task.class))).thenAnswer(inv -> inv.getArgument(0));

        TaskUpdateRequest req = new TaskUpdateRequest();
        req.setColumnId("col2");

        TaskDto result = taskService.updateTask("ws1", "task1", req, testUser());

        assertThat(task.getColumnId()).isEqualTo("col2");
    }

    // ===== deleteTask =====

    @Test
    void deleteTask_deletesSuccessfully() {
        org.mockito.Mockito.lenient().when(authorizationService.checkCanEdit("ws1", testUser())).thenReturn(new com.taskcenter.model.Workspace());
        when(taskRepository.findById("task1")).thenReturn(Optional.of(testTask()));

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
        org.mockito.Mockito.lenient().when(authorizationService.checkAccess("ws1", testUser())).thenReturn(new com.taskcenter.model.Workspace());
        Task t1 = Task.builder().id("t1").title("Task 1").sprintId("sprint-1").lexoRank("0000000001").build();
        Task t2 = Task.builder().id("t2").title("Task 2").sprintId("sprint-2").lexoRank("0000000002").build();
        BoardColumn col = BoardColumn.builder().id("col1").workspaceId("ws1").title("Todo").order(1).tasks(new java.util.HashSet<>(List.of(t1, t2))).build();
        when(columnRepository.findByWorkspaceIdWithTasks("ws1")).thenReturn(List.of(col));

        List<com.taskcenter.dto.ColumnWithCardsDto> board = taskService.getBoard("ws1", "sprint-1", testUser());

        assertThat(board).hasSize(1);
        assertThat(board.get(0).getCards()).hasSize(1);
        assertThat(board.get(0).getCards().get(0).getId()).isEqualTo("t1");
    }

    @Test
    void reorderTask_updatesLexoRank() {
        org.mockito.Mockito.lenient().when(authorizationService.checkCanEdit("ws1", testUser())).thenReturn(new com.taskcenter.model.Workspace());
        Task task = testTask();
        when(taskRepository.findById("task1")).thenReturn(Optional.of(task));        org.mockito.Mockito.lenient().when(taskRepository.findWorkspaceIdById("task1")).thenReturn(Optional.ofNullable("ws1"));
        when(taskRepository.save(any(Task.class))).thenAnswer(inv -> inv.getArgument(0));

        com.taskcenter.dto.TaskReorderRequest req = new com.taskcenter.dto.TaskReorderRequest();
        req.setPrevRank("0000000001");
        req.setNextRank("0000000003");

        TaskDto result = taskService.reorderTask("ws1", "task1", req, testUser());

        assertThat(result.getLexoRank()).isNotNull();
        verify(taskRepository).save(any(Task.class));
    }
}
