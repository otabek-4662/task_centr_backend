package com.taskcenter.controller;

import com.taskcenter.dto.ApiResponse;
import com.taskcenter.dto.TaskDto;
import com.taskcenter.model.User;
import com.taskcenter.service.TaskService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Tasks Direct", description = "Vazifalar bilan bevosita ishlash (workspace id siz)")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/tasks")
public class TaskDirectController {

    private final TaskService taskService;

    public TaskDirectController(TaskService taskService) {
        this.taskService = taskService;
    }

    @Operation(summary = "Vazifani bevosita ID orqali olish (faqat a'zolar uchun)")
    @GetMapping("/{id}")
    public ApiResponse<TaskDto> getTaskDirectById(
            @PathVariable String id,
            @AuthenticationPrincipal User currentUser) {
        TaskDto task = taskService.getTaskDirectById(id, currentUser);
        return ApiResponse.success("ok", task);
    }
}
