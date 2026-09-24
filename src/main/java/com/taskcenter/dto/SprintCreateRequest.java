package com.taskcenter.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;

@Data
public class SprintCreateRequest {
    @Schema(description = "Sprint nomi", example = "Sprint 1 — MVP")
    @NotBlank(message = "Sprint nomi bo'sh bo'lishi mumkin emas")
    @Size(max = 100, message = "Sprint nomi 100 belgidan oshmasligi kerak")
    private String name;

    @Schema(description = "Sprint maqsadi (Goal)", example = "Foydalanuvchi avtorizatsiyasi va vazifalar doskasini yakunlash")
    @Size(max = 2000, message = "Sprint maqsadi 2000 belgidan oshmasligi kerak")
    private String goal;

    @Schema(description = "Boshlanish sanasi (YYYY-MM-DD)", example = "2026-10-01")
    private LocalDate startDate;

    @Schema(description = "Tugash sanasi (YYYY-MM-DD)", example = "2026-10-14")
    private LocalDate endDate;
}
