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
import com.taskcenter.service.WorkspaceAuthorizationService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
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
    private final WorkspaceAuthorizationService authorizationService;

    public BoardController(ColumnRepository columnRepository, LabelRepository labelRepository,
                           TaskRepository taskRepository, WorkspaceMemberRepository memberRepository,
                           UserRepository userRepository,
                           WorkspaceAuthorizationService authorizationService) {
        this.columnRepository = columnRepository;
        this.labelRepository = labelRepository;
        this.taskRepository = taskRepository;
        this.memberRepository = memberRepository;
        this.userRepository = userRepository;
        this.authorizationService = authorizationService;
    }

    // ========== COLUMNS ==========

    @GetMapping("/workspaces/{workspaceId}/columns")
    public ApiResponse<List<ColumnDto>> getColumns(@PathVariable String workspaceId,
                                                   @AuthenticationPrincipal User currentUser) {
        authorizationService.checkAccess(workspaceId, currentUser);
        List<BoardColumn> cols = columnRepository.findByWorkspaceIdOrderByOrderAsc(workspaceId);
        List<ColumnDto> dtos = cols.stream().map(ColumnDto::fromEntity).collect(Collectors.toList());
        return ApiResponse.success("ok", dtos);
    }

    @PostMapping("/workspaces/{workspaceId}/columns")
    public ApiResponse<ColumnDto> createColumn(@PathVariable String workspaceId,
                                               @Valid @RequestBody ColumnCreateRequest req,
                                               @AuthenticationPrincipal User currentUser) {
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
        return ApiResponse.success("Column yaratildi", ColumnDto.fromEntity(col));
    }

    @PutMapping("/workspaces/{workspaceId}/columns/{id}")
    public ApiResponse<ColumnDto> updateColumn(@PathVariable String workspaceId,
                                               @PathVariable String id,
                                               @RequestBody ColumnCreateRequest req,
                                               @AuthenticationPrincipal User currentUser) {
        authorizationService.checkOwnerOrAdmin(workspaceId, currentUser);
        BoardColumn col = columnRepository.findById(id).orElseThrow(() -> new RuntimeException("Column not found"));
        if (!workspaceId.equals(col.getWorkspaceId())) {
            throw new RuntimeException("Column topilmadi");
        }
        if (req.getTitle() != null) col.setTitle(req.getTitle());
        if (req.getOrder() != null) col.setOrder(req.getOrder());
        columnRepository.save(col);
        return ApiResponse.success("Column yangilandi", ColumnDto.fromEntity(col));
    }

    @PatchMapping("/workspaces/{workspaceId}/columns/{id}")
    public ApiResponse<ColumnDto> patchColumn(@PathVariable String workspaceId,
                                              @PathVariable String id,
                                              @RequestBody ColumnPatchRequest req,
                                              @AuthenticationPrincipal User currentUser) {
        authorizationService.checkOwnerOrAdmin(workspaceId, currentUser);
        BoardColumn col = columnRepository.findById(id).orElseThrow(() -> new RuntimeException("Column topilmadi"));
        if (!workspaceId.equals(col.getWorkspaceId())) throw new RuntimeException("Column topilmadi");
        if (req.getTitle() != null) col.setTitle(req.getTitle());
        if (req.getOrder() != null) col.setOrder(req.getOrder());
        columnRepository.save(col);
        return ApiResponse.success("Column yangilandi", ColumnDto.fromEntity(col));
    }

    @PatchMapping("/workspaces/{workspaceId}/columns")
    public ApiResponse<List<ColumnDto>> reorderColumns(@PathVariable String workspaceId,
                                                       @RequestBody List<ColumnReorderItem> items,
                                                       @AuthenticationPrincipal User currentUser) {
        authorizationService.checkOwnerOrAdmin(workspaceId, currentUser);
        List<ColumnDto> result = items.stream().map(item -> {
            BoardColumn col = columnRepository.findById(item.getId())
                    .orElseThrow(() -> new RuntimeException("Column topilmadi"));
            if (!workspaceId.equals(col.getWorkspaceId())) throw new RuntimeException("Column topilmadi");
            if (item.getOrder() != null) col.setOrder(item.getOrder());
            return ColumnDto.fromEntity(columnRepository.save(col));
        }).collect(Collectors.toList());
        return ApiResponse.success("Columnlar tartibi yangilandi", result);
    }

    @DeleteMapping("/workspaces/{workspaceId}/columns/{id}")
    public ApiResponse<Void> deleteColumn(@PathVariable String workspaceId,
                                          @PathVariable String id,
                                          @AuthenticationPrincipal User currentUser) {
        authorizationService.checkOwnerOrAdmin(workspaceId, currentUser);
        BoardColumn col = columnRepository.findById(id).orElseThrow(() -> new RuntimeException("Column topilmadi"));
        if (!workspaceId.equals(col.getWorkspaceId())) {
            throw new RuntimeException("Column topilmadi");
        }
        columnRepository.deleteById(id);
        return ApiResponse.success("Column o'chirildi", null);
    }

    // ========== LABELS ==========

    @GetMapping("/workspaces/{workspaceId}/labels")
    public ApiResponse<List<LabelDto>> getLabels(@PathVariable String workspaceId,
                                                 @AuthenticationPrincipal User currentUser) {
        authorizationService.checkAccess(workspaceId, currentUser);
        List<Label> labels = labelRepository.findByWorkspaceId(workspaceId);
        return ApiResponse.success("ok", labels.stream().map(LabelDto::fromEntity).collect(Collectors.toList()));
    }

    @PostMapping("/workspaces/{workspaceId}/labels")
    public ApiResponse<LabelDto> createLabel(@PathVariable String workspaceId,
                                             @RequestBody LabelDto req,
                                             @AuthenticationPrincipal User currentUser) {
        authorizationService.checkOwnerOrAdmin(workspaceId, currentUser);
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
                                         @PathVariable String id,
                                         @AuthenticationPrincipal User currentUser) {
        authorizationService.checkOwnerOrAdmin(workspaceId, currentUser);
        Label label = labelRepository.findById(id).orElseThrow(() -> new RuntimeException("Label topilmadi"));
        if (!workspaceId.equals(label.getWorkspaceId())) {
            throw new RuntimeException("Label topilmadi");
        }
        labelRepository.deleteById(id);
        return ApiResponse.success("Label o'chirildi", null);
    }

    // ========== BOARD ==========

    @GetMapping("/workspaces/{workspaceId}/board")
    public ApiResponse<List<ColumnWithCardsDto>> getBoard(@PathVariable String workspaceId,
                                                          @AuthenticationPrincipal User currentUser) {
        authorizationService.checkAccess(workspaceId, currentUser);
        List<BoardColumn> cols = columnRepository.findByWorkspaceIdWithTasks(workspaceId);
        List<ColumnWithCardsDto> result = cols.stream().map(c -> {
            List<Task> tasks = c.getTasks().stream()
                    .sorted(java.util.Comparator.comparing(Task::getOrder))
                    .collect(Collectors.toList());
            return ColumnWithCardsDto.fromEntity(c, tasks);
        }).collect(Collectors.toList());
        return ApiResponse.success("ok", result);
    }

    // ========== TASKS ==========

    @GetMapping("/workspaces/{workspaceId}/tasks")
    public ApiResponse<List<TaskDto>> getTasksByWorkspace(
            @PathVariable String workspaceId,
            @AuthenticationPrincipal User currentUser,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        authorizationService.checkAccess(workspaceId, currentUser);
        if (size > 100) size = 100;
        if (page < 0 || size <= 0) {
            List<Task> tasks = taskRepository.findByWorkspaceIdOrderByOrderAsc(workspaceId);
            return ApiResponse.success("ok", tasks.stream().map(TaskDto::fromEntity).collect(Collectors.toList()));
        }
        org.springframework.data.domain.Pageable pageable = org.springframework.data.domain.PageRequest.of(page, size);
        org.springframework.data.domain.Page<Task> taskPage = taskRepository.findByWorkspaceIdPaginated(workspaceId, pageable);
        List<TaskDto> dtos = taskPage.getContent().stream().map(TaskDto::fromEntity).collect(Collectors.toList());
        return ApiResponse.success("ok", dtos);
    }

    @GetMapping("/workspaces/{workspaceId}/tasks/{id}")
    public ApiResponse<TaskDto> getTaskById(@PathVariable String workspaceId,
                                            @PathVariable String id,
                                            @AuthenticationPrincipal User currentUser) {
        authorizationService.checkAccess(workspaceId, currentUser);
        Task task = taskRepository.findByIdWithDetails(id).orElseThrow(() -> new RuntimeException("Task topilmadi"));
        if (!workspaceId.equals(task.getWorkspaceId())) {
            throw new RuntimeException("Task topilmadi");
        }
        return ApiResponse.success("ok", TaskDto.fromEntity(task));
    }

    @PostMapping("/workspaces/{workspaceId}/tasks")
    public ApiResponse<TaskDto> createTask(@PathVariable String workspaceId,
                                           @Valid @RequestBody TaskCreateRequest req,
                                           @AuthenticationPrincipal User currentUser) {
        authorizationService.checkAccess(workspaceId, currentUser);
        BoardColumn column = columnRepository.findById(req.getColumnId())
                .orElseThrow(() -> new RuntimeException("Column topilmadi"));
        if (!workspaceId.equals(column.getWorkspaceId())) {
            throw new RuntimeException("Column topilmadi");
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
        taskRepository.save(task);
        return ApiResponse.success("Task yaratildi", TaskDto.fromEntity(task));
    }

    @PutMapping("/workspaces/{workspaceId}/tasks/{id}")
    public ApiResponse<TaskDto> updateTask(@PathVariable String workspaceId,
                                           @PathVariable String id,
                                           @RequestBody TaskUpdateRequest req,
                                           @AuthenticationPrincipal User currentUser) {
        authorizationService.checkAccess(workspaceId, currentUser);
        Task task = taskRepository.findById(id).orElseThrow(() -> new RuntimeException("Task topilmadi"));
        if (!workspaceId.equals(task.getWorkspaceId())) {
            throw new RuntimeException("Task topilmadi");
        }
        if (req.getTitle() != null) task.setTitle(req.getTitle());
        if (req.getDescription() != null) task.setDescription(req.getDescription());
        if (req.getColumnId() != null) task.setColumnId(req.getColumnId());
        if (req.getOrder() != null) task.setOrder(req.getOrder());
        taskRepository.save(task);
        return ApiResponse.success("Task yangilandi", TaskDto.fromEntity(task));
    }

    @PatchMapping("/workspaces/{workspaceId}/tasks/{id}")
    public ApiResponse<TaskDto> patchTask(@PathVariable String workspaceId,
                                          @PathVariable String id,
                                          @RequestBody TaskUpdateRequest req,
                                          @AuthenticationPrincipal User currentUser) {
        authorizationService.checkAccess(workspaceId, currentUser);
        Task task = taskRepository.findById(id).orElseThrow(() -> new RuntimeException("Task topilmadi"));
        if (!workspaceId.equals(task.getWorkspaceId())) throw new RuntimeException("Task topilmadi");
        if (req.getTitle() != null) task.setTitle(req.getTitle());
        if (req.getDescription() != null) task.setDescription(req.getDescription());
        if (req.getColumnId() != null) task.setColumnId(req.getColumnId());
        if (req.getOrder() != null) task.setOrder(req.getOrder());
        taskRepository.save(task);
        return ApiResponse.success("Task yangilandi", TaskDto.fromEntity(task));
    }

    @DeleteMapping("/workspaces/{workspaceId}/tasks/{id}")
    public ApiResponse<Void> deleteTask(@PathVariable String workspaceId,
                                        @PathVariable String id,
                                        @AuthenticationPrincipal User currentUser) {
        authorizationService.checkAccess(workspaceId, currentUser);
        Task task = taskRepository.findById(id).orElseThrow(() -> new RuntimeException("Task topilmadi"));
        if (!workspaceId.equals(task.getWorkspaceId())) {
            throw new RuntimeException("Task topilmadi");
        }
        taskRepository.deleteById(id);
        return ApiResponse.success("Task o'chirildi", null);
    }

    // ========== MEMBERS ==========

    @GetMapping("/workspaces/{workspaceId}/members")
    public ApiResponse<List<UserDto>> getMembers(@PathVariable String workspaceId,
                                                 @AuthenticationPrincipal User currentUser) {
        authorizationService.checkAccess(workspaceId, currentUser);
        List<WorkspaceMember> members = memberRepository.findByWorkspaceId(workspaceId);
        Set<String> memberIds = members.stream().map(WorkspaceMember::getUserId).collect(Collectors.toSet());
        List<UserDto> users = userRepository.findByIdIn(memberIds).stream()
                .map(UserDto::fromEntity)
                .collect(Collectors.toList());
        return ApiResponse.success("ok", users);
    }

}
