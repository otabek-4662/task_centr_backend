package com.taskcenter.controller;

import com.taskcenter.dto.ApiResponse;
import com.taskcenter.dto.BoardInitColumnDto;
import com.taskcenter.dto.ColumnWithCardsDto;
import com.taskcenter.model.User;
import com.taskcenter.service.TaskService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
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
            description = "Kanban doskasini barcha ustunlari (position/order bo'yicha tartiblangan) va ularga tegishli vazifalar (rank bo'yicha tartiblangan) bilan birga to'liq qaytaradi. "
                    + "Har bir vazifada id, key, code, title, description, priority, issueType, storyPoints, rank, columnId, assignee {id, fullName, avatarUrl}, reporter {id, fullName}, createdAt, updatedAt maydonlari mavjud. "
                    + "Ixtiyoriy sprintId va doneLimit (tugallangan ustundagi vazifalar limiti) orqali filtrlash mumkin. Faqat workspace a'zosi ko'ra oladi."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Doska ma'lumotlari muvaffaqiyatli qaytarildi",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponse.class),
                            examples = @ExampleObject(
                                    name = "BoardSuccessResponse",
                                    value = "{\"success\":true,\"message\":\"ok\",\"data\":[{\"id\":\"col-1\",\"name\":\"To Do\",\"title\":\"To Do\",\"position\":1,\"order\":1,\"rank\":\"1\",\"tasks\":[{\"id\":\"t-1\",\"key\":\"WFM-204\",\"code\":\"WFM-204\",\"publicId\":\"WFM-204\",\"title\":\"Dastlabki arxitektura\",\"description\":\"Loyiha arxitekturasini loyihalash\",\"priority\":\"HIGH\",\"issueType\":\"STORY\",\"storyPoints\":5,\"rank\":\"0|hzzzza:\",\"lexoRank\":\"0|hzzzza:\",\"columnId\":\"col-1\",\"workspaceId\":\"ws-1\",\"assignee\":{\"id\":\"u-1\",\"fullName\":\"Ali Valiyev\",\"avatarUrl\":null},\"assignees\":[{\"id\":\"u-1\",\"name\":\"ali\",\"fullName\":\"Ali Valiyev\"}],\"reporter\":{\"id\":\"u-2\",\"fullName\":\"Vali Aliyev\"},\"createdAt\":\"2026-10-12T10:00:00\",\"updatedAt\":\"2026-10-12T10:30:00\"}],\"cards\":[{\"id\":\"t-1\",\"key\":\"WFM-204\",\"title\":\"Dastlabki arxitektura\"}]}]}"
                            )
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "Avtorizatsiyadan o'tilmagan (UNAUTHORIZED)"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "403",
                    description = "Foydalanuvchi ushbu workspace a'zosi emas (FORBIDDEN)"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "Workspace topilmadi (NOT_FOUND)"
            )
    })
    @GetMapping
    public ApiResponse<List<ColumnWithCardsDto>> getBoard(
            @Parameter(description = "Ishchi maydon (workspace) ID si", example = "550e8400-e29b-41d4-a716-446655440000")
            @PathVariable String workspaceId,
            @Parameter(description = "Sprint bo'yicha filtrlash uchun sprint ID si (ixtiyoriy)", example = "sprint-uuid-123")
            @RequestParam(required = false) String sprintId,
            @Parameter(description = "Tugallangan (Done) ustunidagi vazifalar maksimal soni (ixtiyoriy, masalan 50)", example = "50")
            @RequestParam(required = false) Integer doneLimit,
            @AuthenticationPrincipal User currentUser) {
        List<ColumnWithCardsDto> board = (doneLimit != null && doneLimit > 0)
                ? taskService.getBoard(workspaceId, sprintId, doneLimit, currentUser)
                : taskService.getBoard(workspaceId, sprintId, currentUser);
        return ApiResponse.success("ok", board);
    }

    @Operation(
            operationId = "getKanbanBoardInit",
            summary = "Workspace doskasini yangi formatda olish",
            description = "Yangi formatda (TaskCardDto) Kanban doskasini qaytaradi."
    )
    @GetMapping("/init")
    public ApiResponse<List<BoardInitColumnDto>> getBoardInit(
            @Parameter(description = "Ishchi maydon (workspace) ID si", example = "550e8400-e29b-41d4-a716-446655440000")
            @PathVariable String workspaceId,
            @Parameter(description = "Sprint bo'yicha filtrlash uchun sprint ID si (ixtiyoriy)", example = "sprint-uuid-123")
            @RequestParam(required = false) String sprintId,
            @AuthenticationPrincipal User currentUser) {
        List<BoardInitColumnDto> board = taskService.getBoardInit(workspaceId, sprintId, currentUser);
        return ApiResponse.success("ok", board);
    }
}
