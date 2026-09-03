package com.taskcenter.controller;

import com.taskcenter.dto.*;
import com.taskcenter.model.BoardColumn;
import com.taskcenter.model.Label;
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

    @GetMapping("/workspaces/{workspaceId}/columns")
    public ApiResponse<List<ColumnDto>> getColumns(@PathVariable String workspaceId) {
        List<BoardColumn> cols = columnRepository.findByWorkspaceIdOrderByOrderAsc(workspaceId);
        List<ColumnDto> dtos = cols.stream().map(ColumnDto::fromEntity).collect(Collectors.toList());
        return ApiResponse.success("ok", dtos);
    }

    @PostMapping("/workspaces/{workspaceId}/columns")
    public ApiResponse<ColumnDto> createColumn(@PathVariable String workspaceId,
                                               @RequestBody ColumnCreateRequest req) {
        BoardColumn col = BoardColumn.builder()
                .workspaceId(workspaceId)
                .title(req.getTitle())
                .order(req.getOrder() != null ? req.getOrder() : 0)
                .build();
        columnRepository.save(col);
        return ApiResponse.success("Column yaratildi", ColumnDto.fromEntity(col));
    }

    @PutMapping("/workspaces/{workspaceId}/columns/{id}")
    public ApiResponse<ColumnDto> updateColumn(@PathVariable String workspaceId,
                                               @PathVariable String id,
                                               @RequestBody ColumnCreateRequest req) {
        BoardColumn col = columnRepository.findById(id).orElseThrow(() -> new RuntimeException("Column not found"));
        col.setTitle(req.getTitle());
        if (req.getOrder() != null) col.setOrder(req.getOrder());
        columnRepository.save(col);
        return ApiResponse.success("Column yangilandi", ColumnDto.fromEntity(col));
    }

    @DeleteMapping("/workspaces/{workspaceId}/columns/{id}")
    public ApiResponse<Void> deleteColumn(@PathVariable String workspaceId,
                                          @PathVariable String id) {
        taskRepository.deleteByColumnId(id);
        columnRepository.deleteById(id);
        return ApiResponse.success("Column o'chirildi", null);
    }

    // ========== LABELS ==========

    @GetMapping("/workspaces/{workspaceId}/labels")
    public ApiResponse<List<LabelDto>> getLabels(@PathVariable String workspaceId) {
        List<Label> labels = labelRepository.findByWorkspaceId(workspaceId);
        return ApiResponse.success("ok", labels.stream().map(LabelDto::fromEntity).collect(Collectors.toList()));
    }

    @PostMapping("/workspaces/{workspaceId}/labels")
    public ApiResponse<LabelDto> createLabel(@PathVariable String workspaceId,
                                             @RequestBody LabelDto req) {
        Label label = Label.builder()
                .workspaceId(workspaceId)
                .name(req.getName())
                .color(req.getColor())
                .build();
        labelRepository.save(label);
        return ApiResponse.success("Label yaratildi", LabelDto.fromEntity(label));
    }

    @DeleteMapping("/workspaces/{workspaceId}/labels/{id}")
    public ApiResponse<Void> deleteLabel(@PathVariable String workspaceId,
                                         @PathVariable String id) {
        labelRepository.deleteById(id);
        return ApiResponse.success("Label o'chirildi", null);
    }

    // ========== BOARD ==========

    @GetMapping("/workspaces/{workspaceId}/board")
    public ApiResponse<List<ColumnWithCardsDto>> getBoard(@PathVariable String workspaceId) {
        List<BoardColumn> cols = columnRepository.findByWorkspaceIdOrderByOrderAsc(workspaceId);
        List<ColumnWithCardsDto> result = cols.stream().map(c -> {
            List<Task> tasks = taskRepository.findByColumnIdOrderByOrderAsc(c.getId());
            return ColumnWithCardsDto.fromEntity(c, tasks);
        }).collect(Collectors.toList());
        return ApiResponse.success("ok", result);
    }

    // ========== TASKS ==========

    @GetMapping("/workspaces/{workspaceId}/tasks")
    public ApiResponse<List<TaskDto>> getTasksByWorkspace(@PathVariable String workspaceId) {
        List<Task> tasks = taskRepository.findByWorkspaceIdOrderByOrderAsc(workspaceId);
        List<TaskDto> dtos = tasks.stream().map(TaskDto::fromEntity).collect(Collectors.toList());
        return ApiResponse.success("ok", dtos);
    }

    @GetMapping("/workspaces/{workspaceId}/tasks/{id}")
    public ApiResponse<TaskDto> getTaskById(@PathVariable String workspaceId,
                                            @PathVariable String id) {
        Task task = taskRepository.findById(id).orElseThrow(() -> new RuntimeException("Task topilmadi"));
        return ApiResponse.success("ok", TaskDto.fromEntity(task));
    }

    @PostMapping("/workspaces/{workspaceId}/tasks")
    public ApiResponse<TaskDto> createTask(@PathVariable String workspaceId,
                                           @RequestBody TaskCreateRequest req) {
        Task task = Task.builder()
                .workspaceId(workspaceId)
                .columnId(req.getColumnId())
                .title(req.getTitle())
                .description(req.getDescription())
                .order(req.getOrder() != null ? req.getOrder() : 0)
                .build();
        taskRepository.save(task);
        return ApiResponse.success("Task yaratildi", TaskDto.fromEntity(task));
    }

    @PutMapping("/workspaces/{workspaceId}/tasks/{id}")
    public ApiResponse<TaskDto> updateTask(@PathVariable String workspaceId,
                                           @PathVariable String id,
                                           @RequestBody TaskUpdateRequest req) {
        Task task = taskRepository.findById(id).orElseThrow(() -> new RuntimeException("Task topilmadi"));
        if (req.getTitle() != null) task.setTitle(req.getTitle());
        if (req.getDescription() != null) task.setDescription(req.getDescription());
        if (req.getColumnId() != null) task.setColumnId(req.getColumnId());
        if (req.getOrder() != null) task.setOrder(req.getOrder());
        taskRepository.save(task);
        return ApiResponse.success("Task yangilandi", TaskDto.fromEntity(task));
    }

    @DeleteMapping("/workspaces/{workspaceId}/tasks/{id}")
    public ApiResponse<Void> deleteTask(@PathVariable String workspaceId,
                                        @PathVariable String id) {
        taskRepository.deleteById(id);
        return ApiResponse.success("Task o'chirildi", null);
    }

    // ========== MEMBERS ==========

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
