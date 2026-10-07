package com.taskcenter.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SprintReportDto {
    @Schema(description = "Sprint identifikatori (UUID)", example = "spr-12345")
    private String sprintId;

    @Schema(description = "Sprint nomi", example = "Sprint 1")
    private String sprintName;

    @Schema(description = "Sprint holati (PLANNED, ACTIVE, COMPLETED)", example = "COMPLETED")
    private String status; // ACTIVE, COMPLETED, vb.
    
    @Schema(description = "Boshlanish sanasi (YYYY-MM-DD)", example = "2026-10-01")
    private LocalDate startDate;

    @Schema(description = "Tugash sanasi (YYYY-MM-DD)", example = "2026-10-14")
    private LocalDate endDate;

    @Schema(description = "Sprintdagi jami vazifalar soni", example = "20")
    private long totalTasks;

    @Schema(description = "Bajarilgan vazifalar soni", example = "18")
    private long completedTasks;
    
    @Schema(description = "Jami Story Points bahosi", example = "60")
    private int totalStoryPoints;

    @Schema(description = "Bajarilgan Story Points bahosi", example = "54")
    private int completedStoryPoints;

    @Schema(description = "Bajarilish foizi (Story Points asosida)", example = "90.0")
    private double completionPercentage; // Bajarilgan story point'lar asosida (yoki tasklar)
}
