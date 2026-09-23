package com.taskcenter.controller;

import com.taskcenter.dto.ApiResponse;
import com.taskcenter.dto.TaskActivityDto;
import com.taskcenter.model.User;
import com.taskcenter.service.TaskActivityService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Task Activities", description = "Task o'zgarishlar tarixi (Audit Trail / History) API lari")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/tasks/{taskId}/activities")
@CrossOrigin(origins = "*")
public class TaskActivityController {

    private final TaskActivityService activityService;

    public TaskActivityController(TaskActivityService activityService) {
        this.activityService = activityService;
    }

    @Operation(summary = "Task o'zgarishlar tarixini sahifalab olish (History)")
    @GetMapping
    public ApiResponse<Page<TaskActivityDto>> getActivities(
            @PathVariable String taskId,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable,
            @AuthenticationPrincipal User currentUser) {
        Page<TaskActivityDto> activities = activityService.getActivities(taskId, pageable, currentUser);
        return ApiResponse.success("ok", activities);
    }
}
