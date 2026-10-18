package com.taskcenter.service;

import com.taskcenter.model.BoardColumn;
import com.taskcenter.model.RecurrenceRule;
import com.taskcenter.model.Task;
import com.taskcenter.repository.ColumnRepository;
import com.taskcenter.repository.TaskRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
public class RecurringTaskService {

    private final TaskRepository taskRepository;
    private final ColumnRepository columnRepository;

    public RecurringTaskService(TaskRepository taskRepository, ColumnRepository columnRepository) {
        this.taskRepository = taskRepository;
        this.columnRepository = columnRepository;
    }

    public static LocalDate computeNextOccurrence(LocalDate baseDate, RecurrenceRule rule, Integer interval) {
        if (rule == null || rule == RecurrenceRule.NONE) {
            return null;
        }
        int step = (interval != null && interval > 0) ? interval : 1;
        LocalDate d = (baseDate != null) ? baseDate : com.taskcenter.util.AppTime.today();

        return switch (rule) {
            case DAILY -> d.plusDays(step);
            case WEEKDAYS -> {
                LocalDate cur = d;
                for (int i = 0; i < step; i++) {
                    cur = cur.plusDays(1);
                    while (cur.getDayOfWeek() == DayOfWeek.SATURDAY || cur.getDayOfWeek() == DayOfWeek.SUNDAY) {
                        cur = cur.plusDays(1);
                    }
                }
                yield cur;
            }
            case WEEKLY -> d.plusWeeks(step);
            case MONTHLY -> d.plusMonths(step);
            case NONE -> null;
        };
    }

    @Transactional
    public void spawnNextTaskWhenCompleted(Task completedTask) {
        if (completedTask == null || completedTask.getRecurrenceRule() == null || completedTask.getRecurrenceRule() == RecurrenceRule.NONE) {
            return;
        }

        LocalDate nextDueDate = completedTask.getNextOccurrenceDate() != null
                ? completedTask.getNextOccurrenceDate()
                : computeNextOccurrence(completedTask.getDueDate(), completedTask.getRecurrenceRule(), completedTask.getRecurrenceInterval());

        if (nextDueDate == null) {
            return;
        }

        LocalDate futureOccurrence = computeNextOccurrence(nextDueDate, completedTask.getRecurrenceRule(), completedTask.getRecurrenceInterval());

        // Find initial (non-done) column for the workspace
        List<BoardColumn> columns = columnRepository.findByWorkspaceIdOrderByOrderAsc(completedTask.getWorkspaceId());
        String initialColId = columns.stream()
                .filter(c -> !Boolean.TRUE.equals(c.getIsDone()))
                .map(BoardColumn::getId)
                .findFirst()
                .orElse(columns.isEmpty() ? completedTask.getColumnId() : columns.get(0).getId());

        Task nextTask = Task.builder()
                .id(UUID.randomUUID().toString())
                .workspaceId(completedTask.getWorkspaceId())
                .columnId(initialColId)
                .title(completedTask.getTitle())
                .description(completedTask.getDescription())
                .priority(completedTask.getPriority())
                .issueType(completedTask.getIssueType())
                .storyPoints(completedTask.getStoryPoints())
                .estimatedHours(completedTask.getEstimatedHours())
                .loggedHours(0.0)
                .dueDate(nextDueDate)
                .recurrenceRule(completedTask.getRecurrenceRule())
                .recurrenceInterval(completedTask.getRecurrenceInterval())
                .nextOccurrenceDate(futureOccurrence)
                .isArchived(false)
                .assignees(completedTask.getAssignees() != null ? new HashSet<>(completedTask.getAssignees()) : new HashSet<>())
                .labels(completedTask.getLabels() != null ? new HashSet<>(completedTask.getLabels()) : new HashSet<>())
                .directions(completedTask.getDirections() != null ? new HashSet<>(completedTask.getDirections()) : new HashSet<>())
                .build();

        taskRepository.save(nextTask);
        log.info("Spawned next recurring task for '{}', next due date: {}", completedTask.getTitle(), nextDueDate);
    }
}
