package com.taskcenter.service;

import com.taskcenter.dto.ColumnCreateRequest;
import com.taskcenter.dto.ColumnDto;
import com.taskcenter.dto.TaskDto;
import com.taskcenter.model.Task;
import com.taskcenter.dto.ColumnPatchRequest;
import com.taskcenter.dto.ColumnReorderItem;
import com.taskcenter.exception.ResourceNotFoundException;
import com.taskcenter.model.BoardColumn;
import com.taskcenter.model.User;
import com.taskcenter.repository.ColumnRepository;
import com.taskcenter.repository.TaskRepository;
import com.taskcenter.dto.WebSocketEvent;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class ColumnService {

    private final ColumnRepository columnRepository;
    private final TaskRepository taskRepository;
    private final WorkspaceAuthorizationService authorizationService;
    private final WebSocketNotifier webSocketNotifier;

    public ColumnService(ColumnRepository columnRepository,
                         TaskRepository taskRepository,
                         WorkspaceAuthorizationService authorizationService,
                         WebSocketNotifier webSocketNotifier) {
        this.columnRepository = columnRepository;
        this.taskRepository = taskRepository;
        this.authorizationService = authorizationService;
        this.webSocketNotifier = webSocketNotifier;
    }

    @Transactional(readOnly = true)
    public List<ColumnDto> getColumns(String workspaceId, User currentUser) {
        authorizationService.checkAccess(workspaceId, currentUser);
        List<BoardColumn> columns = columnRepository.findByWorkspaceIdOrderByOrderAsc(workspaceId);
        if (columns.isEmpty()) return List.of();
        Map<String, List<Task>> byColumn = BoardTasks.groupByColumn(taskRepository.findVisibleBoardTasks(workspaceId, null));
        return columns.stream().map(column -> ColumnDto.fromEntity(column,
                byColumn.getOrDefault(column.getId(), List.of()).stream().map(TaskDto::fromEntity).toList()))
                .toList();
    }

    @Transactional
    @CacheEvict(value = "columns", key = "#workspaceId")
    public ColumnDto createColumn(String workspaceId, ColumnCreateRequest request, User currentUser) {
        authorizationService.checkAdmin(workspaceId, currentUser);

        Integer order = request.getOrder();
        if (order == null || order <= 0) {
            order = columnRepository.findMaxOrderByWorkspaceId(workspaceId) + 1;
        }

        Boolean isDone = request.getIsDone() != null ? request.getIsDone() : false;
        Integer wipLimit = (request.getWipLimit() != null && request.getWipLimit() > 0) ? request.getWipLimit() : null;

        BoardColumn column = BoardColumn.builder()
                .workspaceId(workspaceId)
                .title(request.getTitle())
                .order(order)
                .isDone(isDone)
                .wipLimit(wipLimit)
                .build();

        BoardColumn saved = columnRepository.save(column);
        ColumnDto dto = ColumnDto.fromEntity(saved);
        
        webSocketNotifier.notifyWorkspace(workspaceId, WebSocketEvent.builder()
                .type("COLUMN_CREATED")
                .workspaceId(workspaceId)
                .data(dto)
                .build());
                
        return dto;
    }

    private BoardColumn getColumnAndValidateWorkspace(String id, String workspaceId) {
        return columnRepository.findByIdAndWorkspaceId(id, workspaceId)
                .or(() -> columnRepository.findById(id).filter(c -> workspaceId.equals(c.getWorkspaceId())))
                .orElseThrow(() -> new ResourceNotFoundException("Column topilmadi: " + id));
    }

    @Transactional
    @CacheEvict(value = "columns", key = "#workspaceId")
    public ColumnDto updateColumn(String workspaceId, String id, ColumnCreateRequest request, User currentUser) {
        authorizationService.checkAdmin(workspaceId, currentUser);

        BoardColumn column = getColumnAndValidateWorkspace(id, workspaceId);

        if (request.getTitle() != null) {
            if (Boolean.TRUE.equals(column.getIsDefault()) && !column.getTitle().equals(request.getTitle())) {
                throw new com.taskcenter.exception.BadRequestException("Standart (isDefault) ustunlar nomini o'zgartirish mumkin emas");
            }
            column.setTitle(request.getTitle());
        }
        if (request.getOrder() != null) {
            column.setOrder(request.getOrder());
        }
        if (request.getIsDone() != null) {
            column.setIsDone(request.getIsDone());
        }
        if (request.getWipLimit() != null) {
            column.setWipLimit(request.getWipLimit() > 0 ? request.getWipLimit() : null);
        }

        BoardColumn saved = columnRepository.save(column);
        ColumnDto dto = ColumnDto.fromEntity(saved);
        
        webSocketNotifier.notifyWorkspace(workspaceId, WebSocketEvent.builder()
                .type("COLUMN_UPDATED")
                .workspaceId(workspaceId)
                .data(dto)
                .build());
                
        return dto;
    }

    @Transactional
    @CacheEvict(value = "columns", key = "#workspaceId")
    public ColumnDto patchColumn(String workspaceId, String id, ColumnPatchRequest request, User currentUser) {
        authorizationService.checkAdmin(workspaceId, currentUser);

        BoardColumn column = getColumnAndValidateWorkspace(id, workspaceId);

        if (request.getTitle() != null) {
            if (Boolean.TRUE.equals(column.getIsDefault()) && !column.getTitle().equals(request.getTitle())) {
                throw new com.taskcenter.exception.BadRequestException("Standart (isDefault) ustunlar nomini o'zgartirish mumkin emas");
            }
            column.setTitle(request.getTitle());
        }
        if (request.getOrder() != null) {
            column.setOrder(request.getOrder());
        }
        if (request.getIsDone() != null) {
            column.setIsDone(request.getIsDone());
        }
        if (request.getWipLimit() != null) {
            column.setWipLimit(request.getWipLimit() > 0 ? request.getWipLimit() : null);
        }

        BoardColumn saved = columnRepository.save(column);
        ColumnDto dto = ColumnDto.fromEntity(saved);
        
        webSocketNotifier.notifyWorkspace(workspaceId, WebSocketEvent.builder()
                .type("COLUMN_UPDATED")
                .workspaceId(workspaceId)
                .data(dto)
                .build());
                
        return dto;
    }

    @Transactional
    @CacheEvict(value = "columns", key = "#workspaceId")
    public List<ColumnDto> reorderColumns(String workspaceId, List<ColumnReorderItem> items, User currentUser) {
        authorizationService.checkAdmin(workspaceId, currentUser);

        List<ColumnDto> result = new ArrayList<>();
        for (ColumnReorderItem item : items) {
            BoardColumn column = getColumnAndValidateWorkspace(item.getId(), workspaceId);

            if (item.getOrder() != null) {
                column.setOrder(item.getOrder());
            }
            result.add(ColumnDto.fromEntity(columnRepository.save(column)));
        }
        
        webSocketNotifier.notifyWorkspace(workspaceId, WebSocketEvent.builder()
                .type("COLUMNS_REORDERED")
                .workspaceId(workspaceId)
                .data(result)
                .build());
                
        return result;
    }

    @Transactional
    @CacheEvict(value = "columns", key = "#workspaceId")
    public List<ColumnDto> reorderColumnsByIds(String workspaceId, List<String> columnIds, User currentUser) {
        authorizationService.checkAdmin(workspaceId, currentUser);

        List<ColumnDto> result = new ArrayList<>();
        for (int i = 0; i < columnIds.size(); i++) {
            String colId = columnIds.get(i);
            BoardColumn column = getColumnAndValidateWorkspace(colId, workspaceId);

            column.setOrder(i + 1);
            result.add(ColumnDto.fromEntity(columnRepository.save(column)));
        }
        
        webSocketNotifier.notifyWorkspace(workspaceId, WebSocketEvent.builder()
                .type("COLUMNS_REORDERED")
                .workspaceId(workspaceId)
                .data(result)
                .build());
                
        return result;
    }

    @Transactional
    @CacheEvict(value = "columns", key = "#workspaceId")
    public void deleteColumn(String workspaceId, String id, User currentUser) {
        authorizationService.checkAdmin(workspaceId, currentUser);

        BoardColumn column = getColumnAndValidateWorkspace(id, workspaceId);

        if (Boolean.TRUE.equals(column.getIsDefault())) {
            throw new com.taskcenter.exception.BadRequestException("Standart (isDefault) ustunlarni o'chirish mumkin emas");
        }

        taskRepository.deleteByColumnId(id);
        columnRepository.deleteById(id);
        
        webSocketNotifier.notifyWorkspace(workspaceId, WebSocketEvent.builder()
                .type("COLUMN_DELETED")
                .workspaceId(workspaceId)
                .data(java.util.Map.of("id", id))
                .build());
    }
}
