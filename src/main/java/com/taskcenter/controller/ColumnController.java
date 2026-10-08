package com.taskcenter.controller;

import com.taskcenter.dto.ApiResponse;
import com.taskcenter.dto.ColumnCreateRequest;
import com.taskcenter.dto.ColumnDto;
import com.taskcenter.dto.ColumnPatchRequest;
import com.taskcenter.dto.ColumnReorderItem;
import com.taskcenter.model.User;
import com.taskcenter.service.ColumnService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Columns", description = "Workspace ustunlari (doskalar) bilan ishlash API lari")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/workspaces/{workspaceId}/columns")
public class ColumnController {

    private final ColumnService columnService;

    public ColumnController(ColumnService columnService) {
        this.columnService = columnService;
    }

    @Operation(
            operationId = "getWorkspaceColumns",
            summary = "Workspace ga tegishli ustunlar ro'yxatini olish",
            description = "Ishchi maydonga tegishli barcha ustunlar ro'yxatini tartiblangan holatda qaytaradi."
    )
    @GetMapping
    public ApiResponse<List<ColumnDto>> getColumns(
            @io.swagger.v3.oas.annotations.Parameter(description = "Ishchi maydon (workspace) ID si", example = "550e8400-e29b-41d4-a716-446655440000")
            @PathVariable String workspaceId,
            @AuthenticationPrincipal User currentUser) {
        List<ColumnDto> columns = columnService.getColumns(workspaceId, currentUser);
        return ApiResponse.success("ok", columns);
    }

    @Operation(
            operationId = "createWorkspaceColumn",
            summary = "Yangi ustun yaratish",
            description = "Kanban doskasiga yangi ustun qo'shadi. Ustun nomi va ixtiyoriy tartib raqami beriladi."
    )
    @PostMapping
    public ApiResponse<ColumnDto> createColumn(
            @io.swagger.v3.oas.annotations.Parameter(description = "Ishchi maydon (workspace) ID si", example = "550e8400-e29b-41d4-a716-446655440000")
            @PathVariable String workspaceId,
            @Valid @RequestBody ColumnCreateRequest request,
            @AuthenticationPrincipal User currentUser) {
        ColumnDto column = columnService.createColumn(workspaceId, request, currentUser);
        return ApiResponse.success("Column yaratildi", column);
    }

    @Operation(
            operationId = "updateWorkspaceColumn",
            summary = "Ustunni to'liq yangilash",
            description = "Ustunning sarlavhasi, tartib indeksi va tugallanganlik (isDone) holatini to'liq yangilaydi."
    )
    @PutMapping("/{id}")
    public ApiResponse<ColumnDto> updateColumn(
            @io.swagger.v3.oas.annotations.Parameter(description = "Ishchi maydon (workspace) ID si", example = "550e8400-e29b-41d4-a716-446655440000")
            @PathVariable String workspaceId,
            @io.swagger.v3.oas.annotations.Parameter(description = "Ustun (column) ID si", example = "col-uuid-123")
            @PathVariable String id,
            @Valid @RequestBody ColumnCreateRequest request,
            @AuthenticationPrincipal User currentUser) {
        ColumnDto column = columnService.updateColumn(workspaceId, id, request, currentUser);
        return ApiResponse.success("Column yangilandi", column);
    }

    @Operation(
            operationId = "patchWorkspaceColumn",
            summary = "Ustunni qisman yangilash",
            description = "Ustunning faqat uzatilgan parametrlarini yangilaydi."
    )
    @PatchMapping("/{id}")
    public ApiResponse<ColumnDto> patchColumn(
            @io.swagger.v3.oas.annotations.Parameter(description = "Ishchi maydon (workspace) ID si", example = "550e8400-e29b-41d4-a716-446655440000")
            @PathVariable String workspaceId,
            @io.swagger.v3.oas.annotations.Parameter(description = "Ustun (column) ID si", example = "col-uuid-123")
            @PathVariable String id,
            @Valid @RequestBody ColumnPatchRequest request,
            @AuthenticationPrincipal User currentUser) {
        ColumnDto column = columnService.patchColumn(workspaceId, id, request, currentUser);
        return ApiResponse.success("Column yangilandi", column);
    }

    @Deprecated
    @Operation(
            operationId = "reorderWorkspaceColumnsLegacy",
            summary = "Ustunlar tartibini o'zgartirish (eski variant)",
            description = "Eski variant (List<ColumnReorderItem>). Frontend uchun tavsiya etilgan qulay variant: PATCH /api/workspaces/{workspaceId}/columns/reorder (oddiy List<String> columnIds massivi).",
            deprecated = true
    )
    @PatchMapping
    public ApiResponse<List<ColumnDto>> reorderColumns(
            @io.swagger.v3.oas.annotations.Parameter(description = "Ishchi maydon (workspace) ID si", example = "550e8400-e29b-41d4-a716-446655440000")
            @PathVariable String workspaceId,
            @RequestBody List<ColumnReorderItem> items,
            @AuthenticationPrincipal User currentUser) {
        List<ColumnDto> columns = columnService.reorderColumns(workspaceId, items, currentUser);
        return ApiResponse.success("Columnlar tartibi yangilandi", columns);
    }

    @Operation(
            operationId = "reorderWorkspaceColumns",
            summary = "Ustunlar tartibini ID lar ro'yxati orqali yangilash (oddiy massiv: ['col1', 'col2'])",
            description = "Ustunlar ID larining tartibli massivi orqali ustunlar indekslarini qayta belgilaydi."
    )
    @PatchMapping("/reorder")
    public ApiResponse<List<ColumnDto>> reorderColumnsByIds(
            @io.swagger.v3.oas.annotations.Parameter(description = "Ishchi maydon (workspace) ID si", example = "550e8400-e29b-41d4-a716-446655440000")
            @PathVariable String workspaceId,
            @RequestBody List<String> columnIds,
            @AuthenticationPrincipal User currentUser) {
        List<ColumnDto> columns = columnService.reorderColumnsByIds(workspaceId, columnIds, currentUser);
        return ApiResponse.success("Columnlar tartibi yangilandi", columns);
    }

    @Operation(
            operationId = "deleteWorkspaceColumn",
            summary = "Ustunni o'chirish",
            description = "Ustunni soft-delete qiladi. Ustun ichida vazifalar bo'lsa, xatolik berishi yoki vazifalarni boshqa joyga ko'chirish talab qilinishi mumkin."
    )
    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteColumn(
            @io.swagger.v3.oas.annotations.Parameter(description = "Ishchi maydon (workspace) ID si", example = "550e8400-e29b-41d4-a716-446655440000")
            @PathVariable String workspaceId,
            @io.swagger.v3.oas.annotations.Parameter(description = "Ustun (column) ID si", example = "col-uuid-123")
            @PathVariable String id,
            @AuthenticationPrincipal User currentUser) {
        columnService.deleteColumn(workspaceId, id, currentUser);
        return ApiResponse.success("Column o'chirildi", null);
    }
}
