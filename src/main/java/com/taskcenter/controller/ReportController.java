package com.taskcenter.controller;

import com.taskcenter.dto.ApiResponse;
import com.taskcenter.dto.WorkspaceReportDto;
import com.taskcenter.model.User;
import com.taskcenter.service.ReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/workspaces/{workspaceId}/reports")
@Tag(name = "Reports", description = "Loyiha va xodimlar hisobotlari")
@SecurityRequirement(name = "bearerAuth")
public class ReportController {

    private final ReportService reportService;

    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    @GetMapping("/summary")
    @Operation(
            operationId = "getWorkspaceSummaryReport",
            summary = "Loyiha bo'yicha umumiy hisobot va xodimlar ish yukini olish",
            description = "Ishchi maydondagi jami vazifalar, ustunlar bo'yicha taqsimot va har bir xodimning ustidagi faol vazifalar soni (workload) hisobotini beradi."
    )
    public ResponseEntity<ApiResponse<WorkspaceReportDto>> getWorkspaceSummary(
            @io.swagger.v3.oas.annotations.Parameter(description = "Ishchi maydon (workspace) ID si", example = "550e8400-e29b-41d4-a716-446655440000")
            @PathVariable String workspaceId,
            @AuthenticationPrincipal User currentUser) {
        
        WorkspaceReportDto report = reportService.getWorkspaceSummary(workspaceId, currentUser);
        return ResponseEntity.ok(ApiResponse.success("Hisobot muvaffaqiyatli olindi", report));
    }

    @GetMapping("/sprints/{sprintId}")
    @Operation(
            operationId = "getSprintSummaryReport",
            summary = "Muayyan Sprint bo'yicha ishlash tezligi (Burn-down) hisobotini olish",
            description = "Tanlangan sprint doirasida vazifalarning holati, tugallanganlik foizi va jamoa tezligi tahlilini qaytaradi."
    )
    public ResponseEntity<ApiResponse<com.taskcenter.dto.SprintReportDto>> getSprintSummary(
            @io.swagger.v3.oas.annotations.Parameter(description = "Ishchi maydon (workspace) ID si", example = "550e8400-e29b-41d4-a716-446655440000")
            @PathVariable String workspaceId,
            @io.swagger.v3.oas.annotations.Parameter(description = "Sprint ID si", example = "sprint-uuid-123")
            @PathVariable String sprintId,
            @AuthenticationPrincipal User currentUser) {
        
        com.taskcenter.dto.SprintReportDto report = reportService.getSprintSummary(workspaceId, sprintId, currentUser);
        return ResponseEntity.ok(ApiResponse.success("Sprint hisoboti muvaffaqiyatli olindi", report));
    }
}
