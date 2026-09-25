package com.taskcenter.dto;

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
    private String sprintId;
    private String sprintName;
    private String status; // ACTIVE, COMPLETED, vb.
    
    private LocalDate startDate;
    private LocalDate endDate;

    private long totalTasks;
    private long completedTasks;
    
    private int totalStoryPoints;
    private int completedStoryPoints;

    private double completionPercentage; // Bajarilgan story point'lar asosida (yoki tasklar)
}
