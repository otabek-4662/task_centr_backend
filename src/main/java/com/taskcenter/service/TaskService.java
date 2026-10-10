package com.taskcenter.service;

import com.taskcenter.dto.*;
import com.taskcenter.exception.BadRequestException;
import com.taskcenter.exception.ResourceNotFoundException;
import com.taskcenter.exception.ValidationException;
import com.taskcenter.model.BoardColumn;
import com.taskcenter.model.IssueType;
import com.taskcenter.model.Priority;
import com.taskcenter.model.Task;
import com.taskcenter.model.TaskActivityType;
import com.taskcenter.model.User;
import com.taskcenter.model.Workspace;
import com.taskcenter.model.WorkspaceRole;
import com.taskcenter.repository.ColumnRepository;
import com.taskcenter.repository.TaskRepository;
import com.taskcenter.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
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
    private final com.taskcenter.repository.DirectionRepository directionRepository;
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
             userRepository, null, null, clock);
    }

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
        this(taskRepository, columnRepository, sprintRepository, authorizationService,
             activityService, webSocketNotifier, notificationService,
             telegramReminderLogRepository, telegramNotificationService,
             userRepository, labelRepository, null, clock);
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
                       com.taskcenter.repository.DirectionRepository directionRepository,
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
        this.directionRepository = directionRepository;
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
    public List<com.taskcenter.dto.BoardInitColumnDto> getBoardInit(String workspaceId, String sprintId, User currentUser) {
        authorizationService.checkAccess(workspaceId, currentUser);
        
        String effectiveSprintId = sprintId;
        if (effectiveSprintId == null) {
            java.util.Optional<com.taskcenter.model.Sprint> activeOpt = sprintRepository.findByWorkspaceIdAndStatus(workspaceId, com.taskcenter.model.SprintStatus.ACTIVE);
            if (activeOpt.isEmpty()) {
                List<BoardColumn> cols = columnRepository.findByWorkspaceIdOrderByOrderAsc(workspaceId);
                return cols.stream()
                        .map(c -> com.taskcenter.dto.BoardInitColumnDto.fromEntity(c, java.util.Collections.emptyList()))
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
            return com.taskcenter.dto.BoardInitColumnDto.fromEntity(c, columnTasks);
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
    public com.taskcenter.dto.TaskDetailsDto getTaskDetails(String id, User currentUser) {
        Task task = taskRepository.findByIdWithDetails(id)
                .or(() -> taskRepository.findById(id))
                .orElseThrow(() -> new ResourceNotFoundException("Task topilmadi: " + id));
        authorizationService.checkAccess(task.getWorkspaceId(), currentUser);
        
        com.taskcenter.dto.TaskDto taskDto = TaskDto.fromEntity(task);
        // We can fetch comments and checklists from task if they are mapped, or just return empty for now if not injected.
        // Actually, checklists are inside Task model!
        List<com.taskcenter.dto.ChecklistItemDto> checklists = task.getChecklistItems() != null 
                ? task.getChecklistItems().stream().map(com.taskcenter.dto.ChecklistItemDto::fromEntity).collect(Collectors.toList()) 
                : List.of();
        // comments aren't eagerly loaded in task, but let's assume they are empty or we can just fetch if we had commentService.
        // To strictly avoid duplicate logic and coupling without CommentService, we just build it.
        return com.taskcenter.dto.TaskDetailsDto.builder()
                .task(taskDto)
                .checklists(checklists)
                .comments(List.of()) // Comment fetching can be done by client via existing /comments endpoint, or we can add it later
                .build();
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

    private Set<com.taskcenter.model.Direction> resolveDirections(String workspaceId, Set<String> directionIds, Set<String> directionNamesOrIds) {
        Set<com.taskcenter.model.Direction> result = new HashSet<>();
        if (directionRepository == null) {
            return result;
        }

        if (directionIds != null && !directionIds.isEmpty()) {
            Set<String> ids = directionIds.stream().filter(Objects::nonNull).collect(Collectors.toSet());
            if (!ids.isEmpty()) {
                result.addAll(directionRepository.findAllById(ids));
            }
        }

        if (directionNamesOrIds != null && !directionNamesOrIds.isEmpty()) {
            for (String item : directionNamesOrIds) {
                if (item == null || item.isBlank()) {
                    continue;
                }
                String trimmed = item.trim();

                boolean alreadyResolved = result.stream()
                        .anyMatch(d -> (d.getId() != null && d.getId().equals(trimmed)) ||
                                       (d.getName() != null && d.getName().equalsIgnoreCase(trimmed)));
                if (alreadyResolved) {
                    continue;
                }

                Optional<com.taskcenter.model.Direction> byId = directionRepository.findByIdAndWorkspaceId(trimmed, workspaceId);
                if (byId != null && byId.isPresent()) {
                    result.add(byId.get());
                    continue;
                }

                Optional<com.taskcenter.model.Direction> byName = directionRepository.findByWorkspaceIdAndNameIgnoreCase(workspaceId, trimmed);
                if (byName != null && byName.isPresent()) {
                    result.add(byName.get());
                    continue;
                }

                com.taskcenter.model.Direction newDir = com.taskcenter.model.Direction.builder()
                        .workspaceId(workspaceId)
                        .name(trimmed)
                        .color("#3B82F6")
                        .build();
                try {
                    com.taskcenter.model.Direction saved = directionRepository.saveAndFlush(newDir);
                    result.add(saved != null ? saved : newDir);
                } catch (Exception e) {
                    Optional<com.taskcenter.model.Direction> fallback = directionRepository.findByWorkspaceIdAndNameIgnoreCase(workspaceId, trimmed);
                    if (fallback != null && fallback.isPresent()) {
                        result.add(fallback.get());
                    } else {
                        result.add(newDir);
                    }
                }
            }
        }
        return result;
    }

    private Set<User> resolveAssignees(Set<String> assigneeIds, Set<String> usersOrIdentifiers) {
        Set<User> result = new HashSet<>();
        if (userRepository == null) {
            return result;
        }

        if (assigneeIds != null && !assigneeIds.isEmpty()) {
            Set<String> ids = assigneeIds.stream().filter(Objects::nonNull).collect(Collectors.toSet());
            if (!ids.isEmpty()) {
                result.addAll(userRepository.findAllById(ids));
            }
        }

        if (usersOrIdentifiers != null && !usersOrIdentifiers.isEmpty()) {
            for (String item : usersOrIdentifiers) {
                if (item == null || item.isBlank()) {
                    continue;
                }
                String trimmed = item.trim();
                if (trimmed.startsWith("@")) {
                    trimmed = trimmed.substring(1).trim();
                }

                String identifier = trimmed;
                boolean alreadyResolved = result.stream()
                        .anyMatch(u -> (u.getId() != null && u.getId().equals(identifier)) ||
                                       (u.getName() != null && u.getName().equalsIgnoreCase(identifier)) ||
                                       (u.getEmail() != null && u.getEmail().equalsIgnoreCase(identifier)));
                if (alreadyResolved) {
                    continue;
                }

                Optional<User> byId = userRepository.findById(identifier);
                if (byId != null && byId.isPresent()) {
                    result.add(byId.get());
                    continue;
                }

                Optional<User> byNameOrEmail = userRepository.findByNameOrEmail(identifier);
                if (byNameOrEmail != null && byNameOrEmail.isPresent()) {
                    result.add(byNameOrEmail.get());
                    continue;
                }
            }
        }
        return result;
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
                .sprintId(sprintId)
                .build();

        Set<User> assignees = resolveAssignees(req.getAssigneeIds(), req.getUsers());
        if (!assignees.isEmpty()) {
            task.getAssignees().addAll(assignees);
        }

        Set<com.taskcenter.model.Direction> directions = resolveDirections(workspaceId, req.getDirectionIds(), req.getDirection());
        if (!directions.isEmpty()) {
            task.getDirections().addAll(directions);
        }

        if (req.getLabelIds() != null && !req.getLabelIds().isEmpty() && labelRepository != null) {
            Set<String> targetLabelIds = req.getLabelIds().stream().filter(Objects::nonNull).collect(Collectors.toSet());
            if (!targetLabelIds.isEmpty()) {
                List<com.taskcenter.model.Label> labels = labelRepository.findAllById(targetLabelIds);
                task.getLabels().addAll(labels);
            }
        }

        Task saved = taskRepository.save(task);
        if (task.getAssignees() != null) {
            for (User assignee : task.getAssignees()) {
                telegramNotificationService.sendTaskAssignedNotification(saved, assignee, currentUser);
            }
        }
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
    public TaskResponseV2 createTaskV2(String workspaceId, TaskCreateRequestV2 req, User currentUser) {
        Workspace workspace = authorizationService.checkCanEdit(workspaceId, currentUser);

        // Unknown columnId -> 404 NOT_FOUND
        columnRepository.findByIdAndWorkspaceId(req.columnId(), workspaceId)
                .orElseThrow(() -> new ResourceNotFoundException("Ustun topilmadi: " + req.columnId()));

        // Validation: every id in users must be a member of the workspace, otherwise 400 VALIDATION_ERROR
        if (req.users() != null && !req.users().isEmpty()) {
            for (String userId : req.users()) {
                if (userId == null || userId.isBlank()) {
                    throw new ValidationException(
                            "Foydalanuvchi ID si bo'sh bo'lishi mumkin emas",
                            Map.of("users", "Foydalanuvchi ID si bo'sh bo'lishi mumkin emas")
                    );
                }
                Optional<WorkspaceRole> roleOpt = authorizationService.getRole(workspace, userId.trim());
                if (roleOpt.isEmpty()) {
                    throw new ValidationException(
                            "Foydalanuvchi ushbu workspace a'zosi emas: " + userId,
                            Map.of("users", "Foydalanuvchi ushbu workspace a'zosi emas: " + userId)
                    );
                }
            }
        }

        // Reuse existing createTask via mapper (users -> assigneeIds, direction -> existing directions mechanism)
        TaskCreateRequest v1Req = new TaskCreateRequest();
        v1Req.setColumnId(req.columnId());
        v1Req.setTitle(req.title());
        v1Req.setDescription(req.description());
        v1Req.setPriority(req.priority());
        v1Req.setIssueType(req.issueType());
        v1Req.setDueDate(req.dueDate());
        v1Req.setStoryPoints(req.storyPoints());
        if (req.direction() != null) {
            v1Req.setDirection(new HashSet<>(req.direction()));
        }
        if (req.users() != null) {
            v1Req.setAssigneeIds(new HashSet<>(req.users()));
        }

        TaskDto createdDto = this.createTask(workspaceId, v1Req, currentUser);

        List<String> directionNames = createdDto.getDirections() != null
                ? createdDto.getDirections().stream().map(DirectionDto::getName).filter(Objects::nonNull).toList()
                : List.of();

        List<TaskUserDto> taskUsers = new ArrayList<>();
        if (createdDto.getAssignees() != null) {
            for (UserDto assignee : createdDto.getAssignees()) {
                WorkspaceRole wsRole = authorizationService.getRole(workspace, assignee.getId()).orElse(null);
                taskUsers.add(new TaskUserDto(
                        assignee.getId(),
                        assignee.getName(),
                        assignee.getFullName() != null ? assignee.getFullName() : assignee.getName(),
                        assignee.getEmail(),
                        wsRole,
                        wsRole != null ? wsRole.getDisplayName() : null
                ));
            }
        }

        return new TaskResponseV2(
                createdDto.getId(),
                createdDto.getPublicId(),
                createdDto.getColumnId(),
                createdDto.getTitle(),
                createdDto.getDescription(),
                createdDto.getPriority(),
                createdDto.getIssueType(),
                createdDto.getDueDate(),
                createdDto.getStoryPoints(),
                directionNames,
                taskUsers,
                createdDto.getCreatedAt()
        );
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
        if (req.getDueDate() != null && !req.getDueDate().equals(task.getDueDate())) {
            LocalDate oldDueDate = task.getDueDate();
            task.setDueDate(req.getDueDate());
            activityService.logActivity(task.getId(), currentUser, TaskActivityType.DUE_DATE_UPDATED, "dueDate", String.valueOf(oldDueDate), String.valueOf(req.getDueDate()));
        }
        if (req.getStoryPoints() != null && !req.getStoryPoints().equals(task.getStoryPoints())) {
            Integer oldPoints = task.getStoryPoints();
            task.setStoryPoints(req.getStoryPoints());
            activityService.logActivity(task.getId(), currentUser, TaskActivityType.STORY_POINTS_UPDATED, "storyPoints", String.valueOf(oldPoints), String.valueOf(req.getStoryPoints()));
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

        if ((req.getAssigneeIds() != null || req.getUsers() != null) && userRepository != null) {
            Set<User> targetUsers = resolveAssignees(req.getAssigneeIds(), req.getUsers());
            Set<String> targetIds = targetUsers.stream().map(User::getId).filter(Objects::nonNull).collect(Collectors.toSet());
            Set<User> existingAssignees = new HashSet<>(task.getAssignees());
            Set<String> existingIds = existingAssignees.stream().map(User::getId).filter(Objects::nonNull).collect(Collectors.toSet());

            Set<User> toAdd = targetUsers.stream().filter(u -> !existingIds.contains(u.getId())).collect(Collectors.toSet());
            Set<String> toRemoveIds = existingIds.stream().filter(uid -> !targetIds.contains(uid)).collect(Collectors.toSet());

            if (!toAdd.isEmpty() || !toRemoveIds.isEmpty()) {
                task.getAssignees().removeIf(u -> toRemoveIds.contains(u.getId()));
                for (User newUser : toAdd) {
                    task.getAssignees().add(newUser);
                    telegramNotificationService.sendTaskAssignedNotification(task, newUser, currentUser);
                    activityService.logActivity(task.getId(), currentUser, TaskActivityType.ASSIGNEE_ADDED, "assignee", null, newUser.getFullName() != null ? newUser.getFullName() : newUser.getName());
                }
                for (String removedId : toRemoveIds) {
                    activityService.logActivity(task.getId(), currentUser, TaskActivityType.ASSIGNEE_REMOVED, "assignee", removedId, null);
                }
            }
        }

        if ((req.getDirectionIds() != null || req.getDirection() != null) && directionRepository != null) {
            Set<com.taskcenter.model.Direction> targetDirs = resolveDirections(workspaceId, req.getDirectionIds(), req.getDirection());
            Set<String> targetDirIds = targetDirs.stream().map(com.taskcenter.model.Direction::getId).filter(Objects::nonNull).collect(Collectors.toSet());
            Set<String> existingDirIds = task.getDirections().stream().map(com.taskcenter.model.Direction::getId).filter(Objects::nonNull).collect(Collectors.toSet());

            if (!targetDirIds.equals(existingDirIds)) {
                task.getDirections().removeIf(d -> d.getId() != null && !targetDirIds.contains(d.getId()));
                task.getDirections().addAll(targetDirs);
            }
        }

        if (req.getLabelIds() != null && labelRepository != null) {
            Set<String> targetLabelIds = req.getLabelIds().stream().filter(Objects::nonNull).collect(Collectors.toSet());
            Set<com.taskcenter.model.Label> existingLabels = new HashSet<>(task.getLabels());
            Set<String> existingLabelIds = existingLabels.stream().map(com.taskcenter.model.Label::getId).collect(Collectors.toSet());

            if (!targetLabelIds.equals(existingLabelIds)) {
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

