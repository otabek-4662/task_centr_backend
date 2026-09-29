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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
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

    @org.springframework.beans.factory.annotation.Autowired
    private com.taskcenter.repository.UserRepository userRepository;

    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private @org.springframework.context.annotation.Lazy TelegramBotService telegramBotService;

    public TaskService(TaskRepository taskRepository,
                       ColumnRepository columnRepository,
                       com.taskcenter.repository.SprintRepository sprintRepository,
                       WorkspaceAuthorizationService authorizationService,
                       TaskActivityService activityService,
                       WebSocketNotifier webSocketNotifier,
                       NotificationService notificationService) {
        this.taskRepository = taskRepository;
        this.columnRepository = columnRepository;
        this.sprintRepository = sprintRepository;
        this.authorizationService = authorizationService;
        this.activityService = activityService;
        this.webSocketNotifier = webSocketNotifier;
        this.notificationService = notificationService;
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
            filter.setEndOfWeek(LocalDate.now().with(java.time.temporal.TemporalAdjusters.nextOrSame(java.time.DayOfWeek.SUNDAY)));
        }
        Page<Task> taskPage = taskRepository.findByWorkspaceIdFiltered(workspaceId, filter, pageable);
        return taskPage.map(TaskDto::fromEntity);
    }

    @Transactional(readOnly = true)
    public TaskDto getTaskById(String workspaceId, String id, User currentUser) {
        authorizationService.checkAccess(workspaceId, currentUser);
        Task task = taskRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new ResourceNotFoundException("Task topilmadi: " + id));

        if (!workspaceId.equals(task.getWorkspaceId())) {
            throw new ResourceNotFoundException("Task ushbu workspace ga tegishli emas");
        }
        return TaskDto.fromEntity(task);
    }

    @Transactional
    public TaskDto createTask(String workspaceId, TaskCreateRequest req, User currentUser) {
        authorizationService.checkCanEdit(workspaceId, currentUser);

        BoardColumn column = columnRepository.findById(req.getColumnId())
                .orElseThrow(() -> new ResourceNotFoundException("Column topilmadi: " + req.getColumnId()));

        if (!workspaceId.equals(column.getWorkspaceId())) {
            throw new ResourceNotFoundException("Column ushbu workspace ga tegishli emas");
        }

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

        Task task = taskRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new ResourceNotFoundException("Task topilmadi: " + id));

        if (!workspaceId.equals(task.getWorkspaceId())) {
            throw new ResourceNotFoundException("Task ushbu workspace ga tegishli emas");
        }

        if (req.getColumnId() != null && !req.getColumnId().equals(task.getColumnId())) {
            BoardColumn newColumn = columnRepository.findById(req.getColumnId())
                    .orElseThrow(() -> new ResourceNotFoundException("Yangi column topilmadi: " + req.getColumnId()));
            if (!workspaceId.equals(newColumn.getWorkspaceId())) {
                throw new ResourceNotFoundException("Yangi column ushbu workspace ga tegishli emas");
            }
            String oldColId = task.getColumnId();
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
            LocalDate oldDate = task.getDueDate();
            task.setDueDate(req.getDueDate());
            activityService.logActivity(task.getId(), currentUser, TaskActivityType.DUE_DATE_UPDATED, "dueDate", String.valueOf(oldDate), String.valueOf(req.getDueDate()));
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

        Task saved = taskRepository.save(task);
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

        Task task = taskRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new ResourceNotFoundException("Task topilmadi: " + id));
        if (!workspaceId.equals(task.getWorkspaceId())) {
            throw new ResourceNotFoundException("Task ushbu workspace ga tegishli emas");
        }

        if (req.getColumnId() != null && !req.getColumnId().equals(task.getColumnId())) {
            BoardColumn column = columnRepository.findById(req.getColumnId())
                    .orElseThrow(() -> new ResourceNotFoundException("Column topilmadi: " + req.getColumnId()));
            if (!workspaceId.equals(column.getWorkspaceId())) {
                throw new ResourceNotFoundException("Column ushbu workspace ga tegishli emas");
            }
            String oldColId = task.getColumnId();
            task.setColumnId(req.getColumnId());
            activityService.logActivity(task.getId(), currentUser, TaskActivityType.STATUS_UPDATED, "columnId", oldColId, req.getColumnId());
        }

        task.setLexoRank(com.taskcenter.util.LexoRankUtil.getMiddle(req.getPrevRank(), req.getNextRank()));
        
        Task saved = taskRepository.save(task);
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

        Task task = taskRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new ResourceNotFoundException("Task topilmadi: " + id));

        if (!workspaceId.equals(task.getWorkspaceId())) {
            throw new ResourceNotFoundException("Task ushbu workspace ga tegishli emas");
        }

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

        Task originalTask = taskRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new ResourceNotFoundException("Task topilmadi: " + id));

        if (!workspaceId.equals(originalTask.getWorkspaceId())) {
            throw new ResourceNotFoundException("Task ushbu workspace ga tegishli emas");
        }

        String maxRank = taskRepository.findMaxLexoRankByColumnId(originalTask.getColumnId());
        String newRank = com.taskcenter.util.LexoRankUtil.getMiddle(maxRank, null);

        Task clonedTask = Task.builder()
                .workspaceId(originalTask.getWorkspaceId())
                .columnId(originalTask.getColumnId())
                .title("(Copy) " + originalTask.getTitle())
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

        Task task = taskRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new ResourceNotFoundException("Task topilmadi: " + id));

        if (!workspaceId.equals(task.getWorkspaceId())) {
            throw new ResourceNotFoundException("Task ushbu workspace ga tegishli emas");
        }

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

        Task task = taskRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new ResourceNotFoundException("Task topilmadi: " + id));

        if (!workspaceId.equals(task.getWorkspaceId())) {
            throw new ResourceNotFoundException("Task ushbu workspace ga tegishli emas");
        }

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

        Task task = taskRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new ResourceNotFoundException("Task topilmadi: " + id));

        if (!workspaceId.equals(task.getWorkspaceId())) {
            throw new ResourceNotFoundException("Task ushbu workspace ga tegishli emas");
        }

        User assignee = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Foydalanuvchi topilmadi"));

        boolean isAssigned = task.getAssignees().stream().anyMatch(u -> u.getId().equals(userId));
        if (isAssigned) {
            task.getAssignees().removeIf(u -> u.getId().equals(userId));
        } else {
            task.getAssignees().add(assignee);
            
            if (assignee.getTelegramChatId() != null && telegramBotService != null) {
                telegramBotService.sendMessage(assignee.getTelegramChatId(), "🔔 Yangi vazifa: " + task.getTitle() + "\nWorkspace: " + workspaceId + "\nPrioritet: " + task.getPriority());
            }
        }
        
        Task saved = taskRepository.save(task);
        TaskDto taskDto = TaskDto.fromEntity(saved);
        
        webSocketNotifier.notifyWorkspace(workspaceId, WebSocketEvent.builder()
                .type("TASK_UPDATED")
                .workspaceId(workspaceId)
                .data(taskDto)
                .build());
                
        return taskDto;
    }
}
