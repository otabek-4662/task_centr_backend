package com.taskcenter.service;

import com.taskcenter.dto.TaskActivityDto;
import com.taskcenter.model.Task;
import com.taskcenter.model.TaskActivity;
import com.taskcenter.model.TaskActivityType;
import com.taskcenter.model.User;
import com.taskcenter.repository.TaskActivityRepository;
import com.taskcenter.repository.TaskRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TaskActivityServiceTest {

    @Mock
    private TaskActivityRepository activityRepository;

    @Mock
    private TaskRepository taskRepository;

    @Mock
    private WorkspaceAuthorizationService authorizationService;

    @InjectMocks
    private TaskActivityService activityService;

    private final String taskId = "task-1";
    private final String workspaceId = "ws-1";
    private User user;
    private Task task;

    @BeforeEach
    void setUp() {
        user = User.builder().id("user-1").name("elshod").fullName("Elshod T").build();
        task = Task.builder().id(taskId).workspaceId(workspaceId).title("Test Task").build();
    }

    @Test
    @DisplayName("Activity muvaffaqiyatli loglanadi")
    void logActivity_savesCorrectly() {
        activityService.logActivity(taskId, user, TaskActivityType.TITLE_UPDATED, "title", "Old Title", "New Title");

        verify(activityRepository, times(1)).save(any(TaskActivity.class));
    }

    @Test
    @DisplayName("Task faoliyatlar tarixi sahifalab olinadi")
    void getActivities_returnsPage() {
        when(taskRepository.findById(taskId)).thenReturn(Optional.of(task));
        doNothing().when(authorizationService).checkAccess(workspaceId, user);

        TaskActivity activity = TaskActivity.builder()
                .id("act-1")
                .taskId(taskId)
                .userId(user.getId())
                .user(user)
                .actionType(TaskActivityType.STATUS_UPDATED)
                .fieldName("columnId")
                .oldValue("col-1")
                .newValue("col-2")
                .createdAt(LocalDateTime.now())
                .build();

        Pageable pageable = PageRequest.of(0, 10);
        Page<TaskActivity> page = new PageImpl<>(List.of(activity), pageable, 1);
        when(activityRepository.findByTaskIdOrderByCreatedAtDesc(taskId, pageable)).thenReturn(page);

        Page<TaskActivityDto> result = activityService.getActivities(taskId, pageable, user);

        assertThat(result.getContent()).hasSize(1);
        TaskActivityDto dto = result.getContent().get(0);
        assertThat(dto.getActionType()).isEqualTo(TaskActivityType.STATUS_UPDATED);
        assertThat(dto.getUserName()).isEqualTo("elshod");
        assertThat(dto.getOldValue()).isEqualTo("col-1");
        assertThat(dto.getNewValue()).isEqualTo("col-2");
    }
}
