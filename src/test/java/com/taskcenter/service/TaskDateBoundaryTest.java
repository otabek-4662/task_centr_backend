package com.taskcenter.service;

import com.taskcenter.dto.TaskFilterRequest;
import com.taskcenter.model.User;
import com.taskcenter.repository.TaskRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TaskDateBoundaryTest {

    @Mock
    private TaskRepository taskRepository;
    
    @Mock
    private WorkspaceAuthorizationService authorizationService;

    @Test
    void testServiceFiltersWithTashkentTime() {
        // UTC 2026-10-01 20:00:00 is exactly 2026-10-02 01:00:00 in Asia/Tashkent
        Instant utc20 = Instant.parse("2026-10-01T20:00:00Z");
        Clock clock = Clock.fixed(utc20, ZoneId.of("Asia/Tashkent"));
        
        TaskService taskService = new TaskService(
                taskRepository, null, null, authorizationService, null, null, null, null, null, null, clock
        );
        
        when(taskRepository.findIdsByWorkspaceIdFiltered(any(), any(), any())).thenReturn(Page.empty());
        when(taskRepository.findByIdIn(any())).thenReturn(java.util.Collections.emptyList());
        
        User testUser = User.builder().id("user1").build();
        TaskFilterRequest filter = new TaskFilterRequest();
        
        taskService.getTasksByWorkspace("ws-1", testUser, filter, Pageable.unpaged());
        
        ArgumentCaptor<TaskFilterRequest> captor = ArgumentCaptor.forClass(TaskFilterRequest.class);
        verify(taskRepository).findIdsByWorkspaceIdFiltered(eq("ws-1"), captor.capture(), any());
        
        LocalDate today = captor.getValue().getToday();
        assertThat(today).isEqualTo(LocalDate.of(2026, 10, 2));
    }
}
