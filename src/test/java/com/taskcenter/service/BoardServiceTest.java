package com.taskcenter.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.taskcenter.dto.TaskCreateRequest;
import com.taskcenter.dto.TaskUpdateRequest;
import com.taskcenter.exception.EntityNotFoundException;
import com.taskcenter.model.*;
import com.taskcenter.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;

@ExtendWith(MockitoExtension.class)
class BoardServiceTest {

    @Mock
    private BoardRepository boardRepository;

    @Mock
    private WorkspaceRepository workspaceRepository;

    @Mock
    private TaskRepository taskRepository;

    @Mock
    private LabelRepository labelRepository;

    @Mock
    private WorkspaceMemberRepository memberRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private WorkspaceAuthorizationService authorizationService;

    @InjectMocks
    private BoardService boardService;

    private Workspace testWorkspace;
    private User testUser;
    private BoardColumn testColumn;

    @BeforeEach
    void setUp() {
        testWorkspace = Workspace.builder()
                .id("ws-1")
                .title("Test Workspace")
                .keyPrefix("TC")
                .taskCounter(5)
                .ownerId("user-1")
                .build();

        testUser = User.builder()
                .id("user-1")
                .email("test@example.com")
                .build();

        testColumn = BoardColumn.builder()
                .id("col-1")
                .title("Test Column")
                .workspaceId("ws-1")
                .build();
    }

    @Test
    void createTask_givenCounterIs5_generatesPublicIdTc6() {
        // Arrange
        when(authorizationService.checkAccess(anyString(), any(User.class))).thenReturn(null);
        when(workspaceRepository.findByIdForUpdate("ws-1")).thenReturn(Optional.of(testWorkspace));
        when(taskRepository.findMaxOrderByColumnId("col-1")).thenReturn(0);
        when(taskRepository.save(any(Task.class))).thenAnswer(invocation -> invocation.getArgument(0));

        TaskCreateRequest request = TaskCreateRequest.builder()
                .title("Test Task")
                .columnId("col-1")
                .build();

        // Act
        boardService.createTask("ws-1", request, testUser);

        // Assert
        assertThat(testWorkspace.getTaskCounter()).isEqualTo(6);
        verify(workspaceRepository).save(testWorkspace);
    }

    @Test
    void createTask_usesFindByIdForUpdate_forWorkspaceLock() {
        // Arrange
        when(authorizationService.checkAccess(anyString(), any(User.class))).thenReturn(null);
        when(workspaceRepository.findByIdForUpdate("ws-1")).thenReturn(Optional.of(testWorkspace));
        when(taskRepository.findMaxOrderByColumnId("col-1")).thenReturn(0);
        when(taskRepository.save(any(Task.class))).thenAnswer(invocation -> invocation.getArgument(0));

        TaskCreateRequest request = TaskCreateRequest.builder()
                .title("Test Task")
                .columnId("col-1")
                .build();

        // Act
        boardService.createTask("ws-1", request, testUser);

        // Assert
        verify(workspaceRepository).findByIdForUpdate("ws-1");
        verifyNoMoreInteractions(workspaceRepository);
    }

    @Test
    void createTask_givenWorkspaceNotFound_throwsEntityNotFoundException() {
        // Arrange
        when(authorizationService.checkAccess(anyString(), any(User.class))).thenReturn(null);
        when(workspaceRepository.findByIdForUpdate("ws-1")).thenReturn(Optional.empty());

        TaskCreateRequest request = TaskCreateRequest.builder()
                .title("Test Task")
                .columnId("col-1")
                .build();

        // Act & Assert
        assertThatThrownBy(() -> boardService.createTask("ws-1", request, testUser))
                .isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    void createTask_givenNoPriority_setsDefaultPriorityMedium() {
        // Arrange
        when(authorizationService.checkAccess(anyString(), any(User.class))).thenReturn(null);
        when(workspaceRepository.findByIdForUpdate("ws-1")).thenReturn(Optional.of(testWorkspace));
        when(taskRepository.findMaxOrderByColumnId("col-1")).thenReturn(0);
        when(taskRepository.save(any(Task.class))).thenAnswer(invocation -> invocation.getArgument(0));

        TaskCreateRequest request = TaskCreateRequest.builder()
                .title("Test Task")
                .columnId("col-1")
                .build();

        // Act
        boardService.createTask("ws-1", request, testUser);

        // Assert
        Task savedTask = argCapture(taskRepository).getValue();
        assertThat(savedTask.getPriority()).isEqualTo(Priority.MEDIUM);
    }

    @Test
    void createTask_givenAssigneeIdAndDueDate_setsThemOnTask() {
        // Arrange
        when(authorizationService.checkAccess(anyString(), any(User.class))).thenReturn(null);
        when(workspaceRepository.findByIdForUpdate("ws-1")).thenReturn(Optional.of(testWorkspace));
        when(taskRepository.findMaxOrderByColumnId("col-1")).thenReturn(0);
        when(taskRepository.save(any(Task.class))).thenAnswer(invocation -> invocation.getArgument(0));

        TaskCreateRequest request = TaskCreateRequest.builder()
                .title("Test Task")
                .columnId("col-1")
                .assigneeId("user-2")
                .dueDate(LocalDate.of(2026, 9, 30))
                .build();

        // Act
        boardService.createTask("ws-1", request, testUser);

        // Assert
        Task savedTask = argCapture(taskRepository).getValue();
        assertThat(savedTask.getAssigneeId()).isEqualTo("user-2");
        assertThat(savedTask.getDueDate()).isEqualTo(LocalDate.of(2026, 9, 30));
    }

    @Test
    void updateTask_givenTask_exists_doesNotChangePublicId() {
        // Arrange
        Task existingTask = Task.builder()
                .id("task-1")
                .workspaceId("ws-1")
                .columnId("col-1")
                .title("Old Title")
                .publicId("TC-6")
                .build();

        when(authorizationService.checkAccess(anyString(), any(User.class))).thenReturn(null);
        when(taskRepository.findById("task-1")).thenReturn(Optional.of(existingTask));
        when(taskRepository.save(any(Task.class))).thenAnswer(invocation -> invocation.getArgument(0));

        TaskUpdateRequest request = TaskUpdateRequest.builder()
                .title("New Title")
                .build();

        // Act
        boardService.updateTask("task-1", request, testUser);

        // Assert
        Task savedTask = argCapture(taskRepository).getValue();
        assertThat(savedTask.getPublicId()).isEqualTo("TC-6");
    }

    // Helper method to capture the argument passed to a mock method
    private <T> org.mockito.ArgumentCaptor<T> argCapture(org.mockito.Mockito mock) {
        return org.mockito.ArgumentCaptor.forClass((Class<T>) mock);
    }
}