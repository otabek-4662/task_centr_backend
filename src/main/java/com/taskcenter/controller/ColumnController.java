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
@CrossOrigin(origins = "*")
public class ColumnController {

    private final ColumnService columnService;

    public ColumnController(ColumnService columnService) {
        this.columnService = columnService;
    }

    @Operation(summary = "Workspace ga tegishli ustunlar ro'yxatini olish")
    @GetMapping
    public ApiResponse<List<ColumnDto>> getColumns(
            @PathVariable String workspaceId,
            @AuthenticationPrincipal User currentUser) {
        List<ColumnDto> columns = columnService.getColumns(workspaceId, currentUser);
        return ApiResponse.success("ok", columns);
    }

    @Operation(summary = "Yangi ustun yaratish")
    @PostMapping
    public ApiResponse<ColumnDto> createColumn(
            @PathVariable String workspaceId,
            @Valid @RequestBody ColumnCreateRequest request,
            @AuthenticationPrincipal User currentUser) {
        ColumnDto column = columnService.createColumn(workspaceId, request, currentUser);
        return ApiResponse.success("Column yaratildi", column);
    }

    @Operation(summary = "Ustunni to'liq yangilash")
    @PutMapping("/{id}")
    public ApiResponse<ColumnDto> updateColumn(
            @PathVariable String workspaceId,
            @PathVariable String id,
            @RequestBody ColumnCreateRequest request,
            @AuthenticationPrincipal User currentUser) {
        ColumnDto column = columnService.updateColumn(workspaceId, id, request, currentUser);
        return ApiResponse.success("Column yangilandi", column);
    }

    @Operation(summary = "Ustunni qisman yangilash")
    @PatchMapping("/{id}")
    public ApiResponse<ColumnDto> patchColumn(
            @PathVariable String workspaceId,
            @PathVariable String id,
            @RequestBody ColumnPatchRequest request,
            @AuthenticationPrincipal User currentUser) {
        ColumnDto column = columnService.patchColumn(workspaceId, id, request, currentUser);
        return ApiResponse.success("Column yangilandi", column);
    }

    @Operation(summary = "Ustunlar tartibini o'zgartirish (reorder)")
    @PatchMapping
    public ApiResponse<List<ColumnDto>> reorderColumns(
            @PathVariable String workspaceId,
            @RequestBody List<ColumnReorderItem> items,
            @AuthenticationPrincipal User currentUser) {
        List<ColumnDto> columns = columnService.reorderColumns(workspaceId, items, currentUser);
        return ApiResponse.success("Columnlar tartibi yangilandi", columns);
    }

    @Operation(summary = "Ustunlar tartibini ID lar ro'yxati orqali yangilash (oddiy massiv: ['col1', 'col2'])")
    @PatchMapping("/reorder")
    public ApiResponse<List<ColumnDto>> reorderColumnsByIds(
            @PathVariable String workspaceId,
            @RequestBody List<String> columnIds,
            @AuthenticationPrincipal User currentUser) {
        List<ColumnDto> columns = columnService.reorderColumnsByIds(workspaceId, columnIds, currentUser);
        return ApiResponse.success("Columnlar tartibi yangilandi", columns);
    }

    @Operation(summary = "Ustunni o'chirish")
    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteColumn(
            @PathVariable String workspaceId,
            @PathVariable String id,
            @AuthenticationPrincipal User currentUser) {
        columnService.deleteColumn(workspaceId, id, currentUser);
        return ApiResponse.success("Column o'chirildi", null);
    }
}
