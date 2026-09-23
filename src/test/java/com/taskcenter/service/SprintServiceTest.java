package com.taskcenter.service;

import com.taskcenter.dto.*;
import com.taskcenter.exception.BadRequestException;
import com.taskcenter.exception.ResourceNotFoundException;
import com.taskcenter.model.*;
import com.taskcenter.repository.ColumnRepository;
import com.taskcenter.repository.SprintRepository;
import com.taskcenter.repository.TaskRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SprintServiceTest {

    @Mock
    private SprintRepository sprintRepository;
    @Mock
    private TaskRepository taskRepository;
    @Mock
    private ColumnRepository columnRepository;
    @Mock
    private WorkspaceAuthorizationService authorizationService;
    @Mock
    private TaskActivityService activityService;
    @Mock
    private WebSocketNotifier webSocketNotifier;

    @InjectMocks
    private SprintService sprintService;

    private User user;
    private Sprint testSprint;

    @BeforeEach
    void setUp() {
        user = User.builder().id("u1").name("bek").role(User.Role.USER).build();
        testSprint = Sprint.builder()
                .id("sprint1")
                .workspaceId("ws1")
                .name("Sprint 1")
                .goal("Goal 1")
                .status(SprintStatus.FUTURE)
                .startDate(LocalDate.now())
                .endDate(LocalDate.now().plusWeeks(2))
                .build();
    }

    @Test
    void getSprints_usesAggregatedQuery_noNPlusOne() {
        doNothing().when(authorizationService).checkAccess("ws1", user);
        when(sprintRepository.findByWorkspaceIdOrderByCreatedAtDesc("ws1")).thenReturn(List.of(testSprint));

        com.taskcenter.dto.SprintStatsProjection stats = mock(com.taskcenter.dto.SprintStatsProjection.class);
        when(stats.getSprintId()).thenReturn("sprint1");
        when(stats.getTaskCount()).thenReturn(3L);
        when(stats.getTotalStoryPoints()).thenReturn(13);
        when(taskRepository.findSprintStatsByWorkspaceId("ws1")).thenReturn(List.of(stats));

        List<SprintDto> result = sprintService.getSprints("ws1", null, user);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getTaskCount()).isEqualTo(3L);
        assertThat(result.get(0).getTotalStoryPoints()).isEqualTo(13);
        verify(taskRepository).findSprintStatsByWorkspaceId("ws1");
        verify(taskRepository, never()).countBySprintId(any());
    }

    @Test
    void createSprint_success() {
        doNothing().when(authorizationService).checkCanEdit("ws1", user);
        when(sprintRepository.save(any(Sprint.class))).thenAnswer(inv -> inv.getArgument(0));

        SprintCreateRequest req = new SprintCreateRequest();
        req.setName("Sprint Alpha");
        req.setGoal("Build MVP");
        req.setStartDate(LocalDate.now());
        req.setEndDate(LocalDate.now().plusWeeks(2));

        SprintDto result = sprintService.createSprint("ws1", req, user);

        assertThat(result.getName()).isEqualTo("Sprint Alpha");
        assertThat(result.getStatus()).isEqualTo(SprintStatus.FUTURE);
        assertThat(result.getTotalStoryPoints()).isEqualTo(0);
        verify(sprintRepository).save(any(Sprint.class));
    }

    @Test
    void createSprint_invalidDates_throws() {
        doNothing().when(authorizationService).checkCanEdit("ws1", user);

        SprintCreateRequest req = new SprintCreateRequest();
        req.setName("Sprint Bad Dates");
        req.setStartDate(LocalDate.now().plusDays(5));
        req.setEndDate(LocalDate.now().plusDays(2)); // endDate is before startDate

        assertThatThrownBy(() -> sprintService.createSprint("ws1", req, user))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Tugash sanasi");
    }

    @Test
    void startSprint_success() {
        doNothing().when(authorizationService).checkCanEdit("ws1", user);
        when(sprintRepository.findById("sprint1")).thenReturn(Optional.of(testSprint));
        when(sprintRepository.findByWorkspaceIdAndStatus("ws1", SprintStatus.ACTIVE)).thenReturn(Optional.empty());
        when(sprintRepository.save(any(Sprint.class))).thenAnswer(inv -> inv.getArgument(0));

        SprintDto result = sprintService.startSprint("sprint1", user);

        assertThat(result.getStatus()).isEqualTo(SprintStatus.ACTIVE);
        verify(sprintRepository).save(testSprint);
    }

    @Test
    void startSprint_alreadyActiveSprintExists_throws() {
        doNothing().when(authorizationService).checkCanEdit("ws1", user);
        when(sprintRepository.findById("sprint1")).thenReturn(Optional.of(testSprint));
        Sprint currentActive = Sprint.builder().id("sprint-active").name("Sprint 0").status(SprintStatus.ACTIVE).build();
        when(sprintRepository.findByWorkspaceIdAndStatus("ws1", SprintStatus.ACTIVE)).thenReturn(Optional.of(currentActive));

        assertThatThrownBy(() -> sprintService.startSprint("sprint1", user))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("allaqachon faol sprint mavjud");
    }

    @Test
    void completeSprint_movesUndoneTasksToBacklog() {
        testSprint.setStatus(SprintStatus.ACTIVE);
        doNothing().when(authorizationService).checkCanEdit("ws1", user);
        when(sprintRepository.findById("sprint1")).thenReturn(Optional.of(testSprint));

        BoardColumn colTodo = BoardColumn.builder().id("col-todo").title("To Do").order(1).build();
        BoardColumn colDone = BoardColumn.builder().id("col-done").title("Done").order(2).build();
        when(columnRepository.findByWorkspaceIdOrderByOrderAsc("ws1")).thenReturn(List.of(colTodo, colDone));

        Task taskDone = Task.builder().id("t1").title("Task 1").sprintId("sprint1").columnId("col-done").build();
        Task taskUndone = Task.builder().id("t2").title("Task 2").sprintId("sprint1").columnId("col-todo").build();
        when(taskRepository.findBySprintId("sprint1")).thenReturn(List.of(taskDone, taskUndone));
        when(sprintRepository.save(any(Sprint.class))).thenAnswer(inv -> inv.getArgument(0));

        SprintDto result = sprintService.completeSprint("sprint1", null, user);

        assertThat(result.getStatus()).isEqualTo(SprintStatus.CLOSED);
        assertThat(taskDone.getSprintId()).isEqualTo("sprint1"); // remains in sprint
        assertThat(taskUndone.getSprintId()).isNull(); // moved to backlog
        verify(taskRepository).saveAll(any());
    }

    @Test
    void addTasksToSprint_success() {
        doNothing().when(authorizationService).checkCanEdit("ws1", user);
        when(sprintRepository.findById("sprint1")).thenReturn(Optional.of(testSprint));

        Task t1 = Task.builder().id("task1").workspaceId("ws1").sprintId(null).build();
        when(taskRepository.findById("task1")).thenReturn(Optional.of(t1));
        when(taskRepository.findBySprintIdOrderByLexoRankAsc("sprint1")).thenReturn(List.of(t1));

        SprintTaskMoveRequest req = new SprintTaskMoveRequest();
        req.setTaskIds(List.of("task1"));

        List<TaskDto> result = sprintService.addTasksToSprint("sprint1", req, user);

        assertThat(t1.getSprintId()).isEqualTo("sprint1");
        verify(taskRepository).saveAll(any());
        verify(activityService).logActivity(eq("task1"), eq(user), eq(TaskActivityType.SPRINT_ASSIGNED), eq("sprint"), isNull(), eq("Sprint 1"));
    }

    @Test
    void removeTaskFromSprint_success() {
        doNothing().when(authorizationService).checkCanEdit("ws1", user);
        when(sprintRepository.findById("sprint1")).thenReturn(Optional.of(testSprint));

        Task t1 = Task.builder().id("task1").sprintId("sprint1").build();
        when(taskRepository.findById("task1")).thenReturn(Optional.of(t1));

        sprintService.removeTaskFromSprint("sprint1", "task1", user);

        assertThat(t1.getSprintId()).isNull();
        verify(taskRepository).save(t1);
        verify(activityService).logActivity("task1", user, TaskActivityType.SPRINT_REMOVED, "sprint", "Sprint 1", "Backlog");
    }

    @Test
    void getBacklog_calculatesPointsAndPaginates() {
        doNothing().when(authorizationService).checkAccess("ws1", user);

        Task bTask = Task.builder().id("bt1").title("Backlog task").storyPoints(5).build();
        Page<Task> page = new PageImpl<>(List.of(bTask));
        when(taskRepository.findBacklogTasks(eq("ws1"), any(Pageable.class))).thenReturn(page);
        when(taskRepository.sumStoryPointsInBacklog("ws1")).thenReturn(5);

        BacklogDto backlog = sprintService.getBacklog("ws1", 0, 20, user);

        assertThat(backlog.getTotalTasks()).isEqualTo(1);
        assertThat(backlog.getTotalStoryPoints()).isEqualTo(5);
        assertThat(backlog.getTasks()).hasSize(1);
        assertThat(backlog.getTasks().get(0).getTitle()).isEqualTo("Backlog task");
    }
}
