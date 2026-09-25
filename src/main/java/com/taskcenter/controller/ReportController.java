package com.taskcenter.controller;

import com.taskcenter.dto.ApiResponse;
import com.taskcenter.dto.WorkspaceReportDto;
import com.taskcenter.model.User;
import com.taskcenter.service.ReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/workspaces/{workspaceId}/reports")
@Tag(name = "Reports", description = "Loyiha va xodimlar hisobotlari")
public class ReportController {

    private final ReportService reportService;

    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    @GetMapping("/summary")
    @Operation(summary = "Loyiha bo'yicha umumiy hisobot va xodimlar ish yukini olish")
    public ResponseEntity<ApiResponse<WorkspaceReportDto>> getWorkspaceSummary(
            @PathVariable String workspaceId,
            @AuthenticationPrincipal User currentUser) {
        
        WorkspaceReportDto report = reportService.getWorkspaceSummary(workspaceId, currentUser);
        return ResponseEntity.ok(ApiResponse.success("Hisobot muvaffaqiyatli olindi", report));
    }

    @GetMapping("/sprints/{sprintId}")
    @Operation(summary = "Muayyan Sprint bo'yicha ishlash tezligi (Burn-down) hisobotini olish")
    public ResponseEntity<ApiResponse<com.taskcenter.dto.SprintReportDto>> getSprintSummary(
            @PathVariable String workspaceId,
            @PathVariable String sprintId,
            @AuthenticationPrincipal User currentUser) {
        
        com.taskcenter.dto.SprintReportDto report = reportService.getSprintSummary(workspaceId, sprintId, currentUser);
        return ResponseEntity.ok(ApiResponse.success("Sprint hisoboti muvaffaqiyatli olindi", report));
    }
}
