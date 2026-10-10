package com.taskcenter.controller;

import com.taskcenter.dto.ApiResponse;
import com.taskcenter.dto.TaskCreateRequestV2;
import com.taskcenter.dto.TaskResponseV2;
import com.taskcenter.model.User;
import com.taskcenter.service.TaskService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Tasks", description = "Vazifalar (Tasks) bilan ishlash API lari")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/v2/workspaces/{workspaceId}/tasks")
public class TaskControllerV2 {

    private final TaskService taskService;

    public TaskControllerV2(TaskService taskService) {
        this.taskService = taskService;
    }

    @Operation(
            operationId = "createTaskV2",
            summary = "Yangi vazifa yaratish (v2)",
            description = "Toza DTO lar orqali yangi vazifa yaratadi. Ijrochilar faqat ushbu workspace a'zolari bo'lishi shart, yo'nalishlar (direction) qabul qilinadi. Javobda dueDate, storyPoints va workspace dagi a'zolik rollari qaytariladi."
    )
    @PostMapping
    public ApiResponse<TaskResponseV2> createTaskV2(
            @Parameter(description = "Ishchi maydon (workspace) ID si", example = "550e8400-e29b-41d4-a716-446655440000")
            @PathVariable String workspaceId,
            @Valid @RequestBody TaskCreateRequestV2 request,
            @AuthenticationPrincipal User currentUser) {
        TaskResponseV2 response = taskService.createTaskV2(workspaceId, request, currentUser);
        return ApiResponse.success("ok", response);
    }
}
