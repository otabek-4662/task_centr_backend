package com.taskcenter.dto;

import com.taskcenter.model.IssueType;
import com.taskcenter.model.Priority;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;

@Data
public class TaskCreateRequest {
    @Schema(description = "Vazifa joylashadigan ustun ID si", example = "col-12345")
    @NotBlank(message = "columnId bo'sh bo'lishi mumkin emas")
    private String columnId;

    @Schema(description = "Vazifa nomi", example = "Swagger UI integratsiyasini ulash")
    @NotBlank(message = "title bo'sh bo'lishi mumkin emas")
    @Size(max = 255, message = "title 255 belgidan oshmasligi kerak")
    private String title;

    @Schema(description = "Vazifa batafsil tavsifi", example = "Frontendda Swagger UI guruhlarini ko'rsatish")
    @Size(max = 5000, message = "description 5000 belgidan oshmasligi kerak")
    private String description;

    @Schema(description = "Lexorank saralash qiymati", example = "0|hzzzzz:")
    private String lexoRank;

    @Schema(description = "Muhimlik darajasi", example = "HIGH")
    private Priority priority;

    @Schema(description = "Vazifa turi (TASK, BUG, STORY, EPIC)", example = "TASK")
    private IssueType issueType;

    @Schema(description = "Tugash muddati (YYYY-MM-DD)", example = "2026-10-01")
    private LocalDate dueDate;

    @Schema(description = "Story Points (Agile ball)", example = "5")
    @Min(value = 0, message = "storyPoints 0 dan kam bo'lishi mumkin emas")
    @Max(value = 1000, message = "storyPoints 1000 dan oshmasligi kerak")
    private Integer storyPoints;

    @Schema(description = "Biriktirilgan sprint ID si", example = "sprint-67890")
    private String sprintId;
}
