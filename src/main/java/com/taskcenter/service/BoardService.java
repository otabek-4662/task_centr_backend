package com.taskcenter.service;

import com.taskcenter.dto.*;
import com.taskcenter.exception.EntityNotFoundException;
import com.taskcenter.model.*;
import com.taskcenter.repository.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class BoardService {

    private final ColumnRepository columnRepository;
    private final TaskRepository taskRepository;
    private final LabelRepository labelRepository;
    private final WorkspaceMemberRepository memberRepository;
    private final UserRepository userRepository;
    private final WorkspaceAuthorizationService authorizationService;

    public BoardService(ColumnRepository columnRepository,
                        TaskRepository taskRepository,
                        LabelRepository labelRepository,
                        WorkspaceMemberRepository memberRepository,
                        UserRepository userRepository,
                        WorkspaceAuthorizationService authorizationService) {
        this.columnRepository = columnRepository;
        this.taskRepository = taskRepository;
        this.labelRepository = labelRepository;
        this.memberRepository = memberRepository;
        this.userRepository = userRepository;
        this.authorizationService = authorizationService;
    }

    // ===================== COLUMNS =====================

    public List<ColumnDto> getColumns(String workspaceId, User currentUser) {
        authorizationService.checkAccess(workspaceId, currentUser);
        return columnRepository.findByWorkspaceIdOrderByOrderAsc(workspaceId)
                .stream().map(ColumnDto::fromEntity).collect(Collectors.toList());
    }

    public ColumnDto createColumn(String workspaceId, ColumnCreateRequest req, User currentUser) {
        authorizationService.checkOwnerOrAdmin(workspaceId, currentUser);
        Integer order = req.getOrder();
        if (order == null || order <= 0) {
            order = columnRepository.findMaxOrderByWorkspaceId(workspaceId) + 1;
        }
        BoardColumn col = BoardColumn.builder()
                .workspaceId(workspaceId)
                .title(req.getTitle())
                .order(order)
                .build();
        columnRepository.save(col);
        return ColumnDto.fromEntity(col);
    }

    public ColumnDto updateColumn(String workspaceId, String columnId,
                                  ColumnCreateRequest req, User currentUser) {
        authorizationService.checkOwnerOrAdmin(workspaceId, currentUser);
        BoardColumn col = findColumnInWorkspace(columnId, workspaceId);
        if (req.getTitle() != null) col.setTitle(req.getTitle());
        if (req.getOrder() != null) col.setOrder(req.getOrder());
        columnRepository.save(col);
        return ColumnDto.fromEntity(col);
    }

    public ColumnDto patchColumn(String workspaceId, String columnId,
                                 ColumnPatchRequest req, User currentUser) {
        authorizationService.checkOwnerOrAdmin(workspaceId, currentUser);
        BoardColumn col = findColumnInWorkspace(columnId, workspaceId);
        if (req.getTitle() != null) col.setTitle(req.getTitle());
        if (req.getOrder() != null) col.setOrder(req.getOrder());
        columnRepository.save(col);
        return ColumnDto.fromEntity(col);
    }

    public List<ColumnDto> reorderColumns(String workspaceId,
                                          List<ColumnReorderItem> items, User currentUser) {
        authorizationService.checkOwnerOrAdmin(workspaceId, currentUser);
        List<String> ids = items.stream().map(ColumnReorderItem::getId).collect(Collectors.toList());
        List<BoardColumn> columns = columnRepository.findAllById(ids);
        columns.forEach(col -> {
            if (!workspaceId.equals(col.getWorkspaceId())) {
                throw new EntityNotFoundException("Column", col.getId());
            }
            items.stream()
                    .filter(item -> item.getId().equals(col.getId()))
                    .findFirst()
                    .ifPresent(item -> {
                        if (item.getOrder() != null) col.setOrder(item.getOrder());
                    });
        });
        return columnRepository.saveAll(columns)
                .stream().map(ColumnDto::fromEntity).collect(Collectors.toList());
    }

    public void deleteColumn(String workspaceId, String columnId, User currentUser) {
        authorizationService.checkOwnerOrAdmin(workspaceId, currentUser);
        findColumnInWorkspace(columnId, workspaceId);
        columnRepository.deleteById(columnId);
    }

    // ===================== BOARD =====================

    public List<ColumnWithCardsDto> getBoard(String workspaceId, User currentUser) {
        authorizationService.checkAccess(workspaceId, currentUser);
        return columnRepository.findByWorkspaceIdWithTasks(workspaceId)
                .stream()
                .map(col -> {
                    List<Task> sorted = col.getTasks().stream()
                            .sorted(java.util.Comparator.comparing(Task::getOrder))
                            .collect(Collectors.toList());
                    return ColumnWithCardsDto.fromEntity(col, sorted);
                })
                .collect(Collectors.toList());
    }

    // ===================== TASKS =====================

    public List<TaskDto> getTasksByWorkspace(String workspaceId, User currentUser,
                                             int page, int size) {
        authorizationService.checkAccess(workspaceId, currentUser);
        if (page < 0 || size <= 0) {
            return taskRepository.findByWorkspaceIdOrderByOrderAsc(workspaceId)
                    .stream().map(TaskDto::fromEntity).collect(Collectors.toList());
        }
        if (size > 100) size = 100;
        Pageable pageable = PageRequest.of(page, size);
        Page<Task> taskPage = taskRepository.findByWorkspaceIdPaginated(workspaceId, pageable);
        return taskPage.getContent().stream().map(TaskDto::fromEntity).collect(Collectors.toList());
    }

    public TaskDto getTaskById(String workspaceId, String taskId, User currentUser) {
        authorizationService.checkAccess(workspaceId, currentUser);
        Task task = taskRepository.findByIdWithDetails(taskId)
                .orElseThrow(() -> new EntityNotFoundException("Task", taskId));
        if (!workspaceId.equals(task.getWorkspaceId())) {
            throw new EntityNotFoundException("Task", taskId);
        }
        return TaskDto.fromEntity(task);
    }

    public TaskDto createTask(String workspaceId, TaskCreateRequest req, User currentUser) {
        authorizationService.checkAccess(workspaceId, currentUser);
        BoardColumn column = findColumnInWorkspace(req.getColumnId(), workspaceId);
        Integer order = req.getOrder();
        if (order == null || order <= 0) {
            order = taskRepository.findMaxOrderByColumnId(column.getId()) + 1;
        }
        Task task = Task.builder()
                .workspaceId(workspaceId)
                .columnId(req.getColumnId())
                .title(req.getTitle())
                .description(req.getDescription())
                .order(order)
                .build();
        taskRepository.save(task);
        return TaskDto.fromEntity(task);
    }

    public TaskDto updateTask(String workspaceId, String taskId,
                              TaskUpdateRequest req, User currentUser) {
        authorizationService.checkAccess(workspaceId, currentUser);
        Task task = findTaskInWorkspace(taskId, workspaceId);
        if (req.getTitle() != null) task.setTitle(req.getTitle());
        if (req.getDescription() != null) task.setDescription(req.getDescription());
        if (req.getColumnId() != null) task.setColumnId(req.getColumnId());
        if (req.getOrder() != null) task.setOrder(req.getOrder());
        taskRepository.save(task);
        return TaskDto.fromEntity(task);
    }

    public void deleteTask(String workspaceId, String taskId, User currentUser) {
        authorizationService.checkAccess(workspaceId, currentUser);
        findTaskInWorkspace(taskId, workspaceId);
        taskRepository.deleteById(taskId);
    }

    // ===================== LABELS =====================

    public List<LabelDto> getLabels(String workspaceId, User currentUser) {
        authorizationService.checkAccess(workspaceId, currentUser);
        return labelRepository.findByWorkspaceId(workspaceId)
                .stream().map(LabelDto::fromEntity).collect(Collectors.toList());
    }

    public LabelDto createLabel(String workspaceId, LabelDto req, User currentUser) {
        authorizationService.checkOwnerOrAdmin(workspaceId, currentUser);
        Label label = Label.builder()
                .workspaceId(workspaceId)
                .name(req.getName())
                .color(req.getColor())
                .build();
        labelRepository.save(label);
        return LabelDto.fromEntity(label);
    }

    public void deleteLabel(String workspaceId, String labelId, User currentUser) {
        authorizationService.checkOwnerOrAdmin(workspaceId, currentUser);
        Label label = labelRepository.findById(labelId)
                .orElseThrow(() -> new EntityNotFoundException("Label", labelId));
        if (!workspaceId.equals(label.getWorkspaceId())) {
            throw new EntityNotFoundException("Label", labelId);
        }
        labelRepository.deleteById(labelId);
    }

    // ===================== MEMBERS =====================

    public List<UserDto> getMembers(String workspaceId, User currentUser) {
        authorizationService.checkAccess(workspaceId, currentUser);
        Set<String> memberIds = memberRepository.findByWorkspaceId(workspaceId)
                .stream().map(WorkspaceMember::getUserId).collect(Collectors.toSet());
        return userRepository.findByIdIn(memberIds)
                .stream().map(UserDto::fromEntity).collect(Collectors.toList());
    }

    // ===================== PRIVATE HELPERS =====================

    private BoardColumn findColumnInWorkspace(String columnId, String workspaceId) {
        BoardColumn col = columnRepository.findById(columnId)
                .orElseThrow(() -> new EntityNotFoundException("Column", columnId));
        if (!workspaceId.equals(col.getWorkspaceId())) {
            throw new EntityNotFoundException("Column", columnId);
        }
        return col;
    }

    private Task findTaskInWorkspace(String taskId, String workspaceId) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new EntityNotFoundException("Task", taskId));
        if (!workspaceId.equals(task.getWorkspaceId())) {
            throw new EntityNotFoundException("Task", taskId);
        }
        return task;
    }
}
