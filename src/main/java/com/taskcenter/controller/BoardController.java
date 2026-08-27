package com.taskcenter.controller;

import com.taskcenter.dto.*;
import com.taskcenter.model.BoardColumn;
import com.taskcenter.model.Task;
import com.taskcenter.model.User;
import com.taskcenter.model.WorkspaceMember;
import com.taskcenter.repository.ColumnRepository;
import com.taskcenter.repository.LabelRepository;
import com.taskcenter.repository.TaskRepository;
import com.taskcenter.repository.UserRepository;
import com.taskcenter.repository.WorkspaceMemberRepository;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class BoardController {

    private final ColumnRepository columnRepository;
    private final LabelRepository labelRepository;
    private final TaskRepository taskRepository;
    private final WorkspaceMemberRepository memberRepository;
    private final UserRepository userRepository;

    public BoardController(ColumnRepository columnRepository, LabelRepository labelRepository,
                           TaskRepository taskRepository, WorkspaceMemberRepository memberRepository,
                           UserRepository userRepository) {
        this.columnRepository = columnRepository;
        this.labelRepository = labelRepository;
        this.taskRepository = taskRepository;
        this.memberRepository = memberRepository;
        this.userRepository = userRepository;
    }

    // ========== COLUMNS ==========

    @GetMapping("/columns")
    public ApiResponse<List<ColumnDto>> getColumns(@RequestParam String workspaceId) {
        List<BoardColumn> cols = columnRepository.findByWorkspaceIdOrderByOrderAsc(workspaceId);
        List<ColumnDto> dtos = cols.stream().map(ColumnDto::fromEntity).collect(Collectors.toList());
        return ApiResponse.success("ok", dtos);
    }

    @GetMapping("/workspaces/{workspaceId}/columns")
    public ApiResponse<List<ColumnDto>> getColumnsByWorkspace(@PathVariable String workspaceId) {
        return getColumns(workspaceId);
    }

    @PostMapping("/columns")
    public ApiResponse<ColumnDto> createColumn(@RequestBody ColumnCreateRequest req) {
        BoardColumn col = BoardColumn.builder()
                .workspaceId(req.getWorkspaceId())
                .title(req.getTitle())
                .order(req.getOrder() != null ? req.getOrder() : 0)
                .build();
        columnRepository.save(col);
        return ApiResponse.success("Column yaratildi", ColumnDto.fromEntity(col));
    }

    @PutMapping("/columns/{id}")
    public ApiResponse<ColumnDto> updateColumn(@PathVariable String id, @RequestBody ColumnCreateRequest req) {
        BoardColumn col = columnRepository.findById(id).orElseThrow(() -> new RuntimeException("Column not found"));
        col.setTitle(req.getTitle());
        if (req.getOrder() != null) col.setOrder(req.getOrder());
        columnRepository.save(col);
        return ApiResponse.success("Column yangilandi", ColumnDto.fromEntity(col));
    }

    @DeleteMapping("/columns/{id}")
    public ApiResponse<Void> deleteColumn(@PathVariable String id) {
        columnRepository.deleteById(id);
        return ApiResponse.success("Column o'chirildi", null);
    }

    // ========== LABELS ==========

    @GetMapping("/labels")
    public ApiResponse<List<LabelDto>> getLabels(@RequestParam String workspaceId) {
        return ApiResponse.success("ok", labelRepository.findByWorkspaceId(workspaceId).stream().map(LabelDto::fromEntity).collect(Collectors.toList()));
    }

    @GetMapping("/workspaces/{workspaceId}/labels")
    public ApiResponse<List<LabelDto>> getLabelsByWorkspace(@PathVariable String workspaceId) {
        return getLabels(workspaceId);
    }

    // ========== BOARD ==========

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

    // ========== ITEMS / TASKS ==========

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

    @PostMapping("/items")
    public ApiResponse<TaskDto> createItem(@RequestBody TaskCreateRequest req) {
        Task task = Task.builder()
                .workspaceId(req.getWorkspaceId())
                .columnId(req.getColumnId())
                .title(req.getTitle())
                .description(req.getDescription())
                .order(req.getOrder() != null ? req.getOrder() : 0)
                .build();
        taskRepository.save(task);
        return ApiResponse.success("Task yaratildi", TaskDto.fromEntity(task));
    }

    @PutMapping("/items/{id}")
    public ApiResponse<TaskDto> updateItem(@PathVariable String id, @RequestBody TaskUpdateRequest req) {
        Task task = taskRepository.findById(id).orElseThrow(() -> new RuntimeException("Task not found"));
        if (req.getTitle() != null) task.setTitle(req.getTitle());
        if (req.getDescription() != null) task.setDescription(req.getDescription());
        if (req.getColumnId() != null) task.setColumnId(req.getColumnId());
        if (req.getOrder() != null) task.setOrder(req.getOrder());
        taskRepository.save(task);
        return ApiResponse.success("Task yangilandi", TaskDto.fromEntity(task));
    }

    @DeleteMapping("/items/{id}")
    public ApiResponse<Void> deleteItem(@PathVariable String id) {
        taskRepository.deleteById(id);
        return ApiResponse.success("Task o'chirildi", null);
    }

    @GetMapping("/workspaces/{workspaceId}/members")
    public ApiResponse<List<UserDto>> getMembers(@PathVariable String workspaceId) {
        List<WorkspaceMember> members = memberRepository.findByWorkspaceId(workspaceId);
        Set<String> memberIds = members.stream().map(WorkspaceMember::getUserId).collect(Collectors.toSet());
        List<UserDto> users = userRepository.findAllById(memberIds).stream()
                .map(UserDto::fromEntity)
                .collect(Collectors.toList());
        return ApiResponse.success("ok", users);
    }
}
