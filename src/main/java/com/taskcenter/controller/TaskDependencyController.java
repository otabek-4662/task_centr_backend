package com.taskcenter.controller;

import com.taskcenter.dto.AddDependencyRequest;
import com.taskcenter.dto.ApiResponse;
import com.taskcenter.dto.DependencyItemDto;
import com.taskcenter.dto.TaskDependenciesDto;
import com.taskcenter.model.User;
import com.taskcenter.service.TaskDependencyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/tasks/{taskId}/dependencies")
@Tag(name = "Task Dependencies", description = "Vazifalar orasidagi bog'liqliklar (Blocker)")
@SecurityRequirement(name = "bearerAuth")
public class TaskDependencyController {

    private final TaskDependencyService taskDependencyService;

    public TaskDependencyController(TaskDependencyService taskDependencyService) {
        this.taskDependencyService = taskDependencyService;
    }

    @GetMapping
    @Operation(summary = "Vazifaning bog'liqliklarini olish")
    public ResponseEntity<ApiResponse<TaskDependenciesDto>> getDependencies(
            @PathVariable String taskId,
            @AuthenticationPrincipal User currentUser) {
        TaskDependenciesDto dto = taskDependencyService.getDependencies(taskId, currentUser);
        return ResponseEntity.ok(ApiResponse.success("Bog'liqliklar olindi", dto));
    }

    @PostMapping
    @Operation(summary = "Vazifaga yangi to'siq (Blocker) bog'liqlik qo'shish")
    public ResponseEntity<ApiResponse<DependencyItemDto>> addDependency(
            @PathVariable String taskId,
            @Valid @RequestBody AddDependencyRequest request,
            @AuthenticationPrincipal User currentUser) {
        DependencyItemDto dto = taskDependencyService.addDependency(taskId, request.getBlockerTaskIdOrCode(), currentUser);
        return ResponseEntity.ok(ApiResponse.success("Bog'liqlik muvaffaqiyatli qo'shildi", dto));
    }

    @DeleteMapping("/{dependencyId}")
    @Operation(summary = "Bog'liqlikni o'chirish")
    public ResponseEntity<ApiResponse<Void>> removeDependency(
            @PathVariable String taskId,
            @PathVariable String dependencyId,
            @AuthenticationPrincipal User currentUser) {
        taskDependencyService.removeDependency(taskId, dependencyId, currentUser);
        return ResponseEntity.ok(ApiResponse.success("Bog'liqlik o'chirildi", null));
    }
}
