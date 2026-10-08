package com.taskcenter.controller;

import com.taskcenter.model.User;
import com.taskcenter.service.ExportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;

@Tag(name = "Export", description = "Vazifalarni CSV ga yuklab olish")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/workspaces/{workspaceId}/export")
public class ExportController {

    private final ExportService exportService;

    public ExportController(ExportService exportService) {
        this.exportService = exportService;
    }

    @Operation(
            operationId = "exportWorkspaceTasksToCsv",
            summary = "Workspace dagi barcha vazifalarni CSV formatida yuklash",
            description = "Ishchi maydonga tegishli barcha vazifalarni (sarlavhasi, ustuni, ijrochilari, muhimligi va h.k.) CSV fayl formatida yuklab olish uchun taqdim etadi."
    )
    @GetMapping("/csv")
    public ResponseEntity<Resource> exportCsv(
            @io.swagger.v3.oas.annotations.Parameter(description = "Ishchi maydon (workspace) ID si", example = "550e8400-e29b-41d4-a716-446655440000")
            @PathVariable String workspaceId,
            @AuthenticationPrincipal User currentUser) {
        
        String csvData = exportService.exportWorkspaceTasksToCsv(workspaceId, currentUser);
        byte[] data = csvData.getBytes(StandardCharsets.UTF_8);
        ByteArrayResource resource = new ByteArrayResource(data);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"workspace-" + workspaceId + "-tasks.csv\"")
                .contentType(MediaType.parseMediaType("text/csv"))
                .contentLength(data.length)
                .body(resource);
    }
}
