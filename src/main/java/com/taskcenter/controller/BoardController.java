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
@CrossOrigin(origins = "*")
public class BoardController {

    private final TaskService taskService;

    public BoardController(TaskService taskService) {
        this.taskService = taskService;
    }

    @Operation(summary = "Workspace doskasini barcha ustunlar va ularning vazifalari bilan olish")
    @GetMapping
    public ApiResponse<List<ColumnWithCardsDto>> getBoard(
            @PathVariable String workspaceId,
            @AuthenticationPrincipal User currentUser) {
        List<ColumnWithCardsDto> board = taskService.getBoard(workspaceId, currentUser);
        return ApiResponse.success("ok", board);
    }
}
