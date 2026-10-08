package com.taskcenter.dto;

import com.taskcenter.model.IssueType;
import com.taskcenter.model.Priority;
import com.taskcenter.model.Task;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaskDto {
    @Schema(description = "Vazifaning unikal identifikatori (UUID)", example = "f47ac10b-58cc-4372-a567-0e02b2c3d479")
    private String id;

    @Schema(description = "Qisqa inson o'qiy oladigan identifikator", example = "PROJ-12")
    private String publicId;

    @Schema(description = "Ustun identifikatori", example = "col-12345")
    private String columnId;

    @Schema(description = "Ishchi maydon identifikatori", example = "ws-12345")
    private String workspaceId;

    @Schema(description = "Vazifa sarlavhasi", example = "Swagger UI integratsiyasini ulash")
    private String title;

    @Schema(description = "Vazifa batafsil tavsifi", example = "Frontendda Swagger UI guruhlarini ko'rsatish")
    private String description;

    @Schema(description = "LexoRank saralash tartibi", example = "0|hzzzzz:")
    private String lexoRank;

    @Schema(description = "Muhimlik darajasi", example = "HIGH")
    private Priority priority;

    @Schema(description = "Vazifa turi", example = "TASK")
    private IssueType issueType;

    @Schema(description = "Muddati (YYYY-MM-DD)", example = "2026-10-15")
    private LocalDate dueDate;

    @Schema(description = "Story Points (Agile bahosi)", example = "5")
    private Integer storyPoints;

    @Schema(description = "Taxmin qilingan soatlar", example = "4.5")
    private Double estimatedHours;

    @Schema(description = "Sarflangan soatlar", example = "2.0")
    private Double loggedHours;

    @Schema(description = "Muddati o'tib ketganmi", example = "false")
    private Boolean isOverdue;

    @Schema(description = "Checklist elementlarining umumiy soni", example = "4")
    private Integer totalChecklistItems;

    @Schema(description = "Bajarilgan checklist elementlari soni", example = "2")
    private Integer completedChecklistItems;

    @Schema(description = "Biriktirilgan sprint ID si", example = "sprint-67890")
    private String sprintId;

    @Schema(description = "Vazifaga biriktirilgan teglar")
    private List<LabelDto> labels;

    @Schema(description = "Vazifaga biriktirilgan yo'nalishlar (Frontend, Backend, QA va h.k.)")
    private List<DirectionDto> directions;

    @Schema(description = "Vazifaga biriktirilgan ijrochilar")
    private List<UserDto> assignees;

    @Schema(description = "Kuzatuvchilar ro'yxati")
    private List<UserDto> watchers;

    @Schema(description = "Arxivlanganmi", example = "false")
    private Boolean isArchived;

    @Schema(description = "Yaratilgan vaqt (ISO-8601)", example = "2026-10-08T15:30:00")
    private java.time.LocalDateTime createdAt;

    @Schema(description = "So'nggi o'zgartirilgan vaqt (ISO-8601)", example = "2026-10-08T15:35:00")
    private java.time.LocalDateTime updatedAt;

    public static TaskDto fromEntity(Task t) {
        return TaskDto.builder()
                .id(t.getId())
                .publicId(t.getPublicId())
                .columnId(t.getColumnId())
                .workspaceId(t.getWorkspaceId())
                .title(t.getTitle())
                .description(t.getDescription())
                .lexoRank(t.getLexoRank())
                .priority(t.getPriority() != null ? t.getPriority() : Priority.MEDIUM)
                .issueType(t.getIssueType() != null ? t.getIssueType() : IssueType.TASK)
                .dueDate(t.getDueDate())
                .storyPoints(t.getStoryPoints())
                .estimatedHours(t.getEstimatedHours())
                .loggedHours(t.getLoggedHours())
                .isOverdue(t.getDueDate() != null && t.getDueDate().isBefore(LocalDate.now()))
                .totalChecklistItems(t.getChecklistItems() != null ? t.getChecklistItems().size() : 0)
                .completedChecklistItems(t.getChecklistItems() != null ? (int) t.getChecklistItems().stream().filter(com.taskcenter.model.TaskChecklistItem::getIsCompleted).count() : 0)
                .sprintId(t.getSprintId())
                .labels(t.getLabels() != null ? t.getLabels().stream().map(LabelDto::fromEntity).collect(Collectors.toList()) : List.of())
                .directions(t.getDirections() != null ? t.getDirections().stream().map(DirectionDto::fromEntity).collect(Collectors.toList()) : List.of())
                .assignees(t.getAssignees() != null ? t.getAssignees().stream().map(UserDto::fromEntity).collect(Collectors.toList()) : List.of())
                .watchers(t.getWatchers() != null ? t.getWatchers().stream().map(UserDto::fromEntity).collect(Collectors.toList()) : List.of())
                .isArchived(t.getIsArchived())
                .createdAt(t.getCreatedAt())
                .updatedAt(t.getUpdatedAt())
                .build();
    }
}

