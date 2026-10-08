package com.taskcenter.controller;

import com.taskcenter.dto.*;
import com.taskcenter.model.SprintStatus;
import com.taskcenter.model.User;
import com.taskcenter.service.SprintService;
import io.swagger.v3.oas.annotations.Hidden;
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

    @Operation(
            operationId = "listWorkspaceSprints",
            summary = "Workspacedagi sprintlar ro'yxatini olish (status bo'yicha filter qilish mumkin)",
            description = "Ishchi maydonga tegishli barcha sprintlar ro'yxatini qaytaradi. Ixtiyoriy status parametri (PLANNED, ACTIVE, COMPLETED) orqali filtrlash mumkin."
    )
    @GetMapping("/sprints")
    public ApiResponse<List<SprintDto>> getSprints(
            @io.swagger.v3.oas.annotations.Parameter(description = "Ishchi maydon (workspace) ID si", example = "550e8400-e29b-41d4-a716-446655440000")
            @PathVariable String workspaceId,
            @io.swagger.v3.oas.annotations.Parameter(description = "Sprint holati (PLANNED, ACTIVE, COMPLETED)", example = "ACTIVE")
            @RequestParam(required = false) SprintStatus status,
            @AuthenticationPrincipal User currentUser) {
        List<SprintDto> sprints = sprintService.getSprints(workspaceId, status, currentUser);
        return ApiResponse.success("Sprintlar ro'yxati olindi", sprints);
    }

    @Deprecated
    @Hidden
    @Operation(
            operationId = "getSprintVelocityChart",
            summary = "Workspace sprintlari samaradorligini (Velocity Chart) olish",
            description = "Eski endpoint. Tugallangan sprintlar bo'yicha jamoaning ishlash tezligi statistikasi.",
            deprecated = true
    )
    @GetMapping("/sprints/velocity")
    public ApiResponse<VelocityChartDto> getVelocityChart(
            @io.swagger.v3.oas.annotations.Parameter(description = "Ishchi maydon (workspace) ID si", example = "550e8400-e29b-41d4-a716-446655440000")
            @PathVariable String workspaceId,
            @AuthenticationPrincipal User currentUser) {
        VelocityChartDto chart = sprintService.getVelocityChart(workspaceId, currentUser);
        return ApiResponse.success("Velocity chart ma'lumotlari olindi", chart);
    }

    @Operation(
            operationId = "createWorkspaceSprint",
            summary = "Yangi sprint yaratish",
            description = "Yangi Agile sprint yaratadi (boshlang'ich holati PLANNED bo'ladi). Nomi majburiy, maqsad va sanalar ixtiyoriy."
    )
    @PostMapping("/sprints")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<SprintDto> createSprint(
            @io.swagger.v3.oas.annotations.Parameter(description = "Ishchi maydon (workspace) ID si", example = "550e8400-e29b-41d4-a716-446655440000")
            @PathVariable String workspaceId,
            @Valid @RequestBody SprintCreateRequest req,
            @AuthenticationPrincipal User currentUser) {
        SprintDto sprint = sprintService.createSprint(workspaceId, req, currentUser);
        return ApiResponse.success("Sprint muvaffaqiyatli yaratildi", sprint);
    }

    @Operation(
            operationId = "getWorkspaceSprintById",
            summary = "Sprint ma'lumotlarini id bo'yicha olish",
            description = "Sprintning to'liq holati, sanalari va statistikasini qaytaradi."
    )
    @GetMapping("/sprints/{id}")
    public ApiResponse<SprintDto> getSprintById(
            @io.swagger.v3.oas.annotations.Parameter(description = "Ishchi maydon (workspace) ID si", example = "550e8400-e29b-41d4-a716-446655440000")
            @PathVariable String workspaceId,
            @io.swagger.v3.oas.annotations.Parameter(description = "Sprint ID si", example = "sprint-uuid-123")
            @PathVariable String id,
            @AuthenticationPrincipal User currentUser) {
        SprintDto sprint = sprintService.getSprintById(workspaceId, id, currentUser);
        return ApiResponse.success("Sprint topildi", sprint);
    }

    @Operation(
            operationId = "updateWorkspaceSprint",
            summary = "Sprintni tahrirlash",
            description = "Sprint nomi, maqsadi va sanalarini yangilaydi."
    )
    @PutMapping("/sprints/{id}")
    public ApiResponse<SprintDto> updateSprint(
            @io.swagger.v3.oas.annotations.Parameter(description = "Ishchi maydon (workspace) ID si", example = "550e8400-e29b-41d4-a716-446655440000")
            @PathVariable String workspaceId,
            @io.swagger.v3.oas.annotations.Parameter(description = "Sprint ID si", example = "sprint-uuid-123")
            @PathVariable String id,
            @Valid @RequestBody SprintUpdateRequest req,
            @AuthenticationPrincipal User currentUser) {
        SprintDto sprint = sprintService.updateSprint(workspaceId, id, req, currentUser);
        return ApiResponse.success("Sprint yangilandi", sprint);
    }

    @Operation(
            operationId = "startWorkspaceSprint",
            summary = "Sprintni boshlash (ACTIVE holatiga o'tkazish)",
            description = "Sprintni faollashtiradi. Bir vaqtning o'zida faqat bitta sprint ACTIVE bo'lishi mumkin."
    )
    @PostMapping("/sprints/{id}/start")
    public ApiResponse<SprintDto> startSprint(
            @io.swagger.v3.oas.annotations.Parameter(description = "Ishchi maydon (workspace) ID si", example = "550e8400-e29b-41d4-a716-446655440000")
            @PathVariable String workspaceId,
            @io.swagger.v3.oas.annotations.Parameter(description = "Sprint ID si", example = "sprint-uuid-123")
            @PathVariable String id,
            @AuthenticationPrincipal User currentUser) {
        SprintDto sprint = sprintService.startSprint(workspaceId, id, currentUser);
        return ApiResponse.success("Sprint muvaffaqiyatli boshlandi", sprint);
    }

    @Operation(
            operationId = "completeWorkspaceSprint",
            summary = "Sprintni yakunlash (COMPLETED holatiga o'tkazish)",
            description = "Sprintni yakunlaydi. Bajarilmay qolgan vazifalarni boshqa sprintga yoki backlogga o'tkazish imkonini beradi."
    )
    @PostMapping("/sprints/{id}/complete")
    public ApiResponse<SprintDto> completeSprint(
            @io.swagger.v3.oas.annotations.Parameter(description = "Ishchi maydon (workspace) ID si", example = "550e8400-e29b-41d4-a716-446655440000")
            @PathVariable String workspaceId,
            @io.swagger.v3.oas.annotations.Parameter(description = "Sprint ID si", example = "sprint-uuid-123")
            @PathVariable String id,
            @RequestBody(required = false) CompleteSprintRequest req,
            @AuthenticationPrincipal User currentUser) {
        SprintDto sprint = sprintService.completeSprint(workspaceId, id, req, currentUser);
        return ApiResponse.success("Sprint muvaffaqiyatli yakunlandi", sprint);
    }

    @Operation(
            operationId = "deleteWorkspaceSprint",
            summary = "Sprintni o'chirish (vazifalar backlogga qaytariladi)",
            description = "Sprintni o'chiradi, uning ichidagi barcha vazifalar esa avtomatik ravishda umumiy backlogga qaytariladi."
    )
    @DeleteMapping("/sprints/{id}")
    public ApiResponse<Void> deleteSprint(
            @io.swagger.v3.oas.annotations.Parameter(description = "Ishchi maydon (workspace) ID si", example = "550e8400-e29b-41d4-a716-446655440000")
            @PathVariable String workspaceId,
            @io.swagger.v3.oas.annotations.Parameter(description = "Sprint ID si", example = "sprint-uuid-123")
            @PathVariable String id,
            @AuthenticationPrincipal User currentUser) {
        sprintService.deleteSprint(workspaceId, id, currentUser);
        return ApiResponse.success("Sprint o'chirildi va vazifalar backlogga qaytarildi", null);
    }

    @Operation(
            operationId = "getSprintTasks",
            summary = "Sprintdagi barcha vazifalarni olish",
            description = "Ushbu sprintga biriktirilgan barcha vazifalar ro'yxatini qaytaradi."
    )
    @GetMapping("/sprints/{id}/tasks")
    public ApiResponse<List<TaskDto>> getSprintTasks(
            @io.swagger.v3.oas.annotations.Parameter(description = "Ishchi maydon (workspace) ID si", example = "550e8400-e29b-41d4-a716-446655440000")
            @PathVariable String workspaceId,
            @io.swagger.v3.oas.annotations.Parameter(description = "Sprint ID si", example = "sprint-uuid-123")
            @PathVariable String id,
            @AuthenticationPrincipal User currentUser) {
        List<TaskDto> tasks = sprintService.getSprintTasks(workspaceId, id, currentUser);
        return ApiResponse.success("Sprint vazifalari olindi", tasks);
    }

    @Operation(
            operationId = "addTasksToSprint",
            summary = "Vazifalarni sprintga biriktirish",
            description = "Berilgan vazifalar ID lari ro'yxatini (taskIds) ushbu sprintga ko'chiradi."
    )
    @PostMapping("/sprints/{id}/tasks")
    public ApiResponse<List<TaskDto>> addTasksToSprint(
            @io.swagger.v3.oas.annotations.Parameter(description = "Ishchi maydon (workspace) ID si", example = "550e8400-e29b-41d4-a716-446655440000")
            @PathVariable String workspaceId,
            @io.swagger.v3.oas.annotations.Parameter(description = "Sprint ID si", example = "sprint-uuid-123")
            @PathVariable String id,
            @Valid @RequestBody SprintTaskMoveRequest req,
            @AuthenticationPrincipal User currentUser) {
        List<TaskDto> tasks = sprintService.addTasksToSprint(workspaceId, id, req, currentUser);
        return ApiResponse.success("Vazifalar sprintga biriktirildi", tasks);
    }

    @Operation(
            operationId = "removeTaskFromSprint",
            summary = "Vazifani sprintdan chiqarib backlogga qaytarish",
            description = "Bitta vazifani sprint tarkibidan chiqarib, uning sprintId sini bo'shatadi (backlogga o'tkazadi)."
    )
    @DeleteMapping("/sprints/{id}/tasks/{taskId}")
    public ApiResponse<Void> removeTaskFromSprint(
            @io.swagger.v3.oas.annotations.Parameter(description = "Ishchi maydon (workspace) ID si", example = "550e8400-e29b-41d4-a716-446655440000")
            @PathVariable String workspaceId,
            @io.swagger.v3.oas.annotations.Parameter(description = "Sprint ID si", example = "sprint-uuid-123")
            @PathVariable String id,
            @io.swagger.v3.oas.annotations.Parameter(description = "Vazifa (task) ID si", example = "task-uuid-123")
            @PathVariable String taskId,
            @AuthenticationPrincipal User currentUser) {
        sprintService.removeTaskFromSprint(workspaceId, id, taskId, currentUser);
        return ApiResponse.success("Vazifa sprintdan chiqarildi", null);
    }

    @Operation(
            operationId = "getWorkspaceBacklog",
            summary = "Workspace Backlog vazifalari ro'yxatini olish (sprintsiz tasklar)",
            description = "Hech qaysi sprintga biriktirilmagan (sprintId == null) faol vazifalar ro'yxatini sahifalab qaytaradi."
    )
    @GetMapping("/backlog")
    public ApiResponse<BacklogDto> getBacklog(
            @io.swagger.v3.oas.annotations.Parameter(description = "Ishchi maydon (workspace) ID si", example = "550e8400-e29b-41d4-a716-446655440000")
            @PathVariable String workspaceId,
            @io.swagger.v3.oas.annotations.Parameter(description = "Sahifa raqami (0 dan boshlanadi)", example = "0")
            @RequestParam(defaultValue = "0") int page,
            @io.swagger.v3.oas.annotations.Parameter(description = "Sahifa o'lchami", example = "20")
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal User currentUser) {
        BacklogDto backlog = sprintService.getBacklog(workspaceId, page, size, currentUser);
        return ApiResponse.success("Backlog ro'yxati olindi", backlog);
    }
}
