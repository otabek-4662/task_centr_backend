package com.taskcenter.service;

import com.taskcenter.dto.ColumnCreateRequest;
import com.taskcenter.dto.ColumnDto;
import com.taskcenter.dto.ColumnPatchRequest;
import com.taskcenter.dto.ColumnReorderItem;
import com.taskcenter.exception.ResourceNotFoundException;
import com.taskcenter.model.BoardColumn;
import com.taskcenter.model.User;
import com.taskcenter.repository.ColumnRepository;
import com.taskcenter.repository.TaskRepository;
import com.taskcenter.dto.WebSocketEvent;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

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
        return columnRepository.findByWorkspaceIdWithTasks(workspaceId)
                .stream()
                .map(ColumnDto::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional
    public ColumnDto createColumn(String workspaceId, ColumnCreateRequest request, User currentUser) {
        authorizationService.checkOwnerOrAdmin(workspaceId, currentUser);

        Integer order = request.getOrder();
        if (order == null || order <= 0) {
            order = columnRepository.findMaxOrderByWorkspaceId(workspaceId) + 1;
        }

        BoardColumn column = BoardColumn.builder()
                .workspaceId(workspaceId)
                .title(request.getTitle())
                .order(order)
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

    @Transactional
    public ColumnDto updateColumn(String workspaceId, String id, ColumnCreateRequest request, User currentUser) {
        authorizationService.checkOwnerOrAdmin(workspaceId, currentUser);

        BoardColumn column = columnRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Column topilmadi: " + id));

        if (!workspaceId.equals(column.getWorkspaceId())) {
            throw new ResourceNotFoundException("Column ushbu workspace ga tegishli emas");
        }

        if (request.getTitle() != null) {
            column.setTitle(request.getTitle());
        }
        if (request.getOrder() != null) {
            column.setOrder(request.getOrder());
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
    public ColumnDto patchColumn(String workspaceId, String id, ColumnPatchRequest request, User currentUser) {
        authorizationService.checkOwnerOrAdmin(workspaceId, currentUser);

        BoardColumn column = columnRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Column topilmadi: " + id));

        if (!workspaceId.equals(column.getWorkspaceId())) {
            throw new ResourceNotFoundException("Column ushbu workspace ga tegishli emas");
        }

        if (request.getTitle() != null) {
            column.setTitle(request.getTitle());
        }
        if (request.getOrder() != null) {
            column.setOrder(request.getOrder());
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
    public List<ColumnDto> reorderColumns(String workspaceId, List<ColumnReorderItem> items, User currentUser) {
        authorizationService.checkOwnerOrAdmin(workspaceId, currentUser);

        List<ColumnDto> result = new ArrayList<>();
        for (ColumnReorderItem item : items) {
            BoardColumn column = columnRepository.findById(item.getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Column topilmadi: " + item.getId()));

            if (!workspaceId.equals(column.getWorkspaceId())) {
                throw new ResourceNotFoundException("Column ushbu workspace ga tegishli emas: " + item.getId());
            }

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
    public List<ColumnDto> reorderColumnsByIds(String workspaceId, List<String> columnIds, User currentUser) {
        authorizationService.checkOwnerOrAdmin(workspaceId, currentUser);

        List<ColumnDto> result = new ArrayList<>();
        for (int i = 0; i < columnIds.size(); i++) {
            String colId = columnIds.get(i);
            BoardColumn column = columnRepository.findById(colId)
                    .orElseThrow(() -> new ResourceNotFoundException("Column topilmadi: " + colId));

            if (!workspaceId.equals(column.getWorkspaceId())) {
                throw new ResourceNotFoundException("Column ushbu workspace ga tegishli emas: " + colId);
            }

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
    public void deleteColumn(String workspaceId, String id, User currentUser) {
        authorizationService.checkOwnerOrAdmin(workspaceId, currentUser);

        BoardColumn column = columnRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Column topilmadi: " + id));

        if (!workspaceId.equals(column.getWorkspaceId())) {
            throw new ResourceNotFoundException("Column ushbu workspace ga tegishli emas");
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
