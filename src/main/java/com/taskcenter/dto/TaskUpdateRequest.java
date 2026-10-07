package com.taskcenter.dto;

import com.taskcenter.model.IssueType;
import com.taskcenter.model.Priority;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;

@Data
public class TaskUpdateRequest {
    @Schema(description = "Yangi sarlavha", example = "Yangi vazifa nomi")
    @jakarta.validation.constraints.Pattern(regexp = "^.*\\S.*$", message = "title faqat bo'sh joylardan iborat bo'lishi mumkin emas")
    @Size(max = 255, message = "title bo'sh bo'lmasligi va 255 belgidan oshmasligi kerak")
    private String title;

    @Schema(description = "Yangi batafsil tavsif", example = "Vazifaning yangilangan tafsilotlari")
    @Size(max = 5000, message = "description 5000 belgidan oshmasligi kerak")
    private String description;

    @Schema(description = "Ko'chiriladigan ustun ID si", example = "col-123")
    private String columnId;

    @Schema(description = "LexoRank yangi tartib qiymati", example = "0|hzzzzz:")
    private String lexoRank;

    @Schema(description = "Yangi muhimlik darajasi", example = "HIGH")
    private Priority priority;

    @Schema(description = "Yangi vazifa turi", example = "TASK")
    private IssueType issueType;

    @Schema(description = "Yangi tugash muddati (YYYY-MM-DD)", example = "2026-10-20")
    private LocalDate dueDate;

    @Schema(description = "Mavjud muddatni o'chirib tashlash bayrog'i", example = "false")
    private Boolean clearDueDate;

    @Schema(description = "Story Points (Agile bahosi)", example = "8")
    @Min(value = 0, message = "storyPoints 0 dan kam bo'lishi mumkin emas")
    @Max(value = 1000, message = "storyPoints 1000 dan oshmasligi kerak")
    private Integer storyPoints;

    @Schema(description = "Taxmin qilingan soatlar", example = "6.0")
    @Min(value = 0, message = "estimatedHours 0 dan kam bo'lishi mumkin emas")
    private Double estimatedHours;

    @Schema(description = "Sarflangan soatlar", example = "3.5")
    @Min(value = 0, message = "loggedHours 0 dan kam bo'lishi mumkin emas")
    private Double loggedHours;

    @Schema(hidden = true)
    @com.fasterxml.jackson.annotation.JsonIgnore
    private String sprintId;

    @Schema(description = "Biriktirilgan foydalanuvchilar ID lari (to'liq sinxron yangilash)")
    private java.util.Set<String> assigneeIds;

    @Schema(description = "Teglar (Label) ID lari (to'liq sinxron yangilash)")
    private java.util.Set<String> labelIds;
}
