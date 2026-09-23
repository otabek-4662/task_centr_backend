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

    public TaskService(TaskRepository taskRepository,
                       ColumnRepository columnRepository,
                       com.taskcenter.repository.SprintRepository sprintRepository,
                       WorkspaceAuthorizationService authorizationService,
                       TaskActivityService activityService) {
        this.taskRepository = taskRepository;
        this.columnRepository = columnRepository;
        this.sprintRepository = sprintRepository;
        this.authorizationService = authorizationService;
        this.activityService = activityService;
    }

    @Transactional(readOnly = true)
    public List<ColumnWithCardsDto> getBoard(String workspaceId, String sprintId, User currentUser) {
        authorizationService.checkAccess(workspaceId, currentUser);
        List<BoardColumn> cols = columnRepository.findByWorkspaceIdWithTasks(workspaceId);
        return cols.stream().map(c -> {
            List<Task> tasks = c.getTasks().stream()
                    .filter(t -> sprintId == null || sprintId.equals(t.getSprintId()))
                    .sorted(Comparator.comparing(Task::getOrder))
                    .collect(Collectors.toList());
            return ColumnWithCardsDto.fromEntity(c, tasks);
        }).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ColumnWithCardsDto> getBoard(String workspaceId, User currentUser) {
        return getBoard(workspaceId, null, currentUser);
    }

    @Transactional(readOnly = true)
    public List<TaskDto> getTasksByWorkspace(String workspaceId, User currentUser, int page, int size) {
        authorizationService.checkAccess(workspaceId, currentUser);
        if (page < 0) {
            throw new BadRequestException("Sahifa raqami 0 yoki undan katta bo'lishi kerak");
        }
        if (size <= 0) {
            throw new BadRequestException("Sahifa hajmi 1 yoki undan katta bo'lishi kerak");
        }
        if (size > 100) {
            size = 100;
        }

        Pageable pageable = PageRequest.of(page, size);
        Page<Task> taskPage = taskRepository.findByWorkspaceIdPaginated(workspaceId, pageable);
        return taskPage.getContent()
                .stream()
                .map(TaskDto::fromEntity)
                .collect(Collectors.toList());
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

        Integer order = req.getOrder();
        if (order == null || order <= 0) {
            order = taskRepository.findMaxOrderByColumnId(req.getColumnId()) + 1;
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
            if (sprint.getStatus() == com.taskcenter.model.SprintStatus.CLOSED) {
                throw new BadRequestException("Yopilgan sprintga yangi vazifa biriktirib bo'lmaydi");
            }
            sprintId = sprint.getId();
        }

        Task task = Task.builder()
                .workspaceId(workspaceId)
                .columnId(req.getColumnId())
                .title(req.getTitle())
                .description(req.getDescription())
                .order(order)
                .priority(priority)
                .issueType(issueType)
                .dueDate(req.getDueDate())
                .storyPoints(req.getStoryPoints())
                .sprintId(sprintId)
                .build();

        Task saved = taskRepository.save(task);
        activityService.logActivity(saved.getId(), currentUser, TaskActivityType.TASK_CREATED, "task", null, saved.getTitle());
        return TaskDto.fromEntity(saved);
    }

    @Transactional
    public TaskDto updateTask(String workspaceId, String id, TaskUpdateRequest req, User currentUser) {
        authorizationService.checkCanEdit(workspaceId, currentUser);

        Task task = taskRepository.findById(id)
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
            if (req.getOrder() == null) {
                task.setOrder(taskRepository.findMaxOrderByColumnId(req.getColumnId()) + 1);
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
        if (req.getOrder() != null) {
            task.setOrder(req.getOrder());
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
        if (req.getSprintId() != null) {
            String newSprintId = req.getSprintId().isBlank() ? null : req.getSprintId();
            if (newSprintId != null && !newSprintId.equals(task.getSprintId())) {
                com.taskcenter.model.Sprint sprint = sprintRepository.findById(newSprintId)
                        .orElseThrow(() -> new ResourceNotFoundException("Sprint topilmadi: " + newSprintId));
                if (!workspaceId.equals(sprint.getWorkspaceId())) {
                    throw new BadRequestException("Sprint ushbu workspace ga tegishli emas");
                }
                if (sprint.getStatus() == com.taskcenter.model.SprintStatus.CLOSED) {
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
        return TaskDto.fromEntity(saved);
    }

    @Transactional
    public List<TaskDto> reorderTasks(String workspaceId, String columnId, List<String> taskIds, User currentUser) {
        authorizationService.checkCanEdit(workspaceId, currentUser);

        BoardColumn column = columnRepository.findById(columnId)
                .orElseThrow(() -> new ResourceNotFoundException("Column topilmadi: " + columnId));
        if (!workspaceId.equals(column.getWorkspaceId())) {
            throw new ResourceNotFoundException("Column ushbu workspace ga tegishli emas");
        }

        List<TaskDto> result = new java.util.ArrayList<>();
        for (int i = 0; i < taskIds.size(); i++) {
            String taskId = taskIds.get(i);
            Task task = taskRepository.findById(taskId)
                    .orElseThrow(() -> new ResourceNotFoundException("Task topilmadi: " + taskId));
            if (!workspaceId.equals(task.getWorkspaceId())) {
                throw new ResourceNotFoundException("Task ushbu workspace ga tegishli emas: " + taskId);
            }
            task.setColumnId(columnId);
            task.setOrder(i + 1);
            result.add(TaskDto.fromEntity(taskRepository.save(task)));
        }
        return result;
    }

    @Transactional
    public void deleteTask(String workspaceId, String id, User currentUser) {
        authorizationService.checkCanEdit(workspaceId, currentUser);

        Task task = taskRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Task topilmadi: " + id));

        if (!workspaceId.equals(task.getWorkspaceId())) {
            throw new ResourceNotFoundException("Task ushbu workspace ga tegishli emas");
        }

        taskRepository.deleteById(id);
    }
}
