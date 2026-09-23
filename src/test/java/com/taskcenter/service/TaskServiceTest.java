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
    private WorkspaceAuthorizationService authorizationService;

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
                .order(1)
                .createdAt(LocalDateTime.now())
                .build();
    }

    // ===== createTask =====

    @Test
    void createTask_savesAndReturnsDto() {
        doNothing().when(authorizationService).checkAccess("ws1", testUser());
        when(columnRepository.findById("col1")).thenReturn(Optional.of(testColumn()));
        when(taskRepository.findMaxOrderByColumnId("col1")).thenReturn(0);
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
        verify(taskRepository).save(any(Task.class));
    }

    @Test
    void createTask_columnNotFound_throws() {
        doNothing().when(authorizationService).checkAccess("ws1", testUser());
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
        doNothing().when(authorizationService).checkAccess("ws1", testUser());
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
        doNothing().when(authorizationService).checkAccess("ws1", testUser());
        when(taskRepository.findByIdWithDetails("task1")).thenReturn(Optional.of(testTask()));

        TaskDto result = taskService.getTaskById("ws1", "task1", testUser());

        assertThat(result.getTitle()).isEqualTo("Test Task");
    }

    @Test
    void getTaskById_notFound_throws() {
        doNothing().when(authorizationService).checkAccess("ws1", testUser());
        when(taskRepository.findByIdWithDetails("task999")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> taskService.getTaskById("ws1", "task999", testUser()))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void getTaskById_wrongWorkspace_throws() {
        doNothing().when(authorizationService).checkAccess("ws-other", testUser());
        when(taskRepository.findByIdWithDetails("task1")).thenReturn(Optional.of(testTask()));

        assertThatThrownBy(() -> taskService.getTaskById("ws-other", "task1", testUser()))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("tegishli emas");
    }

    // ===== updateTask =====

    @Test
    void updateTask_updatesFieldsAndSaves() {
        doNothing().when(authorizationService).checkAccess("ws1", testUser());
        Task task = testTask();
        when(taskRepository.findById("task1")).thenReturn(Optional.of(task));
        when(taskRepository.save(any(Task.class))).thenAnswer(inv -> inv.getArgument(0));

        TaskUpdateRequest req = new TaskUpdateRequest();
        req.setTitle("Updated Title");
        req.setDescription("New desc");

        TaskDto result = taskService.updateTask("ws1", "task1", req, testUser());

        assertThat(result.getTitle()).isEqualTo("Updated Title");
        assertThat(result.getDescription()).isEqualTo("New desc");
    }

    @Test
    void updateTask_moveToAnotherColumn() {
        doNothing().when(authorizationService).checkAccess("ws1", testUser());
        Task task = testTask();
        BoardColumn newCol = BoardColumn.builder().id("col2").workspaceId("ws1").title("Done").order(2).build();
        when(taskRepository.findById("task1")).thenReturn(Optional.of(task));
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
        doNothing().when(authorizationService).checkAccess("ws1", testUser());
        when(taskRepository.findById("task1")).thenReturn(Optional.of(testTask()));

        taskService.deleteTask("ws1", "task1", testUser());

        verify(taskRepository).deleteById("task1");
    }

    @Test
    void deleteTask_noAccess_throwsForbidden() {
        User stranger = User.builder().id("stranger").name("other").role(User.Role.USER).build();
        doThrow(new ForbiddenException("Ruxsat yo'q"))
                .when(authorizationService).checkAccess("ws1", stranger);

        assertThatThrownBy(() -> taskService.deleteTask("ws1", "task1", stranger))
                .isInstanceOf(ForbiddenException.class);
    }
}
