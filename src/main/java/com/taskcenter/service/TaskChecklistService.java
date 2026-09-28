package com.taskcenter.service;

import com.taskcenter.dto.*;
import com.taskcenter.exception.ResourceNotFoundException;
import com.taskcenter.model.Task;
import com.taskcenter.model.TaskChecklistItem;
import com.taskcenter.model.User;
import com.taskcenter.repository.TaskChecklistItemRepository;
import com.taskcenter.repository.TaskRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class TaskChecklistService {

    private final TaskChecklistItemRepository checklistItemRepository;
    private final TaskRepository taskRepository;
    private final WorkspaceAuthorizationService authorizationService;
    private final WebSocketNotifier webSocketNotifier;

    public TaskChecklistService(TaskChecklistItemRepository checklistItemRepository,
                                TaskRepository taskRepository,
                                WorkspaceAuthorizationService authorizationService,
                                WebSocketNotifier webSocketNotifier) {
        this.checklistItemRepository = checklistItemRepository;
        this.taskRepository = taskRepository;
        this.authorizationService = authorizationService;
        this.webSocketNotifier = webSocketNotifier;
    }

    private Task checkAccessAndGetTask(String workspaceId, String taskId, User user, boolean write) {
        if (write) authorizationService.checkCanEdit(workspaceId, user);
        else authorizationService.checkAccess(workspaceId, user);
        
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new ResourceNotFoundException("Task topilmadi"));
        if (!workspaceId.equals(task.getWorkspaceId())) {
            throw new ResourceNotFoundException("Task ushbu workspace ga tegishli emas");
        }
        return task;
    }

    @Transactional(readOnly = true)
    public List<ChecklistItemDto> getItems(String workspaceId, String taskId, User currentUser) {
        checkAccessAndGetTask(workspaceId, taskId, currentUser, false);
        return checklistItemRepository.findByTaskIdOrderByOrderIndexAsc(taskId)
                .stream().map(ChecklistItemDto::fromEntity).collect(Collectors.toList());
    }

    @Transactional
    public ChecklistItemDto addItem(String workspaceId, String taskId, ChecklistItemCreateRequest req, User currentUser) {
        Task task = checkAccessAndGetTask(workspaceId, taskId, currentUser, true);
        
        Integer nextOrder = checklistItemRepository.findMaxOrderIndexByTaskId(taskId) + 1;
        TaskChecklistItem item = TaskChecklistItem.builder()
                .taskId(taskId)
                .title(req.getTitle())
                .orderIndex(nextOrder)
                .build();
        
        TaskChecklistItem saved = checklistItemRepository.save(item);
        ChecklistItemDto dto = ChecklistItemDto.fromEntity(saved);
        
        webSocketNotifier.notifyWorkspace(workspaceId, WebSocketEvent.builder()
                .type("CHECKLIST_ITEM_CREATED")
                .workspaceId(workspaceId)
                .data(dto)
                .build());
        return dto;
    }

    @Transactional
    public ChecklistItemDto updateItem(String workspaceId, String taskId, String itemId, ChecklistItemUpdateRequest req, User currentUser) {
        checkAccessAndGetTask(workspaceId, taskId, currentUser, true);
        
        TaskChecklistItem item = checklistItemRepository.findById(itemId)
                .orElseThrow(() -> new ResourceNotFoundException("Checklist item topilmadi"));
        
        if (!taskId.equals(item.getTaskId())) {
            throw new ResourceNotFoundException("Item bu taskga tegishli emas");
        }

        if (req.getTitle() != null) item.setTitle(req.getTitle());
        if (req.getIsCompleted() != null) item.setIsCompleted(req.getIsCompleted());
        if (req.getOrderIndex() != null) item.setOrderIndex(req.getOrderIndex());
        
        item.setUpdatedAt(LocalDateTime.now());
        TaskChecklistItem saved = checklistItemRepository.save(item);
        
        ChecklistItemDto dto = ChecklistItemDto.fromEntity(saved);
        webSocketNotifier.notifyWorkspace(workspaceId, WebSocketEvent.builder()
                .type("CHECKLIST_ITEM_UPDATED")
                .workspaceId(workspaceId)
                .data(dto)
                .build());
        return dto;
    }

    @Transactional
    public void deleteItem(String workspaceId, String taskId, String itemId, User currentUser) {
        checkAccessAndGetTask(workspaceId, taskId, currentUser, true);
        
        TaskChecklistItem item = checklistItemRepository.findById(itemId)
                .orElseThrow(() -> new ResourceNotFoundException("Checklist item topilmadi"));
                
        if (!taskId.equals(item.getTaskId())) {
            throw new ResourceNotFoundException("Item bu taskga tegishli emas");
        }
        
        checklistItemRepository.delete(item);
        
        webSocketNotifier.notifyWorkspace(workspaceId, WebSocketEvent.builder()
                .type("CHECKLIST_ITEM_DELETED")
                .workspaceId(workspaceId)
                .data(java.util.Map.of("taskId", taskId, "itemId", itemId))
                .build());
    }
}
