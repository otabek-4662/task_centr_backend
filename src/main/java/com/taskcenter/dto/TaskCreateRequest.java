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
    @NotBlank(message = "Ustun tanlanishi shart")
    private String columnId;

    @Schema(description = "Vazifa nomi", example = "Swagger UI integratsiyasini ulash")
    @NotBlank(message = "Vazifa (Bosh og'riq) sarlavhasi bo'sh bo'lishi mumkin emas")
    @Size(max = 255, message = "Sarlavha 255 belgidan oshmasligi kerak")
    private String title;

    @Schema(description = "Vazifa batafsil tavsifi", example = "Frontendda Swagger UI guruhlarini ko'rsatish")
    @Size(max = 5000, message = "Tavsif 5000 belgidan oshmasligi kerak")
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
    @Min(value = 0, message = "Vazifa bahosi 0 dan kam bo'lishi mumkin emas")
    @Max(value = 1000, message = "Vazifa bahosi 1000 dan oshmasligi kerak")
    private Integer storyPoints;

    @Schema(description = "Taxmin qilingan soatlar", example = "5.5")
    @Min(value = 0, message = "estimatedHours 0 dan kam bo'lishi mumkin emas")
    private Double estimatedHours;

    @Schema(description = "Sarflangan soatlar", example = "2.5")
    @Min(value = 0, message = "loggedHours 0 dan kam bo'lishi mumkin emas")
    private Double loggedHours;

    @Schema(hidden = true)
    @com.fasterxml.jackson.annotation.JsonIgnore
    private String sprintId;

    @Schema(description = "Biriktirilgan foydalanuvchilar ID lari", example = "[\"a1b2c3d4-e5f6-7890-abcd-ef1234567890\"]")
    private java.util.Set<String> assigneeIds;

    @Schema(description = "Teglar (Label) ID lari", example = "[\"label-123\"]")
    private java.util.Set<String> labelIds;
}
