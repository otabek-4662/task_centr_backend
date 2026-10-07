package com.taskcenter.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;

@Data
public class SprintUpdateRequest {
    @Schema(description = "Sprint yangi nomi", example = "Sprint 1 — MVP (Kengaytirilgan)")
    @Size(max = 100, message = "Sprint nomi 100 belgidan oshmasligi kerak")
    private String name;

    @Schema(description = "Sprint yangi maqsadi", example = "Yangi talablarga muvofiq yangilangan maqsad")
    @Size(max = 2000, message = "Sprint maqsadi 2000 belgidan oshmasligi kerak")
    private String goal;

    @Schema(description = "Yangi boshlanish sanasi (YYYY-MM-DD)", example = "2026-10-01")
    private LocalDate startDate;

    @Schema(description = "Yangi tugash sanasi (YYYY-MM-DD)", example = "2026-10-18")
    private LocalDate endDate;
}
