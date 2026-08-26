package com.taskcenter.controller;

import com.taskcenter.dto.*;
import com.taskcenter.model.BoardColumn;
import com.taskcenter.model.Task;
import com.taskcenter.repository.ColumnRepository;
import com.taskcenter.repository.LabelRepository;
import com.taskcenter.repository.TaskRepository;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class BoardController {

    private final ColumnRepository columnRepository;
    private final LabelRepository labelRepository;
    private final TaskRepository taskRepository;

    public BoardController(ColumnRepository columnRepository, LabelRepository labelRepository, TaskRepository taskRepository) {
        this.columnRepository = columnRepository;
        this.labelRepository = labelRepository;
        this.taskRepository = taskRepository;
    }

    // Columns without cards
    @GetMapping("/columns")
    public ApiResponse<List<ColumnDto>> getColumns(@RequestParam String workspaceId) {
        List<BoardColumn> cols = columnRepository.findByWorkspaceIdOrderByOrderAsc(workspaceId);
        List<ColumnDto> dtos = cols.stream().map(ColumnDto::fromEntity).collect(Collectors.toList());
        return ApiResponse.success("ok", dtos);
    }

    // Backward compat: workspaceId path
    @GetMapping("/workspaces/{workspaceId}/columns")
    public ApiResponse<List<ColumnDto>> getColumnsByWorkspace(@PathVariable String workspaceId) {
        return getColumns(workspaceId);
    }

    // Labels
    @GetMapping("/labels")
    public ApiResponse<List<LabelDto>> getLabels(@RequestParam String workspaceId) {
        return ApiResponse.success("ok", labelRepository.findByWorkspaceId(workspaceId).stream().map(LabelDto::fromEntity).collect(Collectors.toList()));
    }

    @GetMapping("/workspaces/{workspaceId}/labels")
    public ApiResponse<List<LabelDto>> getLabelsByWorkspace(@PathVariable String workspaceId) {
        return getLabels(workspaceId);
    }

    // Board - columns with cards (frontend first snippet)
    @GetMapping("/board")
    public ApiResponse<List<ColumnWithCardsDto>> getBoard(@RequestParam String workspaceId) {
        List<BoardColumn> cols = columnRepository.findByWorkspaceIdOrderByOrderAsc(workspaceId);
        List<ColumnWithCardsDto> result = cols.stream().map(c -> {
            List<Task> tasks = taskRepository.findByColumnIdOrderByOrderAsc(c.getId());
            return ColumnWithCardsDto.fromEntity(c, tasks);
        }).collect(Collectors.toList());
        return ApiResponse.success("ok", result);
    }

    @GetMapping("/workspaces/{workspaceId}/board")
    public ApiResponse<List<ColumnWithCardsDto>> getBoardByWorkspace(@PathVariable String workspaceId) {
        return getBoard(workspaceId);
    }

    // Items / tasks per column - 4 requests frontend does
    @GetMapping("/items")
    public ApiResponse<List<TaskDto>> getItems(@RequestParam(required = false) String columnId,
                                               @RequestParam(required = false) String workspaceId) {
        List<Task> tasks;
        if (columnId != null && !columnId.isBlank()) {
            tasks = taskRepository.findByColumnIdOrderByOrderAsc(columnId);
        } else if (workspaceId != null && !workspaceId.isBlank()) {
            tasks = taskRepository.findByWorkspaceIdOrderByOrderAsc(workspaceId);
        } else {
            tasks = taskRepository.findAll();
        }
        List<TaskDto> dtos = tasks.stream().map(TaskDto::fromEntity).collect(Collectors.toList());
        return ApiResponse.success("ok", dtos);
    }

    @GetMapping("/tasks")
    public ApiResponse<List<TaskDto>> getTasks(@RequestParam(required = false) String columnId,
                                               @RequestParam(required = false) String workspaceId) {
        return getItems(columnId, workspaceId);
    }

    @GetMapping("/columns/{columnId}/cards")
    public ApiResponse<List<TaskDto>> getCardsByColumn(@PathVariable String columnId) {
        return getItems(columnId, null);
    }

    @GetMapping("/items/{id}")
    public ApiResponse<TaskDto> getItemById(@PathVariable String id) {
        Task task = taskRepository.findById(id).orElseThrow(() -> new RuntimeException("Task not found"));
        return ApiResponse.success("ok", TaskDto.fromEntity(task));
    }

    @GetMapping("/workspaces/{workspaceId}/members")
    public ApiResponse<List<TaskDto>> getMembersWithTasks(@PathVariable String workspaceId) {
        // For compat with "6a451... < memberslar va hamma columndagi tasklar" - return same as board?
        return getItems(null, workspaceId);
    }
}
