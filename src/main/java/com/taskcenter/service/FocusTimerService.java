package com.taskcenter.service;

import com.taskcenter.dto.FocusTimerDto;
import com.taskcenter.exception.BadRequestException;
import com.taskcenter.exception.ResourceNotFoundException;
import com.taskcenter.model.FocusTimer;
import com.taskcenter.model.Task;
import com.taskcenter.model.TaskWorklog;
import com.taskcenter.model.User;
import com.taskcenter.repository.FocusTimerRepository;
import com.taskcenter.repository.TaskRepository;
import com.taskcenter.repository.TaskWorklogRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
public class FocusTimerService {

    private final FocusTimerRepository focusTimerRepository;
    private final TaskRepository taskRepository;
    private final WorkspaceAuthorizationService authorizationService;
    private final Clock clock;

    private TaskWorklogRepository taskWorklogRepository;

    public FocusTimerService(
            FocusTimerRepository focusTimerRepository,
            TaskRepository taskRepository,
            WorkspaceAuthorizationService authorizationService,
            Clock clock) {
        this.focusTimerRepository = focusTimerRepository;
        this.taskRepository = taskRepository;
        this.authorizationService = authorizationService;
        this.clock = clock;
    }

    @Autowired(required = false)
    public void setTaskWorklogRepository(TaskWorklogRepository taskWorklogRepository) {
        this.taskWorklogRepository = taskWorklogRepository;
    }

    @Transactional(readOnly = true)
    public Optional<FocusTimerDto> getActiveTimer(User user) {
        if (user == null) return Optional.empty();
        return focusTimerRepository.findByUserIdAndIsActiveTrue(user.getId())
                .map(this::toDto);
    }

    @Transactional
    public FocusTimerDto startTimer(String taskId, Integer targetMinutes, User user) {
        if (user == null) {
            throw new BadRequestException("Foydalanuvchi aniqlanmadi");
        }

        // Deactivate any existing active timers for this user
        focusTimerRepository.findByUserIdAndIsActiveTrue(user.getId()).ifPresent(active -> {
            active.setIsActive(false);
            active.setEndedAt(LocalDateTime.now(clock));
            focusTimerRepository.save(active);
        });

        Task task = null;
        if (taskId != null && !taskId.isBlank()) {
            task = taskRepository.findById(taskId)
                    .orElseThrow(() -> new ResourceNotFoundException("Task topilmadi: " + taskId));
            authorizationService.checkAccess(task.getWorkspaceId(), user);
        }

        int target = (targetMinutes != null && targetMinutes > 0) ? targetMinutes : 25;

        FocusTimer timer = FocusTimer.builder()
                .id(UUID.randomUUID().toString())
                .userId(user.getId())
                .taskId(task != null ? task.getId() : null)
                .targetMinutes(target)
                .startedAt(LocalDateTime.now(clock))
                .isActive(true)
                .loggedHoursAdded(0.0)
                .build();

        FocusTimer saved = focusTimerRepository.save(timer);
        return toDto(saved, task);
    }

    @Transactional
    public FocusTimerDto stopTimer(User user) {
        if (user == null) {
            throw new BadRequestException("Foydalanuvchi aniqlanmadi");
        }

        FocusTimer timer = focusTimerRepository.findByUserIdAndIsActiveTrue(user.getId())
                .orElseThrow(() -> new BadRequestException("Sizda faol Pomodoro taymeri mavjud emas"));

        LocalDateTime now = LocalDateTime.now(clock);
        long elapsedSec = Math.max(0, Duration.between(timer.getStartedAt(), now).getSeconds());
        double hours = Math.round((elapsedSec / 3600.0) * 100.0) / 100.0;

        Task task = null;
        if (timer.getTaskId() != null) {
            Optional<Task> taskOpt = taskRepository.findById(timer.getTaskId());
            if (taskOpt.isPresent()) {
                task = taskOpt.get();
                if (hours > 0.0) {
                    task.setLoggedHours((task.getLoggedHours() != null ? task.getLoggedHours() : 0.0) + hours);
                    taskRepository.save(task);

                    if (taskWorklogRepository != null) {
                        TaskWorklog worklog = TaskWorklog.builder()
                                .id(UUID.randomUUID().toString())
                                .taskId(task.getId())
                                .workspaceId(task.getWorkspaceId())
                                .userId(user.getId())
                                .hours(hours)
                                .description("Pomodoro fokus seansi (" + Math.round(elapsedSec / 60.0) + " daqiqa)")
                                .workDate(LocalDate.now(clock))
                                .createdAt(now)
                                .build();
                        taskWorklogRepository.save(worklog);
                    }
                }
            }
        }

        timer.setIsActive(false);
        timer.setEndedAt(now);
        timer.setLoggedHoursAdded(hours);

        FocusTimer saved = focusTimerRepository.save(timer);
        return toDto(saved, task);
    }

    private FocusTimerDto toDto(FocusTimer timer) {
        Task task = null;
        if (timer.getTaskId() != null) {
            task = taskRepository.findById(timer.getTaskId()).orElse(null);
        }
        return toDto(timer, task);
    }

    private FocusTimerDto toDto(FocusTimer timer, Task task) {
        LocalDateTime now = LocalDateTime.now(clock);
        long elapsed = timer.getEndedAt() != null
                ? Duration.between(timer.getStartedAt(), timer.getEndedAt()).getSeconds()
                : Duration.between(timer.getStartedAt(), now).getSeconds();

        return FocusTimerDto.builder()
                .id(timer.getId())
                .taskId(timer.getTaskId())
                .taskPublicId(task != null ? task.getPublicId() : null)
                .taskTitle(task != null ? task.getTitle() : null)
                .targetMinutes(timer.getTargetMinutes())
                .elapsedSeconds(Math.max(0, elapsed))
                .loggedHoursAdded(timer.getLoggedHoursAdded())
                .startedAt(timer.getStartedAt())
                .endedAt(timer.getEndedAt())
                .isActive(timer.getIsActive())
                .build();
    }
}
