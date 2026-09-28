package com.taskcenter.controller;

import com.taskcenter.dto.*;
import com.taskcenter.model.SprintStatus;
import com.taskcenter.model.User;
import com.taskcenter.service.SprintService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Sprints", description = "Agile & Scrum Sprint boshqaruvi API lari")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/workspaces/{workspaceId}")
public class SprintController {

    private final SprintService sprintService;

    public SprintController(SprintService sprintService) {
        this.sprintService = sprintService;
    }

    @Operation(summary = "Workspacedagi sprintlar ro'yxatini olish (status bo'yicha filter qilish mumkin)")
    @GetMapping("/sprints")
    public ApiResponse<List<SprintDto>> getSprints(
            @PathVariable String workspaceId,
            @RequestParam(required = false) SprintStatus status,
            @AuthenticationPrincipal User currentUser) {
        List<SprintDto> sprints = sprintService.getSprints(workspaceId, status, currentUser);
        return ApiResponse.success("Sprintlar ro'yxati olindi", sprints);
    }

    @Operation(summary = "Workspace sprintlari samaradorligini (Velocity Chart) olish")
    @GetMapping("/sprints/velocity")
    public ApiResponse<VelocityChartDto> getVelocityChart(
            @PathVariable String workspaceId,
            @AuthenticationPrincipal User currentUser) {
        VelocityChartDto chart = sprintService.getVelocityChart(workspaceId, currentUser);
        return ApiResponse.success("Velocity chart ma'lumotlari olindi", chart);
    }

    @Operation(summary = "Yangi sprint yaratish")
    @PostMapping("/sprints")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<SprintDto> createSprint(
            @PathVariable String workspaceId,
            @Valid @RequestBody SprintCreateRequest req,
            @AuthenticationPrincipal User currentUser) {
        SprintDto sprint = sprintService.createSprint(workspaceId, req, currentUser);
        return ApiResponse.success("Sprint muvaffaqiyatli yaratildi", sprint);
    }

    @Operation(summary = "Sprint ma'lumotlarini id bo'yicha olish")
    @GetMapping("/sprints/{id}")
    public ApiResponse<SprintDto> getSprintById(
            @PathVariable String workspaceId,
            @PathVariable String id,
            @AuthenticationPrincipal User currentUser) {
        SprintDto sprint = sprintService.getSprintById(workspaceId, id, currentUser);
        return ApiResponse.success("Sprint topildi", sprint);
    }

    @Operation(summary = "Sprintni tahrirlash")
    @PutMapping("/sprints/{id}")
    public ApiResponse<SprintDto> updateSprint(
            @PathVariable String workspaceId,
            @PathVariable String id,
            @Valid @RequestBody SprintUpdateRequest req,
            @AuthenticationPrincipal User currentUser) {
        SprintDto sprint = sprintService.updateSprint(workspaceId, id, req, currentUser);
        return ApiResponse.success("Sprint yangilandi", sprint);
    }

    @Operation(summary = "Sprintni boshlash (ACTIVE holatiga o'tkazish)")
    @PostMapping("/sprints/{id}/start")
    public ApiResponse<SprintDto> startSprint(
            @PathVariable String workspaceId,
            @PathVariable String id,
            @AuthenticationPrincipal User currentUser) {
        SprintDto sprint = sprintService.startSprint(workspaceId, id, currentUser);
        return ApiResponse.success("Sprint muvaffaqiyatli boshlandi", sprint);
    }

    @Operation(summary = "Sprintni yakunlash (COMPLETED holatiga o'tkazish)")
    @PostMapping("/sprints/{id}/complete")
    public ApiResponse<SprintDto> completeSprint(
            @PathVariable String workspaceId,
            @PathVariable String id,
            @RequestBody(required = false) CompleteSprintRequest req,
            @AuthenticationPrincipal User currentUser) {
        SprintDto sprint = sprintService.completeSprint(workspaceId, id, req, currentUser);
        return ApiResponse.success("Sprint muvaffaqiyatli yakunlandi", sprint);
    }

    @Operation(summary = "Sprintni o'chirish (vazifalar backlogga qaytariladi)")
    @DeleteMapping("/sprints/{id}")
    public ApiResponse<Void> deleteSprint(
            @PathVariable String workspaceId,
            @PathVariable String id,
            @AuthenticationPrincipal User currentUser) {
        sprintService.deleteSprint(workspaceId, id, currentUser);
        return ApiResponse.success("Sprint o'chirildi va vazifalar backlogga qaytarildi", null);
    }

    @Operation(summary = "Sprintdagi barcha vazifalarni olish")
    @GetMapping("/sprints/{id}/tasks")
    public ApiResponse<List<TaskDto>> getSprintTasks(
            @PathVariable String workspaceId,
            @PathVariable String id,
            @AuthenticationPrincipal User currentUser) {
        List<TaskDto> tasks = sprintService.getSprintTasks(workspaceId, id, currentUser);
        return ApiResponse.success("Sprint vazifalari olindi", tasks);
    }

    @Operation(summary = "Vazifalarni sprintga biriktirish")
    @PostMapping("/sprints/{id}/tasks")
    public ApiResponse<List<TaskDto>> addTasksToSprint(
            @PathVariable String workspaceId,
            @PathVariable String id,
            @Valid @RequestBody SprintTaskMoveRequest req,
            @AuthenticationPrincipal User currentUser) {
        List<TaskDto> tasks = sprintService.addTasksToSprint(workspaceId, id, req, currentUser);
        return ApiResponse.success("Vazifalar sprintga biriktirildi", tasks);
    }

    @Operation(summary = "Vazifani sprintdan chiqarib backlogga qaytarish")
    @DeleteMapping("/sprints/{id}/tasks/{taskId}")
    public ApiResponse<Void> removeTaskFromSprint(
            @PathVariable String workspaceId,
            @PathVariable String id,
            @PathVariable String taskId,
            @AuthenticationPrincipal User currentUser) {
        sprintService.removeTaskFromSprint(workspaceId, id, taskId, currentUser);
        return ApiResponse.success("Vazifa sprintdan chiqarildi", null);
    }

    @Operation(summary = "Workspace Backlog vazifalari ro'yxatini olish (sprintsiz tasklar)")
    @GetMapping("/backlog")
    public ApiResponse<BacklogDto> getBacklog(
            @PathVariable String workspaceId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal User currentUser) {
        BacklogDto backlog = sprintService.getBacklog(workspaceId, page, size, currentUser);
        return ApiResponse.success("Backlog ro'yxati olindi", backlog);
    }
}
