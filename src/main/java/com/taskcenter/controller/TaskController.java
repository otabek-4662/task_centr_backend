package com.taskcenter.controller;

import com.taskcenter.dto.ApiResponse;
import com.taskcenter.dto.TaskCreateRequest;
import com.taskcenter.dto.TaskDto;
import com.taskcenter.dto.TaskUpdateRequest;
import com.taskcenter.model.User;
import com.taskcenter.service.TaskService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;

import java.util.List;

@Tag(name = "Tasks", description = "Vazifalar (Tasks) bilan ishlash API lari")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/workspaces/{workspaceId}/tasks")
public class TaskController {

    private final TaskService taskService;

    public TaskController(TaskService taskService) {
        this.taskService = taskService;
    }

    @Operation(
            operationId = "listWorkspaceTasks",
            summary = "Workspace ga tegishli vazifalar ro'yxatini olish (qidiruv, filter va pagination bilan)",
            description = "Filtrlar (columnId, priority, assigneeId, directionId, labelId, sprintId, isArchived, search) va Pageable (page, size, sort) parametrlari bo'yicha vazifalarni sahifalab qaytaradi."
    )
    @GetMapping
    public ApiResponse<Page<TaskDto>> getTasks(
            @io.swagger.v3.oas.annotations.Parameter(description = "Ishchi maydon (workspace) ID si", example = "550e8400-e29b-41d4-a716-446655440000")
            @PathVariable String workspaceId,
            @AuthenticationPrincipal User currentUser,
            @org.springdoc.core.annotations.ParameterObject @ModelAttribute com.taskcenter.dto.TaskFilterRequest filter,
            @org.springdoc.core.annotations.ParameterObject @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        Page<TaskDto> tasks = taskService.getTasksByWorkspace(workspaceId, currentUser, filter, pageable);
        return ApiResponse.success("ok", tasks);
    }

    @Operation(
            operationId = "getWorkspaceTaskById",
            summary = "Bitta vazifani ID orqali olish (batafsil ma'lumotlari bilan)",
            description = "Vazifaning barcha maydonlari, biriktirilgan yo'nalishlari (directions), teglari (labels), ijrochilari (assignees) va sharhlari sonini qaytaradi."
    )
    @GetMapping("/{id}")
    public ApiResponse<TaskDto> getTaskById(
            @io.swagger.v3.oas.annotations.Parameter(description = "Ishchi maydon (workspace) ID si", example = "550e8400-e29b-41d4-a716-446655440000")
            @PathVariable String workspaceId,
            @io.swagger.v3.oas.annotations.Parameter(description = "Vazifa (task) ID si", example = "task-uuid-123")
            @PathVariable String id,
            @AuthenticationPrincipal User currentUser) {
        TaskDto task = taskService.getTaskById(workspaceId, id, currentUser);
        return ApiResponse.success("ok", task);
    }

    @Operation(
            operationId = "createWorkspaceTask",
            summary = "Yangi vazifa yaratish",
            description = "Yangi vazifa yaratadi. Multiple ijrochilar (assigneeIds), multiple yo'nalishlar (directionIds) va multiple teglar (labelIds) qabul qiladi. Lexorank avtomatik hisoblanishi mumkin."
    )
    @PostMapping
    public ApiResponse<TaskDto> createTask(
            @io.swagger.v3.oas.annotations.Parameter(description = "Ishchi maydon (workspace) ID si", example = "550e8400-e29b-41d4-a716-446655440000")
            @PathVariable String workspaceId,
            @Valid @RequestBody TaskCreateRequest request,
            @AuthenticationPrincipal User currentUser) {
        TaskDto task = taskService.createTask(workspaceId, request, currentUser);
        return ApiResponse.success("Bosh og'riq yaratildi", task);
    }

    @Operation(
            operationId = "updateWorkspaceTask",
            summary = "Vazifani to'liq yangilash",
            description = "Vazifaning sarlavhasi, tavsifi, ustuni, muhimligi, ijrochilari, yo'nalishlari va teglarini yangilaydi."
    )
    @PutMapping("/{id}")
    public ApiResponse<TaskDto> updateTask(
            @io.swagger.v3.oas.annotations.Parameter(description = "Ishchi maydon (workspace) ID si", example = "550e8400-e29b-41d4-a716-446655440000")
            @PathVariable String workspaceId,
            @io.swagger.v3.oas.annotations.Parameter(description = "Vazifa (task) ID si", example = "task-uuid-123")
            @PathVariable String id,
            @Valid @RequestBody TaskUpdateRequest request,
            @AuthenticationPrincipal User currentUser) {
        TaskDto task = taskService.updateTask(workspaceId, id, request, currentUser);
        return ApiResponse.success("Bosh og'riq yangilandi", task);
    }

    @Operation(
            operationId = "patchWorkspaceTask",
            summary = "Vazifani qisman yangilash",
            description = "Vazifaning faqat berilgan maydonlarini o'zgartiradi (PATCH amali)."
    )
    @PatchMapping("/{id}")
    public ApiResponse<TaskDto> patchTask(
            @io.swagger.v3.oas.annotations.Parameter(description = "Ishchi maydon (workspace) ID si", example = "550e8400-e29b-41d4-a716-446655440000")
            @PathVariable String workspaceId,
            @io.swagger.v3.oas.annotations.Parameter(description = "Vazifa (task) ID si", example = "task-uuid-123")
            @PathVariable String id,
            @Valid @RequestBody TaskUpdateRequest request,
            @AuthenticationPrincipal User currentUser) {
        TaskDto task = taskService.updateTask(workspaceId, id, request, currentUser);
        return ApiResponse.success("Task yangilandi", task);
    }

    @Operation(
            operationId = "reorderWorkspaceTask",
            summary = "Vazifaning o'rnini (rank) va ustunini yangilash (Lexorank)",
            description = "Kanban doskasida kartochkani sudrab ko'chirish (drag-and-drop) natijasida uning yangi ustunini va prevRank/nextRank asosida LexoRank tartibini yangilaydi."
    )
    @PatchMapping("/{id}/reorder")
    public ApiResponse<TaskDto> reorderTask(
            @io.swagger.v3.oas.annotations.Parameter(description = "Ishchi maydon (workspace) ID si", example = "550e8400-e29b-41d4-a716-446655440000")
            @PathVariable String workspaceId,
            @io.swagger.v3.oas.annotations.Parameter(description = "Vazifa (task) ID si", example = "task-uuid-123")
            @PathVariable String id,
            @Valid @RequestBody com.taskcenter.dto.TaskReorderRequest request,
            @AuthenticationPrincipal User currentUser) {
        TaskDto task = taskService.reorderTask(workspaceId, id, request, currentUser);
        return ApiResponse.success("Vazifa tartibi yangilandi", task);
    }

    @Operation(
            operationId = "toggleTaskWatch",
            summary = "Vazifani kuzatish / kuzatishni to'xtatish (Watch)",
            description = "Joriy foydalanuvchini vazifa kuzatuvchilari ro'yxatiga qo'shadi yoki olib tashlaydi (toggle)."
    )
    @PostMapping("/{id}/watch")
    public ApiResponse<Void> watchTask(
            @io.swagger.v3.oas.annotations.Parameter(description = "Ishchi maydon (workspace) ID si", example = "550e8400-e29b-41d4-a716-446655440000")
            @PathVariable String workspaceId,
            @io.swagger.v3.oas.annotations.Parameter(description = "Vazifa (task) ID si", example = "task-uuid-123")
            @PathVariable String id,
            @AuthenticationPrincipal User currentUser) {
        taskService.toggleWatch(workspaceId, id, currentUser);
        return ApiResponse.success("Kuzatuv holati o'zgartirildi", null);
    }

    @Operation(
            operationId = "deleteWorkspaceTask",
            summary = "Vazifani o'chirish",
            description = "Vazifani soft-delete qiladi. Faqat ruxsatga ega a'zolar bajara oladi."
    )
    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteTask(
            @io.swagger.v3.oas.annotations.Parameter(description = "Ishchi maydon (workspace) ID si", example = "550e8400-e29b-41d4-a716-446655440000")
            @PathVariable String workspaceId,
            @io.swagger.v3.oas.annotations.Parameter(description = "Vazifa (task) ID si", example = "task-uuid-123")
            @PathVariable String id,
            @AuthenticationPrincipal User currentUser) {
        taskService.deleteTask(workspaceId, id, currentUser);
        return ApiResponse.success("Bosh og'riq o'chirildi", null);
    }

    @Operation(
            operationId = "archiveWorkspaceTask",
            summary = "Vazifani arxivga olish",
            description = "Vazifani faol doskadan yashirib, arxivlanganlar ro'yxatiga o'tkazadi."
    )
    @PostMapping("/{id}/archive")
    public ApiResponse<TaskDto> archiveTask(
            @io.swagger.v3.oas.annotations.Parameter(description = "Ishchi maydon (workspace) ID si", example = "550e8400-e29b-41d4-a716-446655440000")
            @PathVariable String workspaceId,
            @io.swagger.v3.oas.annotations.Parameter(description = "Vazifa (task) ID si", example = "task-uuid-123")
            @PathVariable String id,
            @AuthenticationPrincipal User currentUser) {
        TaskDto task = taskService.toggleArchive(workspaceId, id, true, currentUser);
        return ApiResponse.success("Vazifa arxivlandi", task);
    }

    @Operation(
            operationId = "unarchiveWorkspaceTask",
            summary = "Vazifani arxivdan chiqarish",
            description = "Arxivlangan vazifani qaytadan faol doskaga qaytaradi."
    )
    @PostMapping("/{id}/unarchive")
    public ApiResponse<TaskDto> unarchiveTask(
            @io.swagger.v3.oas.annotations.Parameter(description = "Ishchi maydon (workspace) ID si", example = "550e8400-e29b-41d4-a716-446655440000")
            @PathVariable String workspaceId,
            @io.swagger.v3.oas.annotations.Parameter(description = "Vazifa (task) ID si", example = "task-uuid-123")
            @PathVariable String id,
            @AuthenticationPrincipal User currentUser) {
        TaskDto task = taskService.toggleArchive(workspaceId, id, false, currentUser);
        return ApiResponse.success("Vazifa arxivdan chiqarildi", task);
    }

    @Operation(
            operationId = "toggleTaskAssignee",
            summary = "Vazifaga foydalanuvchini biriktirish / olib tashlash",
            description = "Vazifaga bitta foydalanuvchini biriktiradi yoki olib tashlaydi (userId orqali), yoki butun biriktirilganlar ro'yxatini almashtiradi (assigneeIds orqali)."
    )
    @PostMapping("/{id}/assign")
    public ApiResponse<TaskDto> assignTask(
            @io.swagger.v3.oas.annotations.Parameter(description = "Ishchi maydon (workspace) ID si", example = "550e8400-e29b-41d4-a716-446655440000")
            @PathVariable String workspaceId,
            @io.swagger.v3.oas.annotations.Parameter(description = "Vazifa (task) ID si", example = "task-uuid-123")
            @PathVariable String id,
            @io.swagger.v3.oas.annotations.Parameter(description = "Biriktiriladigan foydalanuvchi ID si (ixtiyoriy, agar request body bo'lmasa)", example = "user-uuid-123")
            @RequestParam(required = false) String userId,
            @RequestBody(required = false) com.taskcenter.dto.TaskAssignRequest request,
            @AuthenticationPrincipal User currentUser) {
        String targetUserId = userId;
        if (targetUserId == null && request != null) {
            targetUserId = request.getUserId();
        }

        if (request != null && request.getAssigneeIds() != null) {
            TaskUpdateRequest updateReq = new TaskUpdateRequest();
            updateReq.setAssigneeIds(request.getAssigneeIds());
            TaskDto task = taskService.updateTask(workspaceId, id, updateReq, currentUser);
            return ApiResponse.success("Biriktirilgan foydalanuvchilar yangilandi", task);
        }

        if (targetUserId == null || targetUserId.isBlank()) {
            throw new com.taskcenter.exception.BadRequestException("userId yoki assigneeIds ko'rsatilishi shart");
        }

        TaskDto task = taskService.toggleAssignee(workspaceId, id, targetUserId, currentUser);
        return ApiResponse.success("Biriktirilgan foydalanuvchilar yangilandi", task);
    }
}
