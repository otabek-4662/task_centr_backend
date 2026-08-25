package com.taskcenter.service;

import com.taskcenter.dto.*;
import com.taskcenter.exception.ResourceNotFoundException;
import com.taskcenter.model.BoardColumn;
import com.taskcenter.model.Task;
import com.taskcenter.model.User;
import com.taskcenter.repository.ColumnRepository;
import com.taskcenter.repository.TaskRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class TaskService {

    private final TaskRepository taskRepository;
    private final ColumnRepository columnRepository;
    private final WorkspaceAuthorizationService authorizationService;

    public TaskService(TaskRepository taskRepository,
                       ColumnRepository columnRepository,
                       WorkspaceAuthorizationService authorizationService) {
        this.taskRepository = taskRepository;
        this.columnRepository = columnRepository;
        this.authorizationService = authorizationService;
    }

    @Transactional(readOnly = true)
    public List<ColumnWithCardsDto> getBoard(String workspaceId, User currentUser) {
        authorizationService.checkAccess(workspaceId, currentUser);
        List<BoardColumn> cols = columnRepository.findByWorkspaceIdWithTasks(workspaceId);
        return cols.stream().map(c -> {
            List<Task> tasks = c.getTasks().stream()
                    .sorted(Comparator.comparing(Task::getOrder))
                    .collect(Collectors.toList());
            return ColumnWithCardsDto.fromEntity(c, tasks);
        }).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<TaskDto> getTasksByWorkspace(String workspaceId, User currentUser, int page, int size) {
        authorizationService.checkAccess(workspaceId, currentUser);
        if (size > 100) {
            size = 100;
        }
        if (page < 0 || size <= 0) {
            return taskRepository.findByWorkspaceIdOrderByOrderAsc(workspaceId)
                    .stream()
                    .map(TaskDto::fromEntity)
                    .collect(Collectors.toList());
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
        authorizationService.checkAccess(workspaceId, currentUser);

        BoardColumn column = columnRepository.findById(req.getColumnId())
                .orElseThrow(() -> new ResourceNotFoundException("Column topilmadi: " + req.getColumnId()));

        if (!workspaceId.equals(column.getWorkspaceId())) {
            throw new ResourceNotFoundException("Column ushbu workspace ga tegishli emas");
        }

        Integer order = req.getOrder();
        if (order == null || order <= 0) {
            order = taskRepository.findMaxOrderByColumnId(req.getColumnId()) + 1;
        }

        Task task = Task.builder()
                .workspaceId(workspaceId)
                .columnId(req.getColumnId())
                .title(req.getTitle())
                .description(req.getDescription())
                .order(order)
                .build();

        Task saved = taskRepository.save(task);
        return TaskDto.fromEntity(saved);
    }

    @Transactional
    public TaskDto updateTask(String workspaceId, String id, TaskUpdateRequest req, User currentUser) {
        authorizationService.checkAccess(workspaceId, currentUser);

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
            task.setColumnId(req.getColumnId());
        }

        if (req.getTitle() != null) {
            task.setTitle(req.getTitle());
        }
        if (req.getDescription() != null) {
            task.setDescription(req.getDescription());
        }
        if (req.getOrder() != null) {
            task.setOrder(req.getOrder());
        }

        Task saved = taskRepository.save(task);
        return TaskDto.fromEntity(saved);
    }

    @Transactional
    public void deleteTask(String workspaceId, String id, User currentUser) {
        authorizationService.checkAccess(workspaceId, currentUser);

        Task task = taskRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Task topilmadi: " + id));

        if (!workspaceId.equals(task.getWorkspaceId())) {
            throw new ResourceNotFoundException("Task ushbu workspace ga tegishli emas");
        }

        taskRepository.deleteById(id);
    }
}
