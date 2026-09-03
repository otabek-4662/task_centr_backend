package com.taskcenter.controller;

import com.taskcenter.dto.*;
import com.taskcenter.model.User;
import com.taskcenter.model.Workspace;
import com.taskcenter.repository.*;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/workspaces")
@CrossOrigin(origins = "*")
public class WorkspaceController {

    private final WorkspaceRepository workspaceRepository;
    private final ColumnRepository columnRepository;
    private final TaskRepository taskRepository;
    private final WorkspaceMemberRepository memberRepository;
    private final LabelRepository labelRepository;

    public WorkspaceController(WorkspaceRepository workspaceRepository,
                               ColumnRepository columnRepository,
                               TaskRepository taskRepository,
                               WorkspaceMemberRepository memberRepository,
                               LabelRepository labelRepository) {
        this.workspaceRepository = workspaceRepository;
        this.columnRepository = columnRepository;
        this.taskRepository = taskRepository;
        this.memberRepository = memberRepository;
        this.labelRepository = labelRepository;
    }

    @GetMapping
    public ApiResponse<List<WorkspaceDto>> getWorkspaces(@AuthenticationPrincipal User currentUser) {
        List<Workspace> workspaces = workspaceRepository.findByOwnerIdOrMemberUserId(currentUser.getId());
        return ApiResponse.success("ok", workspaces.stream().map(WorkspaceDto::fromEntity).collect(Collectors.toList()));
    }

    @GetMapping("/{id}")
    public ApiResponse<WorkspaceDto> getWorkspace(@PathVariable String id) {
        Workspace ws = workspaceRepository.findById(id).orElseThrow(() -> new RuntimeException("Workspace topilmadi"));
        return ApiResponse.success("ok", WorkspaceDto.fromEntity(ws));
    }

    @PostMapping
    public ApiResponse<WorkspaceDto> createWorkspace(@RequestBody WorkspaceCreateRequest req,
                                                     @AuthenticationPrincipal User currentUser) {
        Workspace ws = Workspace.builder()
                .title(req.getTitle())
                .bgColor(req.getBgColor())
                .description(req.getDescription())
                .ownerId(currentUser.getId())
                .build();
        workspaceRepository.save(ws);
        return ApiResponse.success("Workspace yaratildi", WorkspaceDto.fromEntity(ws));
    }

    @PutMapping("/{id}")
    public ApiResponse<WorkspaceDto> updateWorkspace(@PathVariable String id,
                                                     @RequestBody WorkspaceCreateRequest req) {
        Workspace ws = workspaceRepository.findById(id).orElseThrow(() -> new RuntimeException("Workspace topilmadi"));
        if (req.getTitle() != null) ws.setTitle(req.getTitle());
        if (req.getBgColor() != null) ws.setBgColor(req.getBgColor());
        if (req.getDescription() != null) ws.setDescription(req.getDescription());
        workspaceRepository.save(ws);
        return ApiResponse.success("Workspace yangilandi", WorkspaceDto.fromEntity(ws));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteWorkspace(@PathVariable String id) {
        List<String> columnIds = columnRepository.findByWorkspaceIdOrderByOrderAsc(id)
                .stream().map(c -> c.getId()).collect(Collectors.toList());
        for (String colId : columnIds) {
            taskRepository.deleteByColumnId(colId);
        }
        columnRepository.deleteByWorkspaceId(id);
        labelRepository.deleteByWorkspaceId(id);
        memberRepository.deleteByWorkspaceId(id);
        workspaceRepository.deleteById(id);
        return ApiResponse.success("Workspace o'chirildi", null);
    }
}
