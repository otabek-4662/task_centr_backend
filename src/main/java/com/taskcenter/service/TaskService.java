package com.taskcenter.service;

import com.taskcenter.dto.*;
import com.taskcenter.exception.BadRequestException;
import com.taskcenter.exception.ResourceNotFoundException;
import com.taskcenter.model.BoardColumn;
import com.taskcenter.model.IssueType;
import com.taskcenter.model.Priority;
import com.taskcenter.model.Task;
import com.taskcenter.model.TaskActivityType;
import com.taskcenter.model.User;
import com.taskcenter.repository.ColumnRepository;
import com.taskcenter.repository.TaskRepository;
import com.taskcenter.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class TaskService {

    private final TaskRepository taskRepository;
    private final ColumnRepository columnRepository;
    private final com.taskcenter.repository.SprintRepository sprintRepository;
    private final WorkspaceAuthorizationService authorizationService;
    private final TaskActivityService activityService;
    private final WebSocketNotifier webSocketNotifier;
    private final NotificationService notificationService;
    private final com.taskcenter.repository.TelegramReminderLogRepository telegramReminderLogRepository;
    private final TelegramNotificationService telegramNotificationService;
    private final UserRepository userRepository;
    private final com.taskcenter.repository.LabelRepository labelRepository;
    private final java.time.Clock clock;

    public TaskService(TaskRepository taskRepository,
                       ColumnRepository columnRepository,
                       com.taskcenter.repository.SprintRepository sprintRepository,
                       WorkspaceAuthorizationService authorizationService,
                       TaskActivityService activityService,
                       WebSocketNotifier webSocketNotifier,
                       NotificationService notificationService,
                       com.taskcenter.repository.TelegramReminderLogRepository telegramReminderLogRepository,
                       TelegramNotificationService telegramNotificationService,
                       UserRepository userRepository,
                       java.time.Clock clock) {
        this(taskRepository, columnRepository, sprintRepository, authorizationService,
             activityService, webSocketNotifier, notificationService,
             telegramReminderLogRepository, telegramNotificationService,
             userRepository, null, clock);
    }

    @org.springframework.beans.factory.annotation.Autowired
    public TaskService(TaskRepository taskRepository,
                       ColumnRepository columnRepository,
                       com.taskcenter.repository.SprintRepository sprintRepository,
                       WorkspaceAuthorizationService authorizationService,
                       TaskActivityService activityService,
                       WebSocketNotifier webSocketNotifier,
                       NotificationService notificationService,
                       com.taskcenter.repository.TelegramReminderLogRepository telegramReminderLogRepository,
                       TelegramNotificationService telegramNotificationService,
                       UserRepository userRepository,
                       com.taskcenter.repository.LabelRepository labelRepository,
                       java.time.Clock clock) {
        this.taskRepository = taskRepository;
        this.columnRepository = columnRepository;
        this.sprintRepository = sprintRepository;
        this.authorizationService = authorizationService;
        this.activityService = activityService;
        this.webSocketNotifier = webSocketNotifier;
        this.notificationService = notificationService;
        this.telegramReminderLogRepository = telegramReminderLogRepository;
        this.telegramNotificationService = telegramNotificationService;
        this.userRepository = userRepository;
        this.labelRepository = labelRepository;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public List<ColumnWithCardsDto> getBoard(String workspaceId, String sprintId, User currentUser) {
        authorizationService.checkAccess(workspaceId, currentUser);
        
        String effectiveSprintId = sprintId;
        if (effectiveSprintId == null) {
            java.util.Optional<com.taskcenter.model.Sprint> activeOpt = sprintRepository.findByWorkspaceIdAndStatus(workspaceId, com.taskcenter.model.SprintStatus.ACTIVE);
            if (activeOpt.isEmpty()) {
                List<BoardColumn> cols = columnRepository.findByWorkspaceIdOrderByOrderAsc(workspaceId);
                return cols.stream()
                        .map(c -> ColumnWithCardsDto.fromEntity(c, java.util.Collections.emptyList()))
                        .collect(Collectors.toList());
            }
            effectiveSprintId = activeOpt.get().getId();
        }
        
        final String targetSprintId = effectiveSprintId;
        
        List<BoardColumn> cols = columnRepository.findByWorkspaceIdOrderByOrderAsc(workspaceId);
        List<Task> allSprintTasks = taskRepository.findBySprintId(targetSprintId);

        return cols.stream().map(c -> {
            List<Task> columnTasks = allSprintTasks.stream()
                    .filter(t -> c.getId().equals(t.getColumnId()) && Boolean.FALSE.equals(t.getIsArchived()))
                    .sorted(Comparator.comparing(Task::getLexoRank))
                    .collect(Collectors.toList());
            return ColumnWithCardsDto.fromEntity(c, columnTasks);
        }).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ColumnWithCardsDto> getBoard(String workspaceId, User currentUser) {
        return getBoard(workspaceId, null, currentUser);
    }

    @Transactional(readOnly = true)
    public Page<TaskDto> getTasksByWorkspace(String workspaceId, User currentUser, TaskFilterRequest filter, Pageable pageable) {
        authorizationService.checkAccess(workspaceId, currentUser);
        if (filter == null) {
            filter = new TaskFilterRequest();
        }
        filter.setCurrentUserId(currentUser.getId());
        String query = filter.getSearch();
        if (query == null || query.isBlank()) {
            query = filter.getQ();
        }
        if (query != null && !query.isBlank()) {
            filter.setSearch(query.trim());
        } else {
            filter.setSearch(null);
        }
        if (Boolean.TRUE.equals(filter.getDueThisWeek())) {
            filter.setEndOfWeek(LocalDate.now(clock).with(java.time.temporal.TemporalAdjusters.nextOrSame(java.time.DayOfWeek.SUNDAY)));
        }
        filter.setToday(LocalDate.now(clock));
        Page<String> idPage = taskRepository.findIdsByWorkspaceIdFiltered(workspaceId, filter, pageable);
        List<Task> tasks = taskRepository.findByIdIn(idPage.getContent());
        java.util.Map<String, Task> taskMap = tasks.stream().collect(Collectors.toMap(Task::getId, t -> t));
        List<TaskDto> dtos = idPage.getContent().stream()
                .map(taskMap::get)
                .filter(java.util.Objects::nonNull)
                .map(TaskDto::fromEntity)
                .collect(Collectors.toList());
        return new org.springframework.data.domain.PageImpl<>(dtos, pageable, idPage.getTotalElements());
    }

    public Page<TaskDto> getMyTasks(User currentUser, com.taskcenter.dto.TaskFilterRequest filter, Pageable pageable) {
        if (currentUser == null) {
            throw new com.taskcenter.exception.ForbiddenException("Foydalanuvchi aniqlanmadi");
        }
        if (filter == null) {
            filter = new com.taskcenter.dto.TaskFilterRequest();
        }
        if (filter.getQ() != null && !filter.getQ().isBlank() && (filter.getSearch() == null || filter.getSearch().isBlank())) {
            filter.setSearch(filter.getQ());
        }
        if (filter.getSearch() != null && filter.getSearch().trim().isEmpty()) {
            filter.setSearch(null);
        }
        if (Boolean.TRUE.equals(filter.getDueThisWeek())) {
            filter.setEndOfWeek(LocalDate.now(clock).with(java.time.temporal.TemporalAdjusters.nextOrSame(java.time.DayOfWeek.SUNDAY)));
        }
        filter.setToday(LocalDate.now(clock));
        filter.setCurrentUserId(currentUser.getId());

        Page<String> idPage = taskRepository.findAssignedTaskIdsByUserIdFiltered(currentUser.getId(), filter, pageable);
        if (idPage.isEmpty()) {
            return new org.springframework.data.domain.PageImpl<>(List.of(), pageable, idPage.getTotalElements());
        }
        List<Task> tasks = taskRepository.findByIdIn(idPage.getContent());
        java.util.Map<String, Task> taskMap = tasks.stream().collect(Collectors.toMap(Task::getId, t -> t));
        List<TaskDto> dtos = idPage.getContent().stream()
                .map(taskMap::get)
                .filter(java.util.Objects::nonNull)
                .map(TaskDto::fromEntity)
                .collect(Collectors.toList());
        return new org.springframework.data.domain.PageImpl<>(dtos, pageable, idPage.getTotalElements());
    }

    private Task getTaskAndValidateWorkspace(String id, String workspaceId) {
        Optional<Task> taskOpt = taskRepository.findByIdAndWorkspaceIdWithDetails(id, workspaceId);
        if (taskOpt.isPresent()) {
            return taskOpt.get();
        }
        Optional<Task> fallback = taskRepository.findByIdWithDetails(id)
                .or(() -> taskRepository.findById(id));
        if (fallback.isPresent()) {
            if (!workspaceId.equals(fallback.get().getWorkspaceId())) {
                throw new ResourceNotFoundException("Task ushbu workspace ga tegishli emas: " + id);
            }
            return fallback.get();
        }
        throw new ResourceNotFoundException("Task topilmadi: " + id);
    }

    private Task getTaskSimpleAndValidateWorkspace(String id, String workspaceId) {
        Optional<Task> taskOpt = taskRepository.findByIdAndWorkspaceId(id, workspaceId);
        if (taskOpt.isPresent()) {
            return taskOpt.get();
        }
        Optional<Task> fallback = taskRepository.findByIdWithDetails(id)
                .or(() -> taskRepository.findById(id));
        if (fallback.isPresent()) {
            if (!workspaceId.equals(fallback.get().getWorkspaceId())) {
                throw new ResourceNotFoundException("Task ushbu workspace ga tegishli emas: " + id);
            }
            return fallback.get();
        }
        throw new ResourceNotFoundException("Task topilmadi: " + id);
    }

    private BoardColumn getColumnAndValidateWorkspace(String columnId, String workspaceId) {
        Optional<BoardColumn> colOpt = columnRepository.findByIdAndWorkspaceId(columnId, workspaceId);
        if (colOpt.isPresent()) {
            return colOpt.get();
        }
        Optional<BoardColumn> fallback = columnRepository.findById(columnId);
        if (fallback.isPresent()) {
            if (!workspaceId.equals(fallback.get().getWorkspaceId())) {
                throw new ResourceNotFoundException("Column ushbu workspace ga tegishli emas: " + columnId);
            }
            return fallback.get();
        }
        throw new ResourceNotFoundException("Column topilmadi: " + columnId);
    }

    @Transactional(readOnly = true)
    public TaskDto getTaskById(String workspaceId, String id, User currentUser) {
        authorizationService.checkAccess(workspaceId, currentUser);
        Task task = getTaskAndValidateWorkspace(id, workspaceId);

        return TaskDto.fromEntity(task);
    }

    @Transactional(readOnly = true)
    public TaskDto getTaskDirectById(String id, User currentUser) {
        Task task = taskRepository.findByIdWithDetails(id).orElse(null);
        if (task == null) {
            throw new ResourceNotFoundException("Task topilmadi yoki huquq yo'q");
        }
        try {
            authorizationService.checkAccess(task.getWorkspaceId(), currentUser);
        } catch (Exception e) {
            throw new ResourceNotFoundException("Task topilmadi yoki huquq yo'q");
        }
        return TaskDto.fromEntity(task);
    }

    @Transactional
    public TaskDto createTask(String workspaceId, TaskCreateRequest req, User currentUser) {
        authorizationService.checkCanEdit(workspaceId, currentUser);

        BoardColumn column = getColumnAndValidateWorkspace(req.getColumnId(), workspaceId);

        String lexoRank = req.getLexoRank();
        if (lexoRank == null || lexoRank.isBlank()) {
            String maxRank = taskRepository.findMaxLexoRankByColumnId(req.getColumnId());
            lexoRank = com.taskcenter.util.LexoRankUtil.getMiddle(maxRank, null);
        }

        Priority priority = req.getPriority() != null ? req.getPriority() : Priority.MEDIUM;
        IssueType issueType = req.getIssueType() != null ? req.getIssueType() : IssueType.TASK;

        String sprintId = null;
        if (req.getSprintId() != null && !req.getSprintId().isBlank()) {
            com.taskcenter.model.Sprint sprint = sprintRepository.findById(req.getSprintId())
                    .orElseThrow(() -> new ResourceNotFoundException("Sprint topilmadi: " + req.getSprintId()));
            if (!workspaceId.equals(sprint.getWorkspaceId())) {
                throw new BadRequestException("Sprint ushbu workspace ga tegishli emas");
            }
            if (sprint.getStatus() == com.taskcenter.model.SprintStatus.COMPLETED) {
                throw new BadRequestException("Yopilgan sprintga yangi vazifa biriktirib bo'lmaydi");
            }
            sprintId = sprint.getId();
        }

        Task task = Task.builder()
                .workspaceId(workspaceId)
                .columnId(req.getColumnId())
                .title(req.getTitle())
                .description(req.getDescription())
                .lexoRank(lexoRank)
                .priority(priority)
                .issueType(issueType)
                .dueDate(req.getDueDate())
                .storyPoints(req.getStoryPoints())
                .estimatedHours(req.getEstimatedHours())
                .loggedHours(req.getLoggedHours())
                .sprintId(sprintId)
                .build();

        Task saved = taskRepository.save(task);
        activityService.logActivity(saved.getId(), currentUser, TaskActivityType.TASK_CREATED, "task", null, saved.getTitle());
        TaskDto taskDto = TaskDto.fromEntity(saved);
        webSocketNotifier.notifyWorkspace(workspaceId, WebSocketEvent.builder()
                .type("TASK_CREATED")
                .workspaceId(workspaceId)
                .data(taskDto)
                .build());
        return taskDto;
    }

    @Transactional
    public TaskDto updateTask(String workspaceId, String id, TaskUpdateRequest req, User currentUser) {
        authorizationService.checkCanEdit(workspaceId, currentUser);

        Task task = getTaskAndValidateWorkspace(id, workspaceId);

        boolean columnChanged = false;
        String oldColumnTitle = null;
        String newColumnTitle = null;
        if (req.getColumnId() != null && !req.getColumnId().equals(task.getColumnId())) {
            BoardColumn newColumn = getColumnAndValidateWorkspace(req.getColumnId(), workspaceId);
            String oldColId = task.getColumnId();
            BoardColumn oldColumn = columnRepository.findById(oldColId).orElse(null);
            oldColumnTitle = oldColumn != null ? oldColumn.getTitle() : oldColId;
            newColumnTitle = newColumn.getTitle();
            columnChanged = true;
            task.setColumnId(req.getColumnId());
            if (req.getLexoRank() == null) {
                String maxRank = taskRepository.findMaxLexoRankByColumnId(req.getColumnId());
                task.setLexoRank(com.taskcenter.util.LexoRankUtil.getMiddle(maxRank, null));
            }
            activityService.logActivity(task.getId(), currentUser, TaskActivityType.STATUS_UPDATED, "columnId", oldColId, req.getColumnId());
        }

        if (req.getTitle() != null && !req.getTitle().equals(task.getTitle())) {
            String oldTitle = task.getTitle();
            task.setTitle(req.getTitle());
            activityService.logActivity(task.getId(), currentUser, TaskActivityType.TITLE_UPDATED, "title", oldTitle, req.getTitle());
        }
        if (req.getDescription() != null && !req.getDescription().equals(task.getDescription())) {
            String oldDesc = task.getDescription();
            task.setDescription(req.getDescription());
            activityService.logActivity(task.getId(), currentUser, TaskActivityType.DESCRIPTION_UPDATED, "description", oldDesc, req.getDescription());
        }
        if (req.getLexoRank() != null) {
            task.setLexoRank(req.getLexoRank());
        }
        if (req.getPriority() != null && req.getPriority() != task.getPriority()) {
            Priority oldPriority = task.getPriority();
            task.setPriority(req.getPriority());
            activityService.logActivity(task.getId(), currentUser, TaskActivityType.PRIORITY_UPDATED, "priority", String.valueOf(oldPriority), String.valueOf(req.getPriority()));
        }
        if (req.getIssueType() != null && req.getIssueType() != task.getIssueType()) {
            IssueType oldType = task.getIssueType();
            task.setIssueType(req.getIssueType());
            activityService.logActivity(task.getId(), currentUser, TaskActivityType.ISSUE_TYPE_UPDATED, "issueType", String.valueOf(oldType), String.valueOf(req.getIssueType()));
        }
        if (Boolean.TRUE.equals(req.getClearDueDate())) {
            LocalDate oldDate = task.getDueDate();
            task.setDueDate(null);
            activityService.logActivity(task.getId(), currentUser, TaskActivityType.DUE_DATE_UPDATED, "dueDate", String.valueOf(oldDate), "null");
            clearReminderLogsForTask(task.getId());
        } else if (req.getDueDate() != null && !req.getDueDate().equals(task.getDueDate())) {
            LocalDate oldDate = task.getDueDate();
            task.setDueDate(req.getDueDate());
            activityService.logActivity(task.getId(), currentUser, TaskActivityType.DUE_DATE_UPDATED, "dueDate", String.valueOf(oldDate), String.valueOf(req.getDueDate()));
            clearReminderLogsForTask(task.getId());
        }
        if (req.getStoryPoints() != null && !req.getStoryPoints().equals(task.getStoryPoints())) {
            Integer oldPoints = task.getStoryPoints();
            task.setStoryPoints(req.getStoryPoints());
            activityService.logActivity(task.getId(), currentUser, TaskActivityType.STORY_POINTS_UPDATED, "storyPoints", String.valueOf(oldPoints), String.valueOf(req.getStoryPoints()));
        }
        if (req.getEstimatedHours() != null && !req.getEstimatedHours().equals(task.getEstimatedHours())) {
            task.setEstimatedHours(req.getEstimatedHours());
        }
        if (req.getLoggedHours() != null && !req.getLoggedHours().equals(task.getLoggedHours())) {
            task.setLoggedHours(req.getLoggedHours());
        }
        if (req.getSprintId() != null) {
            String newSprintId = req.getSprintId().isBlank() ? null : req.getSprintId();
            if (newSprintId != null && !newSprintId.equals(task.getSprintId())) {
                com.taskcenter.model.Sprint sprint = sprintRepository.findById(newSprintId)
                        .orElseThrow(() -> new ResourceNotFoundException("Sprint topilmadi: " + newSprintId));
                if (!workspaceId.equals(sprint.getWorkspaceId())) {
                    throw new BadRequestException("Sprint ushbu workspace ga tegishli emas");
                }
                if (sprint.getStatus() == com.taskcenter.model.SprintStatus.COMPLETED) {
                    throw new BadRequestException("Yopilgan sprintga vazifani ko'chirib bo'lmaydi");
                }
                String oldSprintId = task.getSprintId();
                task.setSprintId(newSprintId);
                activityService.logActivity(task.getId(), currentUser, TaskActivityType.SPRINT_ASSIGNED, "sprint", oldSprintId, sprint.getName());
            } else if (newSprintId == null && task.getSprintId() != null) {
                task.setSprintId(null);
                activityService.logActivity(task.getId(), currentUser, TaskActivityType.SPRINT_REMOVED, "sprint", null, "Backlog");
            }
        }

        if (req.getAssigneeIds() != null && userRepository != null) {
            java.util.Set<String> targetIds = req.getAssigneeIds().stream()
                    .filter(java.util.Objects::nonNull)
                    .collect(Collectors.toSet());
            java.util.Set<User> existingAssignees = new java.util.HashSet<>(task.getAssignees());
            java.util.Set<String> existingIds = existingAssignees.stream().map(User::getId).collect(Collectors.toSet());

            java.util.Set<String> toAddIds = targetIds.stream().filter(uid -> !existingIds.contains(uid)).collect(Collectors.toSet());
            java.util.Set<String> toRemoveIds = existingIds.stream().filter(uid -> !targetIds.contains(uid)).collect(Collectors.toSet());

            if (!toAddIds.isEmpty() || !toRemoveIds.isEmpty()) {
                task.getAssignees().removeIf(u -> toRemoveIds.contains(u.getId()));
                if (!toAddIds.isEmpty()) {
                    List<User> newUsers = userRepository.findAllById(toAddIds);
                    for (User newUser : newUsers) {
                        task.getAssignees().add(newUser);
                        telegramNotificationService.sendTaskAssignedNotification(task, newUser, currentUser);
                        activityService.logActivity(task.getId(), currentUser, TaskActivityType.ASSIGNEE_ADDED, "assignee", null, newUser.getFullName() != null ? newUser.getFullName() : newUser.getName());
                    }
                }
                for (String removedId : toRemoveIds) {
                    activityService.logActivity(task.getId(), currentUser, TaskActivityType.ASSIGNEE_REMOVED, "assignee", removedId, null);
                }
            }
        }

        if (req.getLabelIds() != null && labelRepository != null) {
            java.util.Set<String> targetLabelIds = req.getLabelIds().stream()
                    .filter(java.util.Objects::nonNull)
                    .collect(Collectors.toSet());
            java.util.Set<com.taskcenter.model.Label> existingLabels = new java.util.HashSet<>(task.getLabels());
            java.util.Set<String> existingIds = existingLabels.stream().map(com.taskcenter.model.Label::getId).collect(Collectors.toSet());

            if (!targetLabelIds.equals(existingIds)) {
                task.getLabels().removeIf(l -> !targetLabelIds.contains(l.getId()));
                if (!targetLabelIds.isEmpty()) {
                    List<com.taskcenter.model.Label> newLabels = labelRepository.findAllById(targetLabelIds);
                    task.getLabels().addAll(newLabels);
                }
            }
        }

        Task saved = taskRepository.save(task);
        if (columnChanged) {
            telegramNotificationService.sendTaskMovedNotification(saved, currentUser, oldColumnTitle, newColumnTitle);
        }
        notificationService.notifyWatchers(task.getWatchers(), currentUser.getId(), "Vazifa yangilandi", task.getTitle() + " vazifasi o'zgartirildi", task.getId());
        TaskDto taskDto = TaskDto.fromEntity(saved);
        webSocketNotifier.notifyWorkspace(workspaceId, WebSocketEvent.builder()
                .type("TASK_UPDATED")
                .workspaceId(workspaceId)
                .data(taskDto)
                .build());
        return taskDto;
    }

    @Transactional
    public TaskDto reorderTask(String workspaceId, String id, TaskReorderRequest req, User currentUser) {
        authorizationService.checkCanEdit(workspaceId, currentUser);

        Task task = getTaskAndValidateWorkspace(id, workspaceId);

        boolean columnChanged = false;
        String oldColumnTitle = null;
        String newColumnTitle = null;
        if (req.getColumnId() != null && !req.getColumnId().equals(task.getColumnId())) {
            BoardColumn column = getColumnAndValidateWorkspace(req.getColumnId(), workspaceId);
            String oldColId = task.getColumnId();
            BoardColumn oldColumn = columnRepository.findById(oldColId).orElse(null);
            oldColumnTitle = oldColumn != null ? oldColumn.getTitle() : oldColId;
            newColumnTitle = column.getTitle();
            columnChanged = true;
            task.setColumnId(req.getColumnId());
            activityService.logActivity(task.getId(), currentUser, TaskActivityType.STATUS_UPDATED, "columnId", oldColId, req.getColumnId());
        }

        task.setLexoRank(com.taskcenter.util.LexoRankUtil.getMiddle(req.getPrevRank(), req.getNextRank()));
        
        Task saved = taskRepository.save(task);
        if (columnChanged) {
            telegramNotificationService.sendTaskMovedNotification(saved, currentUser, oldColumnTitle, newColumnTitle);
        }
        TaskDto taskDto = TaskDto.fromEntity(saved);
        
        webSocketNotifier.notifyWorkspace(workspaceId, WebSocketEvent.builder()
                .type("TASK_MOVED")
                .workspaceId(workspaceId)
                .data(taskDto)
                .build());
                
        return taskDto;
    }

    @Transactional
    public void deleteTask(String workspaceId, String id, User currentUser) {
        authorizationService.checkCanEdit(workspaceId, currentUser);

        Task task = getTaskSimpleAndValidateWorkspace(id, workspaceId);

        taskRepository.deleteById(id);
        
        webSocketNotifier.notifyWorkspace(workspaceId, WebSocketEvent.builder()
                .type("TASK_DELETED")
                .workspaceId(workspaceId)
                .data(java.util.Map.of("taskId", id, "columnId", task.getColumnId()))
                .build());
    }

    @Transactional
    public TaskDto cloneTask(String workspaceId, String id, User currentUser) {
        authorizationService.checkCanEdit(workspaceId, currentUser);

        Task originalTask = getTaskAndValidateWorkspace(id, workspaceId);

        String maxRank = taskRepository.findMaxLexoRankByColumnId(originalTask.getColumnId());
        String newRank = com.taskcenter.util.LexoRankUtil.getMiddle(maxRank, null);

        String prefix = "(Copy) ";
        String newTitle = prefix + originalTask.getTitle();
        if (newTitle.length() > 255) {
            newTitle = prefix + originalTask.getTitle().substring(0, 255 - prefix.length());
        }

        Task clonedTask = Task.builder()
                .workspaceId(originalTask.getWorkspaceId())
                .columnId(originalTask.getColumnId())
                .title(newTitle)
                .description(originalTask.getDescription())
                .lexoRank(newRank)
                .priority(originalTask.getPriority())
                .issueType(originalTask.getIssueType())
                .dueDate(originalTask.getDueDate())
                .storyPoints(originalTask.getStoryPoints())
                .estimatedHours(originalTask.getEstimatedHours())
                .sprintId(originalTask.getSprintId())
                .build();
        
        clonedTask.prePersist();

        if (originalTask.getLabels() != null) {
            clonedTask.getLabels().addAll(originalTask.getLabels());
        }

        if (originalTask.getChecklistItems() != null) {
            for (com.taskcenter.model.TaskChecklistItem item : originalTask.getChecklistItems()) {
                com.taskcenter.model.TaskChecklistItem clonedItem = com.taskcenter.model.TaskChecklistItem.builder()
                        .taskId(clonedTask.getId())
                        .title(item.getTitle())
                        .isCompleted(item.getIsCompleted())
                        .orderIndex(item.getOrderIndex())
                        .build();
                clonedTask.getChecklistItems().add(clonedItem);
            }
        }

        Task saved = taskRepository.save(clonedTask);
        activityService.logActivity(saved.getId(), currentUser, TaskActivityType.TASK_CREATED, "task", null, saved.getTitle());
        
        TaskDto taskDto = TaskDto.fromEntity(saved);
        webSocketNotifier.notifyWorkspace(workspaceId, WebSocketEvent.builder()
                .type("TASK_CREATED")
                .workspaceId(workspaceId)
                .data(taskDto)
                .build());
        return taskDto;
    }

    @Transactional
    public void toggleWatch(String workspaceId, String id, User currentUser) {
        authorizationService.checkAccess(workspaceId, currentUser);

        Task task = getTaskAndValidateWorkspace(id, workspaceId);

        boolean isWatching = task.getWatchers().stream().anyMatch(u -> u.getId().equals(currentUser.getId()));
        if (isWatching) {
            task.getWatchers().removeIf(u -> u.getId().equals(currentUser.getId()));
        } else {
            task.getWatchers().add(currentUser);
        }
        
        taskRepository.save(task);
    }

    @Transactional
    public TaskDto toggleArchive(String workspaceId, String id, boolean archive, User currentUser) {
        authorizationService.checkCanEdit(workspaceId, currentUser);

        Task task = getTaskAndValidateWorkspace(id, workspaceId);

        task.setIsArchived(archive);
        Task saved = taskRepository.save(task);
        
        TaskDto taskDto = TaskDto.fromEntity(saved);
        
        webSocketNotifier.notifyWorkspace(workspaceId, WebSocketEvent.builder()
                .type("TASK_UPDATED")
                .workspaceId(workspaceId)
                .data(taskDto)
                .build());
                
        return taskDto;
    }

    @Transactional
    public TaskDto toggleAssignee(String workspaceId, String id, String userId, User currentUser) {
        authorizationService.checkCanEdit(workspaceId, currentUser);

        Task task = getTaskAndValidateWorkspace(id, workspaceId);

        User assignee = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Foydalanuvchi topilmadi"));

        boolean isAssigned = task.getAssignees().stream().anyMatch(u -> u.getId().equals(userId));
        boolean newlyAssigned = false;
        if (isAssigned) {
            task.getAssignees().removeIf(u -> u.getId().equals(userId));
        } else {
            task.getAssignees().add(assignee);
            newlyAssigned = true;
        }
        
        Task saved = taskRepository.save(task);

        if (newlyAssigned) {
            telegramNotificationService.sendTaskAssignedNotification(saved, assignee, currentUser);
        }
        TaskDto taskDto = TaskDto.fromEntity(saved);
        
        webSocketNotifier.notifyWorkspace(workspaceId, WebSocketEvent.builder()
                .type("TASK_UPDATED")
                .workspaceId(workspaceId)
                .data(taskDto)
                .build());
                
        return taskDto;
    }

    @Transactional
    public void clearReminderLogsForTask(String taskId) {
        if (taskId != null) {
            telegramReminderLogRepository.deleteByTaskId(taskId);
        }
    }
}

