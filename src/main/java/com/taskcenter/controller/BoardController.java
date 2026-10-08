package com.taskcenter.controller;

import com.taskcenter.dto.ApiResponse;
import com.taskcenter.dto.ColumnWithCardsDto;
import com.taskcenter.model.User;
import com.taskcenter.service.TaskService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Board", description = "Workspace to'liq doska ko'rinishi (Board View)")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/workspaces/{workspaceId}/board")
public class BoardController {

    private final TaskService taskService;

    public BoardController(TaskService taskService) {
        this.taskService = taskService;
    }

    @Operation(
            operationId = "getKanbanBoard",
            summary = "Workspace doskasini barcha ustunlar va ularning vazifalari bilan olish",
            description = "Kanban doskasini barcha faol ustunlari va ularga tegishli kartochkalari (vazifalari) bilan birgalikda yig'ma holda qaytaradi. Ixtiyoriy sprintId orqali filtrlash mumkin."
    )
    @GetMapping
    public ApiResponse<List<ColumnWithCardsDto>> getBoard(
            @io.swagger.v3.oas.annotations.Parameter(description = "Ishchi maydon (workspace) ID si", example = "550e8400-e29b-41d4-a716-446655440000")
            @PathVariable String workspaceId,
            @io.swagger.v3.oas.annotations.Parameter(description = "Sprint bo'yicha filtrlash uchun sprint ID si (ixtiyoriy)", example = "sprint-uuid-123")
            @RequestParam(required = false) String sprintId,
            @AuthenticationPrincipal User currentUser) {
        List<ColumnWithCardsDto> board = taskService.getBoard(workspaceId, sprintId, currentUser);
        return ApiResponse.success("ok", board);
    }
}
