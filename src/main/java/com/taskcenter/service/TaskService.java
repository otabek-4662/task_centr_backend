package com.taskcenter.service;

import com.taskcenter.dto.*;
import com.taskcenter.exception.BadRequestException;
import com.taskcenter.exception.ForbiddenException;
import com.taskcenter.exception.ResourceNotFoundException;
import com.taskcenter.exception.ValidationException;
import com.taskcenter.model.BoardColumn;
import com.taskcenter.model.Direction;
import com.taskcenter.model.IssueType;
import com.taskcenter.model.Label;
import com.taskcenter.model.Priority;
import com.taskcenter.model.Sprint;
import com.taskcenter.model.SprintStatus;
import com.taskcenter.model.Task;
import com.taskcenter.model.TaskActivityType;
import com.taskcenter.model.TaskWorklog;
import com.taskcenter.model.User;
import com.taskcenter.model.Workspace;
import com.taskcenter.model.WorkspaceRole;
import com.taskcenter.repository.ColumnRepository;
import com.taskcenter.repository.DirectionRepository;
import com.taskcenter.repository.LabelRepository;
import com.taskcenter.repository.SprintRepository;
import com.taskcenter.repository.TaskRepository;
import com.taskcenter.repository.TaskWorklogRepository;
import com.taskcenter.repository.TelegramReminderLogRepository;
import com.taskcenter.repository.UserRepository;
import com.taskcenter.util.LexoRankUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class TaskService {

    private static final Logger log = LoggerFactory.getLogger(TaskService.class);

    private final TaskRepository taskRepository;
    private final ColumnRepository columnRepository;
    private final SprintRepository sprintRepository;
    private final WorkspaceAuthorizationService authorizationService;
    private final TaskActivityService activityService;
    private final WebSocketNotifier webSocketNotifier;
    private final NotificationService notificationService;
    private final TelegramReminderLogRepository telegramReminderLogRepository;
    private final TelegramNotificationService telegramNotificationService;
    private final UserRepository userRepository;
    private final LabelRepository labelRepository;
    private final DirectionRepository directionRepository;
    private final Clock clock;

    private TaskWorklogRepository taskWorklogRepository;

    public TaskService(TaskRepository taskRepository,
                       ColumnRepository columnRepository,
                       SprintRepository sprintRepository,
                       WorkspaceAuthorizationService authorizationService,
                       TaskActivityService activityService,
                       WebSocketNotifier webSocketNotifier,
                       NotificationService notificationService,
                       TelegramReminderLogRepository telegramReminderLogRepository,
                       TelegramNotificationService telegramNotificationService,
                       UserRepository userRepository,
                       Clock clock) {
        this(taskRepository, columnRepository, sprintRepository, authorizationService,
             activityService, webSocketNotifier, notificationService,
             telegramReminderLogRepository, telegramNotificationService,
             userRepository, null, null, clock);
    }

    public TaskService(TaskRepository taskRepository,
                       ColumnRepository columnRepository,
                       SprintRepository sprintRepository,
                       WorkspaceAuthorizationService authorizationService,
                       TaskActivityService activityService,
                       WebSocketNotifier webSocketNotifier,
                       NotificationService notificationService,
                       TelegramReminderLogRepository telegramReminderLogRepository,
                       TelegramNotificationService telegramNotificationService,
                       UserRepository userRepository,
                       LabelRepository labelRepository,
                       Clock clock) {
        this(taskRepository, columnRepository, sprintRepository, authorizationService,
             activityService, webSocketNotifier, notificationService,
             telegramReminderLogRepository, telegramNotificationService,
             userRepository, labelRepository, null, clock);
    }

    @Autowired
    public TaskService(TaskRepository taskRepository,
                       ColumnRepository columnRepository,
                       SprintRepository sprintRepository,
                       WorkspaceAuthorizationService authorizationService,
                       TaskActivityService activityService,
                       WebSocketNotifier webSocketNotifier,
                       NotificationService notificationService,
                       TelegramReminderLogRepository telegramReminderLogRepository,
                       TelegramNotificationService telegramNotificationService,
                       UserRepository userRepository,
                       LabelRepository labelRepository,
                       DirectionRepository directionRepository,
                       Clock clock) {
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

    private com.taskcenter.repository.CommentRepository commentRepository;

    @Autowired(required = false)
    public void setTaskWorklogRepository(TaskWorklogRepository taskWorklogRepository) {
        this.taskWorklogRepository = taskWorklogRepository;
    }

    private TaskDependencyService taskDependencyService;
    private RecurringTaskService recurringTaskService;

    @Autowired(required = false)
    public void setCommentRepository(com.taskcenter.repository.CommentRepository commentRepository) {
        this.commentRepository = commentRepository;
    }

    @Autowired(required = false)
    public void setTaskDependencyService(TaskDependencyService taskDependencyService) {
        this.taskDependencyService = taskDependencyService;
    }

    @Autowired(required = false)
    public void setRecurringTaskService(RecurringTaskService recurringTaskService) {
        this.recurringTaskService = recurringTaskService;
    }

    @Transactional(readOnly = true)
    public List<ColumnWithCardsDto> getBoard(String workspaceId, String sprintId, User currentUser) {
        return getBoard(workspaceId, sprintId, null, currentUser);
    }

    @Transactional(readOnly = true)
    public List<ColumnWithCardsDto> getBoard(String workspaceId, String sprintId, Integer doneLimit, User currentUser) {
        authorizationService.checkAccess(workspaceId, currentUser);

        String effectiveSprintId = sprintId;
        if (effectiveSprintId == null) {
            Optional<Sprint> activeOpt = sprintRepository.findByWorkspaceIdAndStatus(workspaceId, SprintStatus.ACTIVE);
            if (activeOpt.isPresent()) {
                effectiveSprintId = activeOpt.get().getId();
            }
        }

        List<BoardColumn> cols = columnRepository.findByWorkspaceIdOrderByOrderAsc(workspaceId);
        List<Task> allTasks = (effectiveSprintId != null)
                ? taskRepository.findBySprintId(effectiveSprintId)
                : taskRepository.findByWorkspaceIdWithDetails(workspaceId);

        Set<String> creatorIds = allTasks.stream()
                .map(Task::getCreatedBy)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<String, User> reporterMap = new HashMap<>();
        if (!creatorIds.isEmpty()) {
            userRepository.findAllById(creatorIds).forEach(u -> {
                if (u.getId() != null) {
                    reporterMap.put(u.getId(), u);
                }
            });
        }

        return cols.stream().map(c -> {
            Stream<Task> stream = allTasks.stream()
                    .filter(t -> c.getId().equals(t.getColumnId()) && !Boolean.TRUE.equals(t.getIsArchived()))
                    .sorted(Comparator.comparing(Task::getLexoRank, Comparator.nullsLast(Comparator.naturalOrder())));

            if (Boolean.TRUE.equals(c.getIsDone()) && doneLimit != null && doneLimit > 0) {
                stream = stream.limit(doneLimit);
            }

            List<Task> columnTasks = stream.collect(Collectors.toList());
            return ColumnWithCardsDto.fromEntity(c, columnTasks, reporterMap);
        }).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<BoardInitColumnDto> getBoardInit(String workspaceId, String sprintId, User currentUser) {
        authorizationService.checkAccess(workspaceId, currentUser);

        String effectiveSprintId = sprintId;
        if (effectiveSprintId == null) {
            Optional<Sprint> activeOpt = sprintRepository.findByWorkspaceIdAndStatus(workspaceId, SprintStatus.ACTIVE);
            if (activeOpt.isPresent()) {
                effectiveSprintId = activeOpt.get().getId();
            }
        }

        List<BoardColumn> cols = columnRepository.findByWorkspaceIdOrderByOrderAsc(workspaceId);
        List<Task> allTasks = (effectiveSprintId != null)
                ? taskRepository.findBySprintId(effectiveSprintId)
                : taskRepository.findByWorkspaceIdWithDetails(workspaceId);

        return cols.stream().map(c -> {
            List<Task> columnTasks = allTasks.stream()
                    .filter(t -> c.getId().equals(t.getColumnId()) && !Boolean.TRUE.equals(t.getIsArchived()))
                    .sorted(Comparator.comparing(Task::getLexoRank, Comparator.nullsLast(Comparator.naturalOrder())))
                    .collect(Collectors.toList());
            return BoardInitColumnDto.fromEntity(c, columnTasks);
        }).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ColumnWithCardsDto> getBoard(String workspaceId, User currentUser) {
        return getBoard(workspaceId, null, null, currentUser);
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
        filter.setSearch((query != null && !query.isBlank()) ? query.trim() : null);

        if (Boolean.TRUE.equals(filter.getDueThisWeek())) {
            filter.setEndOfWeek(LocalDate.now(clock).with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY)));
        }
        filter.setToday(LocalDate.now(clock));

        Page<String> idPage = taskRepository.findIdsByWorkspaceIdFiltered(workspaceId, filter, pageable);
        List<Task> tasks = taskRepository.findByIdIn(idPage.getContent());
        Map<String, Task> taskMap = tasks.stream().collect(Collectors.toMap(Task::getId, t -> t));
        List<TaskDto> dtos = idPage.getContent().stream()
                .map(taskMap::get)
                .filter(Objects::nonNull)
                .map(TaskDto::fromEntity)
                .collect(Collectors.toList());
        return new PageImpl<>(dtos, pageable, idPage.getTotalElements());
    }

    @Transactional(readOnly = true)
    public Page<TaskDto> getMyTasks(User currentUser, TaskFilterRequest filter, Pageable pageable) {
        if (currentUser == null) {
            throw new ForbiddenException("Foydalanuvchi aniqlanmadi");
        }
        if (filter == null) {
            filter = new TaskFilterRequest();
        }
        if (filter.getQ() != null && !filter.getQ().isBlank() && (filter.getSearch() == null || filter.getSearch().isBlank())) {
            filter.setSearch(filter.getQ());
        }
        if (filter.getSearch() != null && filter.getSearch().trim().isEmpty()) {
            filter.setSearch(null);
        }
        if (Boolean.TRUE.equals(filter.getDueThisWeek())) {
            filter.setEndOfWeek(LocalDate.now(clock).with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY)));
        }
        filter.setToday(LocalDate.now(clock));
        filter.setCurrentUserId(currentUser.getId());

        Page<String> idPage = taskRepository.findAssignedTaskIdsByUserIdFiltered(currentUser.getId(), filter, pageable);
        if (idPage.isEmpty()) {
            return new PageImpl<>(List.of(), pageable, idPage.getTotalElements());
        }
        List<Task> tasks = taskRepository.findByIdIn(idPage.getContent());
        Map<String, Task> taskMap = tasks.stream().collect(Collectors.toMap(Task::getId, t -> t));
        List<TaskDto> dtos = idPage.getContent().stream()
                .map(taskMap::get)
                .filter(Objects::nonNull)
                .map(TaskDto::fromEntity)
                .collect(Collectors.toList());
        return new PageImpl<>(dtos, pageable, idPage.getTotalElements());
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
    public TaskDetailsDto getTaskDetails(String id, User currentUser) {
        Task task = taskRepository.findByIdWithDetails(id)
                .or(() -> taskRepository.findById(id))
                .orElseThrow(() -> new ResourceNotFoundException("Task topilmadi: " + id));
        authorizationService.checkAccess(task.getWorkspaceId(), currentUser);

        TaskDto taskDto = TaskDto.fromEntity(task);
        List<ChecklistItemDto> checklists = task.getChecklistItems() != null
                ? task.getChecklistItems().stream().map(ChecklistItemDto::fromEntity).collect(Collectors.toList())
                : List.of();

        List<CommentDto> comments = (commentRepository != null)
                ? commentRepository.findByTaskIdOrderByCreatedAtAsc(id, org.springframework.data.domain.PageRequest.of(0, 50))
                        .getContent().stream().map(CommentDto::fromEntity).collect(Collectors.toList())
                : List.of();

        return TaskDetailsDto.builder()
                .task(taskDto)
                .checklists(checklists)
                .comments(comments)
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

    @Transactional(readOnly = true)
    public List<TaskDto> getSubtasks(String workspaceId, String parentTaskId, User currentUser) {
        authorizationService.checkAccess(workspaceId, currentUser);
        getTaskSimpleAndValidateWorkspace(parentTaskId, workspaceId);
        List<Task> subtasks = taskRepository.findByParentIdOrderByLexoRankAsc(parentTaskId);
        if (subtasks == null || subtasks.isEmpty()) {
            return Collections.emptyList();
        }
        return subtasks.stream()
                .filter(t -> workspaceId.equals(t.getWorkspaceId()))
                .map(TaskDto::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<TaskWorklogDto> getWorklogs(String workspaceId, String taskId, User currentUser) {
        authorizationService.checkAccess(workspaceId, currentUser);
        getTaskSimpleAndValidateWorkspace(taskId, workspaceId);
        if (taskWorklogRepository == null) {
            return Collections.emptyList();
        }
        List<TaskWorklog> logs = taskWorklogRepository.findByTaskIdOrderByCreatedAtDesc(taskId);
        if (logs == null || logs.isEmpty()) {
            return Collections.emptyList();
        }
        Set<String> userIds = logs.stream()
                .map(TaskWorklog::getUserId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<String, User> userMap = new HashMap<>();
        if (!userIds.isEmpty() && userRepository != null) {
            userRepository.findAllById(userIds).forEach(u -> {
                if (u.getId() != null) {
                    userMap.put(u.getId(), u);
                }
            });
        }
        return logs.stream()
                .map(w -> TaskWorklogDto.fromEntity(w, userMap.get(w.getUserId())))
                .collect(Collectors.toList());
    }

    @Transactional
    public TaskWorklogDto addWorklog(String workspaceId, String taskId, TaskWorklogCreateRequest req, User currentUser) {
        authorizationService.checkCanEdit(workspaceId, currentUser);
        Task task = getTaskAndValidateWorkspace(taskId, workspaceId);
        if (req == null || req.getHours() == null || req.getHours() <= 0) {
            throw new BadRequestException("Sarflangan soat (hours) 0 dan katta bo'lishi shart");
        }

        LocalDate workDate = req.getWorkDate() != null ? req.getWorkDate() : LocalDate.now(clock);
        TaskWorklog worklog = TaskWorklog.builder()
                .taskId(taskId)
                .workspaceId(workspaceId)
                .userId(currentUser.getId())
                .hours(req.getHours())
                .description(req.getDescription())
                .workDate(workDate)
                .build();

        if (taskWorklogRepository != null) {
            worklog = taskWorklogRepository.save(worklog);
            Double totalLogged = taskWorklogRepository.sumHoursByTaskId(taskId);
            task.setLoggedHours(totalLogged != null ? totalLogged : req.getHours());
        } else {
            task.logTime(req.getHours());
        }

        Task savedTask = taskRepository.save(task);
        webSocketNotifier.notifyWorkspace(workspaceId, WebSocketEvent.builder()
                .type("TASK_UPDATED")
                .workspaceId(workspaceId)
                .data(TaskDto.fromEntity(savedTask))
                .build());

        return TaskWorklogDto.fromEntity(worklog, currentUser);
    }

    private Set<Direction> resolveDirections(String workspaceId, Set<String> directionIds, Set<String> directionNamesOrIds) {
        Set<Direction> result = new HashSet<>();
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

                Optional<Direction> byId = directionRepository.findByIdAndWorkspaceId(trimmed, workspaceId);
                if (byId != null && byId.isPresent()) {
                    result.add(byId.get());
                    continue;
                }

                Optional<Direction> byName = directionRepository.findByWorkspaceIdAndNameIgnoreCase(workspaceId, trimmed);
                if (byName != null && byName.isPresent()) {
                    result.add(byName.get());
                    continue;
                }

                Direction newDir = Direction.builder()
                        .workspaceId(workspaceId)
                        .name(trimmed)
                        .color("#3B82F6")
                        .build();
                try {
                    Direction saved = directionRepository.saveAndFlush(newDir);
                    result.add(saved != null ? saved : newDir);
                } catch (Exception e) {
                    Optional<Direction> fallback = directionRepository.findByWorkspaceIdAndNameIgnoreCase(workspaceId, trimmed);
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
                }
            }
        }
        return result;
    }

    /**
     * Workspace keyPrefix asosida ketma-ket Jira uslubidagi kodni (masalan, LOY-1, LOY-2) generatsiya qiladi.
     */
    private String generateSequentialPublicId(String workspaceId, Workspace workspace) {
        if (workspace == null) {
            return null;
        }
        String rawPrefix = workspace.getKeyPrefix();
        String cleanPrefix = "WFM";
        if (rawPrefix != null && !rawPrefix.isBlank()) {
            String head = rawPrefix.split("-")[0].replaceAll("[^a-zA-Z0-9]", "").toUpperCase();
            if (!head.isEmpty()) {
                cleanPrefix = head;
            }
        }
        long baseCount = taskRepository.countAllByWorkspaceId(workspaceId);
        long currentSeq = workspace.getTaskSeq() != null ? workspace.getTaskSeq() : 0L;
        long nextSeq = Math.max(baseCount, currentSeq) + 1L;

        String candidate = cleanPrefix + "-" + nextSeq;
        while (taskRepository.existsByPublicId(candidate)) {
            nextSeq++;
            candidate = cleanPrefix + "-" + nextSeq;
        }
        workspace.setTaskSeq(nextSeq);
        return candidate;
    }

    @Transactional
    public TaskDto createTask(String workspaceId, TaskCreateRequest req, User currentUser) {
        Workspace workspace = authorizationService.checkCanEdit(workspaceId, currentUser);

        getColumnAndValidateWorkspace(req.getColumnId(), workspaceId);

        String parentId = null;
        if (req.getParentId() != null && !req.getParentId().isBlank()) {
            Task parentTask = getTaskSimpleAndValidateWorkspace(req.getParentId().trim(), workspaceId);
            parentId = parentTask.getId();
        }

        String lexoRank = req.getLexoRank();
        if (lexoRank == null || lexoRank.isBlank()) {
            String maxRank = taskRepository.findMaxLexoRankByColumnId(req.getColumnId());
            lexoRank = LexoRankUtil.getMiddleSafe(maxRank, null);
        }

        Priority priority = req.getPriority() != null ? req.getPriority() : Priority.MEDIUM;
        IssueType issueType = req.getIssueType() != null ? req.getIssueType() : IssueType.TASK;

        String sprintId = null;
        if (req.getSprintId() != null && !req.getSprintId().isBlank()) {
            Sprint sprint = sprintRepository.findById(req.getSprintId())
                    .orElseThrow(() -> new ResourceNotFoundException("Sprint topilmadi: " + req.getSprintId()));
            if (!workspaceId.equals(sprint.getWorkspaceId())) {
                throw new BadRequestException("Sprint ushbu workspace ga tegishli emas");
            }
            if (sprint.getStatus() == SprintStatus.COMPLETED) {
                throw new BadRequestException("Yopilgan sprintga yangi vazifa biriktirib bo'lmaydi");
            }
            sprintId = sprint.getId();
        }

        String sequentialPublicId = generateSequentialPublicId(workspaceId, workspace);

        Task task = Task.builder()
                .workspaceId(workspaceId)
                .columnId(req.getColumnId())
                .parentId(parentId)
                .publicId(sequentialPublicId)
                .title(req.getTitle())
                .description(req.getDescription())
                .lexoRank(lexoRank)
                .priority(priority)
                .issueType(issueType)
                .dueDate(req.getDueDate())
                .storyPoints(req.getStoryPoints())
                .estimatedHours(req.getEstimatedHours())
                .sprintId(sprintId)
                .recurrenceRule(req.getRecurrenceRule() != null ? req.getRecurrenceRule() : com.taskcenter.model.RecurrenceRule.NONE)
                .recurrenceInterval(req.getRecurrenceInterval() != null && req.getRecurrenceInterval() > 0 ? req.getRecurrenceInterval() : 1)
                .nextOccurrenceDate(RecurringTaskService.computeNextOccurrence(
                        req.getDueDate() != null ? req.getDueDate() : LocalDate.now(clock),
                        req.getRecurrenceRule(),
                        req.getRecurrenceInterval()))
                .build();

        Set<User> assignees = resolveAssignees(req.getAssigneeIds(), req.getUsers());
        if (!assignees.isEmpty()) {
            task.getAssignees().addAll(assignees);
        }

        Set<Direction> directions = resolveDirections(workspaceId, req.getDirectionIds(), req.getDirection());
        if (!directions.isEmpty()) {
            task.getDirections().addAll(directions);
        }

        if (req.getLabelIds() != null && !req.getLabelIds().isEmpty() && labelRepository != null) {
            Set<String> targetLabelIds = req.getLabelIds().stream().filter(Objects::nonNull).collect(Collectors.toSet());
            if (!targetLabelIds.isEmpty()) {
                List<Label> labels = labelRepository.findAllById(targetLabelIds);
                task.getLabels().addAll(labels);
            }
        }

        Task saved = taskRepository.save(task);
        if (LexoRankUtil.needsRebalancing(saved.getLexoRank())) {
            rebalanceColumnInternal(workspaceId, saved.getColumnId());
            saved = taskRepository.findById(saved.getId()).orElse(saved);
        }

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

        columnRepository.findByIdAndWorkspaceId(req.columnId(), workspaceId)
                .orElseThrow(() -> new ResourceNotFoundException("Ustun topilmadi: " + req.columnId()));

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
        boolean movedToDone = false;
        String oldColumnTitle = null;
        String newColumnTitle = null;
        if (req.getColumnId() != null && !req.getColumnId().equals(task.getColumnId())) {
            BoardColumn newColumn = getColumnAndValidateWorkspace(req.getColumnId(), workspaceId);
            if (Boolean.TRUE.equals(newColumn.getIsDone())) {
                if (taskDependencyService != null) {
                    taskDependencyService.assertCanMoveToDone(task);
                }
                movedToDone = true;
            }
            String oldColId = task.getColumnId();
            BoardColumn oldColumn = columnRepository.findById(oldColId).orElse(null);
            oldColumnTitle = oldColumn != null ? oldColumn.getTitle() : oldColId;
            newColumnTitle = newColumn.getTitle();
            columnChanged = true;
            task.setColumnId(req.getColumnId());
            if (req.getLexoRank() == null) {
                String maxRank = taskRepository.findMaxLexoRankByColumnId(req.getColumnId());
                task.setLexoRank(LexoRankUtil.getMiddleSafe(maxRank, null));
            }
            activityService.logActivity(task.getId(), currentUser, TaskActivityType.STATUS_UPDATED, "columnId", oldColId, req.getColumnId());
        }

        if (req.getParentId() != null) {
            String trimmedParent = req.getParentId().trim();
            if (trimmedParent.isEmpty()) {
                task.setParentId(null);
            } else if (!trimmedParent.equals(task.getId())) {
                Task parentTask = getTaskSimpleAndValidateWorkspace(trimmedParent, workspaceId);
                task.setParentId(parentTask.getId());
            }
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
        if (req.getEstimatedHours() != null) {
            task.setEstimatedHours(req.getEstimatedHours());
        }
        if (req.getRecurrenceRule() != null) {
            task.setRecurrenceRule(req.getRecurrenceRule());
            int interval = req.getRecurrenceInterval() != null && req.getRecurrenceInterval() > 0
                    ? req.getRecurrenceInterval()
                    : (task.getRecurrenceInterval() != null ? task.getRecurrenceInterval() : 1);
            task.setRecurrenceInterval(interval);
            task.setNextOccurrenceDate(RecurringTaskService.computeNextOccurrence(
                    task.getDueDate() != null ? task.getDueDate() : LocalDate.now(clock),
                    req.getRecurrenceRule(),
                    interval));
            activityService.logActivity(task.getId(), currentUser, TaskActivityType.RECURRENCE_UPDATED,
                    "recurrenceRule", null, req.getRecurrenceRule().name());
        }
        if (req.getSprintId() != null) {
            String newSprintId = req.getSprintId().isBlank() ? null : req.getSprintId();
            if (newSprintId != null && !newSprintId.equals(task.getSprintId())) {
                Sprint sprint = sprintRepository.findById(newSprintId)
                        .orElseThrow(() -> new ResourceNotFoundException("Sprint topilmadi: " + newSprintId));
                if (!workspaceId.equals(sprint.getWorkspaceId())) {
                    throw new BadRequestException("Sprint ushbu workspace ga tegishli emas");
                }
                if (sprint.getStatus() == SprintStatus.COMPLETED) {
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
            Set<Direction> targetDirs = resolveDirections(workspaceId, req.getDirectionIds(), req.getDirection());
            Set<String> targetDirIds = targetDirs.stream().map(Direction::getId).filter(Objects::nonNull).collect(Collectors.toSet());
            Set<String> existingDirIds = task.getDirections().stream().map(Direction::getId).filter(Objects::nonNull).collect(Collectors.toSet());

            if (!targetDirIds.equals(existingDirIds)) {
                task.getDirections().removeIf(d -> d.getId() != null && !targetDirIds.contains(d.getId()));
                task.getDirections().addAll(targetDirs);
            }
        }

        if (req.getLabelIds() != null && labelRepository != null) {
            Set<String> targetLabelIds = req.getLabelIds().stream().filter(Objects::nonNull).collect(Collectors.toSet());
            Set<Label> existingLabels = new HashSet<>(task.getLabels());
            Set<String> existingLabelIds = existingLabels.stream().map(Label::getId).collect(Collectors.toSet());

            if (!targetLabelIds.equals(existingLabelIds)) {
                task.getLabels().removeIf(l -> !targetLabelIds.contains(l.getId()));
                if (!targetLabelIds.isEmpty()) {
                    List<Label> newLabels = labelRepository.findAllById(targetLabelIds);
                    task.getLabels().addAll(newLabels);
                }
            }
        }

        Task saved = taskRepository.save(task);
        if (columnChanged) {
            telegramNotificationService.sendTaskMovedNotification(saved, currentUser, oldColumnTitle, newColumnTitle);
            if (movedToDone) {
                if (taskDependencyService != null) {
                    taskDependencyService.notifyUnblockedTasksOnDone(saved, currentUser);
                }
                if (recurringTaskService != null) {
                    recurringTaskService.spawnNextTaskWhenCompleted(saved);
                }
            }
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
        boolean movedToDone = false;
        String oldColumnTitle = null;
        String newColumnTitle = null;
        if (req.getColumnId() != null && !req.getColumnId().equals(task.getColumnId())) {
            BoardColumn column = getColumnAndValidateWorkspace(req.getColumnId(), workspaceId);
            if (Boolean.TRUE.equals(column.getIsDone())) {
                if (taskDependencyService != null) {
                    taskDependencyService.assertCanMoveToDone(task);
                }
                movedToDone = true;
            }
            String oldColId = task.getColumnId();
            BoardColumn oldColumn = columnRepository.findById(oldColId).orElse(null);
            oldColumnTitle = oldColumn != null ? oldColumn.getTitle() : oldColId;
            newColumnTitle = column.getTitle();
            columnChanged = true;
            task.setColumnId(req.getColumnId());
            activityService.logActivity(task.getId(), currentUser, TaskActivityType.STATUS_UPDATED, "columnId", oldColId, req.getColumnId());
        }

        String computedRank = LexoRankUtil.getMiddleSafe(req.getPrevRank(), req.getNextRank());
        task.setLexoRank(computedRank);

        Task saved = taskRepository.save(task);

        if (LexoRankUtil.needsRebalancing(req.getPrevRank(), req.getNextRank(), computedRank)) {
            log.info("LexoRank auto-rebalancing triggered for workspace={}, column={}", workspaceId, saved.getColumnId());
            rebalanceColumnInternal(workspaceId, saved.getColumnId());
            saved = taskRepository.findById(saved.getId()).orElse(saved);
        }

        if (columnChanged) {
            telegramNotificationService.sendTaskMovedNotification(saved, currentUser, oldColumnTitle, newColumnTitle);
            if (movedToDone) {
                if (taskDependencyService != null) {
                    taskDependencyService.notifyUnblockedTasksOnDone(saved, currentUser);
                }
                if (recurringTaskService != null) {
                    recurringTaskService.spawnNextTaskWhenCompleted(saved);
                }
            }
        }
        TaskDto taskDto = TaskDto.fromEntity(saved);

        webSocketNotifier.notifyWorkspace(workspaceId, WebSocketEvent.builder()
                .type("TASK_MOVED")
                .workspaceId(workspaceId)
                .data(taskDto)
                .build());

        return taskDto;
    }

    /**
     * Ustundagi barcha vazifalarning LexoRank qiymatlarini Base-36 bo'yicha tekis qayta taqsimlaydi.
     */
    @Transactional
    public List<TaskDto> rebalanceColumnRanks(String workspaceId, String columnId, User currentUser) {
        authorizationService.checkCanEdit(workspaceId, currentUser);
        getColumnAndValidateWorkspace(columnId, workspaceId);
        List<Task> rebalanced = rebalanceColumnInternal(workspaceId, columnId);
        return rebalanced.stream().map(TaskDto::fromEntity).collect(Collectors.toList());
    }

    private List<Task> rebalanceColumnInternal(String workspaceId, String columnId) {
        List<Task> columnTasks = taskRepository.findByColumnIdOrderByLexoRankAsc(columnId);
        if (columnTasks == null || columnTasks.isEmpty()) {
            return Collections.emptyList();
        }
        List<String> balancedRanks = LexoRankUtil.generateBalancedRanks(columnTasks.size());
        for (int i = 0; i < columnTasks.size(); i++) {
            columnTasks.get(i).setLexoRank(balancedRanks.get(i));
        }
        List<Task> savedTasks = taskRepository.saveAll(columnTasks);
        webSocketNotifier.notifyWorkspace(workspaceId, WebSocketEvent.builder()
                .type("COLUMN_REBALANCED")
                .workspaceId(workspaceId)
                .data(Map.of("columnId", columnId, "count", savedTasks.size()))
                .build());
        return savedTasks;
    }

    @Transactional
    public void deleteTask(String workspaceId, String id, User currentUser) {
        authorizationService.checkCanEdit(workspaceId, currentUser);

        Task task = getTaskSimpleAndValidateWorkspace(id, workspaceId);
        taskRepository.deleteById(id);

        webSocketNotifier.notifyWorkspace(workspaceId, WebSocketEvent.builder()
                .type("TASK_DELETED")
                .workspaceId(workspaceId)
                .data(Map.of("taskId", id, "columnId", task.getColumnId()))
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
