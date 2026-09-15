package com.taskcenter.service;

import com.taskcenter.dto.BoardEvent;
import com.taskcenter.dto.TaskCreateRequest;
import com.taskcenter.dto.TaskDto;
import com.taskcenter.dto.TaskUpdateRequest;
import com.taskcenter.exception.EntityNotFoundException;
import com.taskcenter.model.BoardColumn;
import com.taskcenter.model.Task;
import com.taskcenter.model.User;
import com.taskcenter.model.Workspace;
import com.taskcenter.model.Priority;
import com.taskcenter.repository.ColumnRepository;
import com.taskcenter.repository.LabelRepository;
import com.taskcenter.repository.TaskRepository;
import com.taskcenter.repository.UserRepository;
import com.taskcenter.repository.WorkspaceMemberRepository;
import com.taskcenter.repository.WorkspaceRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BoardServiceTest {

    @Mock
    private ColumnRepository columnRepository;
    @Mock
    private TaskRepository taskRepository;
    @Mock
    private LabelRepository labelRepository;
    @Mock
    private WorkspaceMemberRepository memberRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private WorkspaceRepository workspaceRepository;
    @Mock
    private WorkspaceAuthorizationService authorizationService;
    @Mock
    private WebSocketEventPublisher webSocketEventPublisher;

    @InjectMocks
    private BoardService boardService;

    private User currentUser() {
        return User.builder().id("user-1").name("tester").email("test@example.com").build();
    }

    private Workspace workspaceWithCounter5() {
        return Workspace.builder()
                .id("ws-1")
                .title("Test Workspace")
                .ownerId("user-1")
                .keyPrefix("TC")
                .taskCounter(5)
                .build();
    }

    private BoardColumn column() {
        return BoardColumn.builder()
                .id("col-1")
                .workspaceId("ws-1")
                .title("To Do")
                .order(1)
                .build();
    }

    private TaskCreateRequest createRequest() {
        TaskCreateRequest req = new TaskCreateRequest();
        req.setColumnId("col-1");
        req.setTitle("Test Task");
        return req;
    }

    @Test
    void createTask_incrementsCounterAndGeneratesPublicId() {
        Workspace ws = workspaceWithCounter5();
        when(columnRepository.findById("col-1")).thenReturn(Optional.of(column()));
        when(taskRepository.findMaxOrderByColumnId("col-1")).thenReturn(0);
        when(workspaceRepository.findByIdForUpdate("ws-1")).thenReturn(Optional.of(ws));

        TaskDto result = boardService.createTask("ws-1", createRequest(), currentUser());

        assertThat(result.getPublicId()).isEqualTo("TC-6");
        assertThat(ws.getTaskCounter()).isEqualTo(6);
        verify(workspaceRepository).save(ws);
    }

    @Test
    void createTask_usesFindByIdForUpdateForWorkspaceLock() {
        Workspace ws = workspaceWithCounter5();
        when(columnRepository.findById("col-1")).thenReturn(Optional.of(column()));
        when(taskRepository.findMaxOrderByColumnId("col-1")).thenReturn(0);
        when(workspaceRepository.findByIdForUpdate("ws-1")).thenReturn(Optional.of(ws));

        boardService.createTask("ws-1", createRequest(), currentUser());

        verify(workspaceRepository).findByIdForUpdate("ws-1");
        verify(workspaceRepository, never()).findById(any());
    }

    @Test
    void createTask_workspaceNotFound_throwsEntityNotFound() {
        when(columnRepository.findById("col-1")).thenReturn(Optional.of(column()));
        when(workspaceRepository.findByIdForUpdate("ws-1")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> boardService.createTask("ws-1", createRequest(), currentUser()))
                .isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    void createTask_noPriority_setsDefaultMedium() {
        Workspace ws = workspaceWithCounter5();
        when(columnRepository.findById("col-1")).thenReturn(Optional.of(column()));
        when(taskRepository.findMaxOrderByColumnId("col-1")).thenReturn(0);
        when(workspaceRepository.findByIdForUpdate("ws-1")).thenReturn(Optional.of(ws));

        boardService.createTask("ws-1", createRequest(), currentUser());

        ArgumentCaptor<Task> captor = ArgumentCaptor.forClass(Task.class);
        verify(taskRepository).save(captor.capture());
        assertThat(captor.getValue().getPriority()).isEqualTo(Priority.MEDIUM);
    }

    @Test
    void createTask_withAssigneeAndDueDate_setsThemOnTask() {
        Workspace ws = workspaceWithCounter5();
        LocalDateTime dueDate = LocalDateTime.of(2026, 9, 30, 12, 0);
        when(columnRepository.findById("col-1")).thenReturn(Optional.of(column()));
        when(taskRepository.findMaxOrderByColumnId("col-1")).thenReturn(0);
        when(workspaceRepository.findByIdForUpdate("ws-1")).thenReturn(Optional.of(ws));
        when(userRepository.existsById("user-2")).thenReturn(true);
        when(authorizationService.isUserMemberOfWorkspace("ws-1", "user-2")).thenReturn(true);

        TaskCreateRequest req = createRequest();
        req.setAssigneeId("user-2");
        req.setDueDate(dueDate);

        boardService.createTask("ws-1", req, currentUser());

        ArgumentCaptor<Task> captor = ArgumentCaptor.forClass(Task.class);
        verify(taskRepository).save(captor.capture());
        assertThat(captor.getValue().getAssigneeId()).isEqualTo("user-2");
        assertThat(captor.getValue().getDueDate()).isEqualTo(dueDate);
    }

    @Test
    void createTask_publishesTaskCreatedEvent() {
        Workspace ws = workspaceWithCounter5();
        when(columnRepository.findById("col-1")).thenReturn(Optional.of(column()));
        when(taskRepository.findMaxOrderByColumnId("col-1")).thenReturn(0);
        when(workspaceRepository.findByIdForUpdate("ws-1")).thenReturn(Optional.of(ws));

        boardService.createTask("ws-1", createRequest(), currentUser());

        verify(webSocketEventPublisher).publishBoardEvent(
                eq("ws-1"),
                argThat((BoardEvent e) -> e.getType() == BoardEvent.Type.TASK_CREATED));
    }

    @Test
    void updateTask_updatesFieldsAndKeepsPublicId() {
        Task existing = Task.builder()
                .id("task-1")
                .workspaceId("ws-1")
                .columnId("col-1")
                .title("Old Title")
                .publicId("TC-6")
                .build();
        when(taskRepository.findById("task-1")).thenReturn(Optional.of(existing));

        TaskUpdateRequest req = new TaskUpdateRequest();
        req.setTitle("New Title");

        TaskDto result = boardService.updateTask("ws-1", "task-1", req, currentUser());

        assertThat(result.getTitle()).isEqualTo("New Title");
        assertThat(result.getPublicId()).isEqualTo("TC-6");
        verify(webSocketEventPublisher).publishBoardEvent(
                eq("ws-1"),
                argThat((BoardEvent e) -> e.getType() == BoardEvent.Type.TASK_UPDATED));
    }

    @Test
    void updateTask_taskNotFound_throwsEntityNotFound() {
        when(taskRepository.findById("task-1")).thenReturn(Optional.empty());

        TaskUpdateRequest req = new TaskUpdateRequest();
        req.setTitle("New Title");

        assertThatThrownBy(() -> boardService.updateTask("ws-1", "task-1", req, currentUser()))
                .isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    void deleteTask_deletesAndPublishesEvent() {
        Task existing = Task.builder()
                .id("task-1")
                .workspaceId("ws-1")
                .columnId("col-1")
                .title("Old Title")
                .publicId("TC-6")
                .build();
        when(taskRepository.findById("task-1")).thenReturn(Optional.of(existing));

        boardService.deleteTask("ws-1", "task-1", currentUser());

        verify(taskRepository).deleteById("task-1");
        verify(webSocketEventPublisher).publishBoardEvent(
                eq("ws-1"),
                argThat((BoardEvent e) -> e.getType() == BoardEvent.Type.TASK_DELETED));
    }
}
