package com.taskcenter.service;

import com.taskcenter.dto.TaskDto;
import com.taskcenter.dto.TaskFilterRequest;
import com.taskcenter.dto.TaskUpdateRequest;
import com.taskcenter.model.*;
import com.taskcenter.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GlobalTasksAndRelationalUpdateTest {

    @Mock
    private TaskRepository taskRepository;

    @Mock
    private ColumnRepository columnRepository;

    @Mock
    private SprintRepository sprintRepository;

    @Mock
    private WorkspaceAuthorizationService authorizationService;

    @Mock
    private TaskActivityService activityService;

    @Mock
    private WebSocketNotifier webSocketNotifier;

    @Mock
    private NotificationService notificationService;

    @Mock
    private TelegramReminderLogRepository telegramReminderLogRepository;

    @Mock
    private TelegramNotificationService telegramNotificationService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private LabelRepository labelRepository;

    private Clock clock;
    private TaskService taskService;

    @BeforeEach
    void setUp() {
        clock = Clock.fixed(Instant.parse("2026-10-06T12:00:00Z"), ZoneId.of("Asia/Tashkent"));
        taskService = new TaskService(
                taskRepository,
                columnRepository,
                sprintRepository,
                authorizationService,
                activityService,
                webSocketNotifier,
                notificationService,
                telegramReminderLogRepository,
                telegramNotificationService,
                userRepository,
                labelRepository,
                clock
        );
    }

    @Test
    void testGetMyTasks_Success() {
        User user = User.builder().id("user-1").name("bekmurod").build();
        Task task1 = Task.builder()
                .id("t-1")
                .publicId("WFM-111")
                .workspaceId("ws-1")
                .columnId("col-1")
                .title("Global Task 1")
                .assignees(new HashSet<>(List.of(user)))
                .build();

        Pageable pageable = PageRequest.of(0, 10);
        TaskFilterRequest filter = new TaskFilterRequest();

        when(taskRepository.findAssignedTaskIdsByUserIdFiltered(eq("user-1"), any(TaskFilterRequest.class), eq(pageable)))
                .thenReturn(new PageImpl<>(List.of("t-1"), pageable, 1));
        when(taskRepository.findByIdIn(List.of("t-1"))).thenReturn(List.of(task1));

        Page<TaskDto> result = taskService.getMyTasks(user, filter, pageable);

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals("Global Task 1", result.getContent().get(0).getTitle());
        verify(taskRepository).findAssignedTaskIdsByUserIdFiltered(eq("user-1"), any(TaskFilterRequest.class), eq(pageable));
    }

    @Test
    void testUpdateTask_WithRelationalAssigneesAndLabels() {
        User currentUser = User.builder().id("admin-1").name("admin").build();
        User oldAssignee = User.builder().id("user-old").name("old_user").build();
        User newAssignee = User.builder().id("user-new").name("new_user").build();

        Label oldLabel = Label.builder().id("lbl-1").name("Frontend").build();
        Label newLabel = Label.builder().id("lbl-2").name("Backend").build();

        Task task = Task.builder()
                .id("task-123")
                .workspaceId("ws-1")
                .columnId("col-1")
                .title("Original Title")
                .assignees(new HashSet<>(List.of(oldAssignee)))
                .labels(new HashSet<>(List.of(oldLabel)))
                .watchers(new HashSet<>())
                .build();

        when(taskRepository.findByIdAndWorkspaceIdWithDetails("task-123", "ws-1"))
                .thenReturn(Optional.of(task));
        when(userRepository.findAllById(anySet()))
                .thenReturn(List.of(newAssignee));
        when(labelRepository.findAllById(anySet()))
                .thenReturn(List.of(newLabel));
        when(taskRepository.save(any(Task.class))).thenAnswer(invocation -> invocation.getArgument(0));

        TaskUpdateRequest updateRequest = new TaskUpdateRequest();
        updateRequest.setTitle("Updated Title");
        updateRequest.setAssigneeIds(Set.of("user-new"));
        updateRequest.setLabelIds(Set.of("lbl-2"));

        TaskDto updatedDto = taskService.updateTask("ws-1", "task-123", updateRequest, currentUser);

        assertNotNull(updatedDto);
        assertEquals("Updated Title", updatedDto.getTitle());

        // Verify old assignee removed, new assignee added
        assertTrue(task.getAssignees().stream().anyMatch(u -> u.getId().equals("user-new")));
        assertFalse(task.getAssignees().stream().anyMatch(u -> u.getId().equals("user-old")));

        // Verify old label removed, new label added
        assertTrue(task.getLabels().stream().anyMatch(l -> l.getId().equals("lbl-2")));
        assertFalse(task.getLabels().stream().anyMatch(l -> l.getId().equals("lbl-1")));

        verify(telegramNotificationService).sendTaskAssignedNotification(task, newAssignee, currentUser);
        verify(webSocketNotifier).notifyWorkspace(eq("ws-1"), any());
    }
}
