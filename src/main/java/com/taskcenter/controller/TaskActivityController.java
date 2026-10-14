package com.taskcenter.controller;

import com.taskcenter.dto.ApiResponse;
import com.taskcenter.dto.TaskActivityDto;
import com.taskcenter.model.User;
import com.taskcenter.service.TaskActivityService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Task Activities", description = "Task va Workspace o'zgarishlar tarixi (Audit Trail / Activity Feed) API lari")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api")
public class TaskActivityController {

    private final TaskActivityService activityService;

    public TaskActivityController(TaskActivityService activityService) {
        this.activityService = activityService;
    }

    @Operation(
            operationId = "getTaskActivities",
            summary = "Task o'zgarishlar tarixini sahifalab olish (History)",
            description = "Vazifada amalga oshirilgan barcha o'zgarishlar (status o'zgarishi, izohlar, ijrochi biriktirilishi, tahrirlar) xronologik tarixini sahifalab qaytaradi."
    )
    @GetMapping("/tasks/{taskId}/activities")
    public ApiResponse<Page<TaskActivityDto>> getActivities(
            @Parameter(description = "Vazifa (task) ID si", example = "task-uuid-123")
            @PathVariable String taskId,
            @ParameterObject @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable,
            @AuthenticationPrincipal User currentUser) {
        Page<TaskActivityDto> activities = activityService.getActivities(taskId, pageable, currentUser);
        return ApiResponse.success("ok", activities);
    }

    @Operation(
            operationId = "getWorkspaceActivities",
            summary = "Workspace bo'yicha barcha vazifalar o'zgarishlar tarixini (Activity Feed) sahifalab olish",
            description = "Ishchi maydondagi barcha vazifalarda sodir bo'lgan harakatlar lentasini (Activity Feed) eng yangisidan boshlab sahifalab qaytaradi."
    )
    @GetMapping("/workspaces/{workspaceId}/activities")
    public ApiResponse<Page<TaskActivityDto>> getWorkspaceActivities(
            @Parameter(description = "Ishchi maydon (workspace) ID si", example = "ws-uuid-123")
            @PathVariable String workspaceId,
            @ParameterObject @PageableDefault(size = 30, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable,
            @AuthenticationPrincipal User currentUser) {
        Page<TaskActivityDto> activities = activityService.getWorkspaceActivities(workspaceId, pageable, currentUser);
        return ApiResponse.success("ok", activities);
    }
}
