package com.taskcenter.service;

import com.taskcenter.dto.DependencyItemDto;
import com.taskcenter.dto.TaskDependenciesDto;
import com.taskcenter.exception.BadRequestException;
import com.taskcenter.exception.ResourceNotFoundException;
import com.taskcenter.model.BoardColumn;
import com.taskcenter.model.Task;
import com.taskcenter.model.TaskDependency;
import com.taskcenter.model.User;
import com.taskcenter.repository.ColumnRepository;
import com.taskcenter.repository.TaskDependencyRepository;
import com.taskcenter.repository.TaskRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
public class TaskDependencyService {

    private final TaskDependencyRepository taskDependencyRepository;
    private final TaskRepository taskRepository;
    private final ColumnRepository columnRepository;
    private final WorkspaceAuthorizationService authorizationService;
    private final TelegramNotificationService telegramNotificationService;

    public TaskDependencyService(
            TaskDependencyRepository taskDependencyRepository,
            TaskRepository taskRepository,
            ColumnRepository columnRepository,
            WorkspaceAuthorizationService authorizationService,
            @Lazy TelegramNotificationService telegramNotificationService) {
        this.taskDependencyRepository = taskDependencyRepository;
        this.taskRepository = taskRepository;
        this.columnRepository = columnRepository;
        this.authorizationService = authorizationService;
        this.telegramNotificationService = telegramNotificationService;
    }

    @Transactional(readOnly = true)
    public List<Task> getUnfinishedBlockers(Task task) {
        if (task == null || task.getId() == null) {
            return Collections.emptyList();
        }
        return taskRepository.findUnfinishedBlockers(task.getId());
    }

    @Transactional(readOnly = true)
    public void assertCanMoveToDone(Task task) {
        List<Task> blockers = getUnfinishedBlockers(task);
        if (!blockers.isEmpty()) {
            String names = blockers.stream()
                    .map(b -> b.getPublicId() != null ? b.getPublicId() : b.getTitle())
                    .collect(Collectors.joining(", "));
            throw new BadRequestException("Vazifani tugatish mumkin emas. Avval ushbu bog'liq vazifalar yakunlanishi kerak: " + names);
        }
    }

    @Transactional
    public void notifyUnblockedTasksOnDone(Task task, User currentUser) {
        if (task == null || task.getId() == null) {
            return;
        }
        List<TaskDependency> downstream = taskDependencyRepository.findByDependsOnTaskId(task.getId());
        for (TaskDependency dep : downstream) {
            Optional<Task> targetOpt = taskRepository.findById(dep.getTaskId());
            if (targetOpt.isEmpty() || targetOpt.get().getDeletedAt() != null) {
                continue;
            }
            Task target = targetOpt.get();
            List<Task> remaining = getUnfinishedBlockers(target);
            if (remaining.isEmpty()) {
                log.info("Task {} is now unblocked because blocker {} was completed", target.getPublicId(), task.getPublicId());
                if (telegramNotificationService != null && target.getAssignees() != null) {
                    for (User assignee : target.getAssignees()) {
                        if (assignee != null && assignee.getTelegramChatId() != null) {
                            try {
                                String msg = "🔓 <b>Vazifa blokdan chiqdi!</b>\n\n[" + target.getPublicId() + "] " + target.getTitle()
                                        + " vazifasi endi bajarishga tayyor (Blocker [" + task.getPublicId() + "] yakunlandi).";
                                telegramNotificationService.sendMessage(assignee.getTelegramChatId(), msg);
                            } catch (Exception ignored) {}
                        }
                    }
                }
            }
        }
    }

    @Transactional(readOnly = true)
    public TaskDependenciesDto getDependencies(String taskId, User currentUser) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new ResourceNotFoundException("Task topilmadi: " + taskId));
        authorizationService.checkAccess(task.getWorkspaceId(), currentUser);

        List<TaskDependency> blockedByDeps = taskDependencyRepository.findByTaskId(taskId);
        List<TaskDependency> blockingDeps = taskDependencyRepository.findByDependsOnTaskId(taskId);

        Set<String> relatedIds = new HashSet<>();
        blockedByDeps.forEach(dep -> relatedIds.add(dep.getDependsOnTaskId()));
        blockingDeps.forEach(dep -> relatedIds.add(dep.getTaskId()));
        Map<String, Task> tasksById = taskRepository.findAllById(relatedIds).stream()
                .collect(Collectors.toMap(Task::getId, t -> t));
        Set<String> blockerColumns = blockedByDeps.stream().map(dep -> tasksById.get(dep.getDependsOnTaskId()))
                .filter(Objects::nonNull).map(Task::getColumnId).collect(Collectors.toSet());
        Set<String> doneColumns = columnRepository.findAllById(blockerColumns).stream()
                .filter(col -> Boolean.TRUE.equals(col.getIsDone())).map(BoardColumn::getId).collect(Collectors.toSet());

        List<DependencyItemDto> blockedBy = new ArrayList<>();
        boolean isBlocked = false;

        for (TaskDependency dep : blockedByDeps) {
            Task blocker = tasksById.get(dep.getDependsOnTaskId());
            if (blocker == null) continue;
            boolean done = doneColumns.contains(blocker.getColumnId());
            if (!done) isBlocked = true;

            blockedBy.add(DependencyItemDto.builder()
                    .id(dep.getId())
                    .blockerTaskId(blocker.getId())
                    .blockerPublicId(blocker.getPublicId())
                    .blockerTitle(blocker.getTitle())
                    .blockerCompleted(done)
                    .blockedTaskId(task.getId())
                    .blockedPublicId(task.getPublicId())
                    .blockedTitle(task.getTitle())
                    .build());
        }

        List<DependencyItemDto> blocking = new ArrayList<>();
        for (TaskDependency dep : blockingDeps) {
            Task target = tasksById.get(dep.getTaskId());
            if (target == null) continue;

            blocking.add(DependencyItemDto.builder()
                    .id(dep.getId())
                    .blockerTaskId(task.getId())
                    .blockerPublicId(task.getPublicId())
                    .blockerTitle(task.getTitle())
                    .blockedTaskId(target.getId())
                    .blockedPublicId(target.getPublicId())
                    .blockedTitle(target.getTitle())
                    .build());
        }

        return TaskDependenciesDto.builder()
                .blocked(isBlocked)
                .blockedBy(blockedBy)
                .blocking(blocking)
                .build();
    }

    @Transactional
    public DependencyItemDto addDependency(String taskId, String blockerTaskIdOrCode, User currentUser) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new ResourceNotFoundException("Task topilmadi: " + taskId));
        authorizationService.checkCanEdit(task.getWorkspaceId(), currentUser);

        String lookup = blockerTaskIdOrCode.trim();
        Task blocker = taskRepository.findById(lookup)
                .or(() -> taskRepository.findByPublicId(lookup))
                .or(() -> taskRepository.findByPublicId(lookup.toUpperCase()))
                .orElseThrow(() -> new ResourceNotFoundException("Blocker vazifa topilmadi: " + lookup));

        if (blocker.getId().equals(task.getId())) {
            throw new BadRequestException("Vazifa o'z-o'ziga bog'liq bo'la olmaydi");
        }
        if (!blocker.getWorkspaceId().equals(task.getWorkspaceId())) {
            throw new BadRequestException("Bog'liq vazifa ayni ishchi maydonda bo'lishi kerak");
        }

        // Check if inverse dependency already exists (prevent 1-hop circular dependency)
        if (taskDependencyRepository.findByTaskIdAndDependsOnTaskId(blocker.getId(), task.getId()).isPresent()) {
            throw new BadRequestException("Aylanma bog'liqlik (Circular dependency) yaratish taqiqlanadi");
        }

        TaskDependency dep = taskDependencyRepository.findByTaskIdAndDependsOnTaskId(task.getId(), blocker.getId())
                .orElseGet(() -> taskDependencyRepository.save(TaskDependency.builder()
                        .taskId(task.getId())
                        .dependsOnTaskId(blocker.getId())
                        .build()));

        BoardColumn col = columnRepository.findById(blocker.getColumnId()).orElse(null);
        boolean done = col != null && Boolean.TRUE.equals(col.getIsDone());

        return DependencyItemDto.builder()
                .id(dep.getId())
                .blockerTaskId(blocker.getId())
                .blockerPublicId(blocker.getPublicId())
                .blockerTitle(blocker.getTitle())
                .blockerCompleted(done)
                .blockedTaskId(task.getId())
                .blockedPublicId(task.getPublicId())
                .blockedTitle(task.getTitle())
                .build();
    }

    @Transactional
    public void removeDependency(String taskId, String dependencyId, User currentUser) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new ResourceNotFoundException("Task topilmadi: " + taskId));
        authorizationService.checkCanEdit(task.getWorkspaceId(), currentUser);

        TaskDependency dep = taskDependencyRepository.findById(dependencyId)
                .orElseThrow(() -> new ResourceNotFoundException("Bog'liqlik topilmadi: " + dependencyId));

        if (!dep.getTaskId().equals(taskId) && !dep.getDependsOnTaskId().equals(taskId)) {
            throw new BadRequestException("Bog'liqlik ko'rsatilgan vazifaga tegishli emas");
        }

        taskDependencyRepository.delete(dep);
    }
}
