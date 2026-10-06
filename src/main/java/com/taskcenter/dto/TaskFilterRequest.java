package com.taskcenter.dto;

import com.taskcenter.model.IssueType;
import com.taskcenter.model.Priority;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDate;

@Data
public class TaskFilterRequest {
    @Schema(description = "Vazifa nomi, kodi yoki tavsifi bo'yicha qidirish", example = "Swagger")
    private String search;

    @Schema(description = "Qidiruv so'zi (search parametri bilan bir xil)", example = "Swagger")
    private String q;

    @Schema(description = "Ustun (status) bo'yicha filter", example = "col-123")
    private String status;

    @Schema(description = "Muhimlik darajasi bo'yicha filter", example = "HIGH")
    private Priority priority;

    @Schema(description = "Vazifa turi bo'yicha filter", example = "BUG")
    private IssueType issueType;

    @Schema(hidden = true)
    @com.fasterxml.jackson.annotation.JsonIgnore
    private String sprintId;

    @Schema(description = "Biriktirilgan foydalanuvchi ID si", example = "user-123")
    private String assigneeId;

    @Schema(description = "Ustun ID si", example = "col-123")
    private String columnId;

    @Schema(description = "Muddati o'tib ketgan vazifalar", example = "true")
    private Boolean isOverdue;

    @Schema(description = "Bugun qilinishi kerak bo'lgan vazifalar", example = "true")
    private Boolean dueToday;

    @Schema(description = "Faqat menga biriktirilgan vazifalar", example = "true")
    private Boolean assignedToMe;

    @Schema(description = "Shu hafta qilinishi kerak bo'lgan vazifalar", example = "true")
    private Boolean dueThisWeek;

    @Schema(description = "Arxivlangan vazifalarni ham qo'shish", example = "false")
    private Boolean includeArchived;

    @Schema(description = "Workspace ID bo'yicha filter", example = "ws-123")
    private String workspaceId;

    private String currentUserId;
    private LocalDate endOfWeek;
    private LocalDate today;
}
