package com.taskcenter.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkspaceReportDto {
    @Schema(description = "Ishchi maydondagi jami vazifalar soni", example = "50")
    private long totalTasks;

    @Schema(description = "Bajarilishi kerak bo'lgan (To Do) vazifalar soni", example = "20")
    private long todoTasks;

    @Schema(description = "Jarayondagi (In Progress) vazifalar soni", example = "15")
    private long inProgressTasks;

    @Schema(description = "Bajarilgan (Done) vazifalar soni", example = "15")
    private long doneTasks;
    
    // Foizda ko'rsatish uchun (masalan 45.5%)
    @Schema(description = "Vazifalar bajarilish foizi", example = "30.0")
    private double completionPercentage;

    @Schema(description = "Foydalanuvchilar bo'yicha ish hajmi taqsimoti")
    private List<UserWorkloadDto> workload;
}
