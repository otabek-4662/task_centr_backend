package com.taskcenter.dto;

import com.taskcenter.model.BoardColumn;
import com.taskcenter.model.Task;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import java.util.List;
import java.util.stream.Collectors;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ColumnWithCardsDto {
    @Schema(description = "Ustun identifikatori (UUID)", example = "col-12345")
    private String id;

    @Schema(description = "Ustun nomi", example = "To Do")
    private String title;

    @Schema(description = "Ustun tartib indeksi", example = "0")
    private Integer order;

    @Schema(description = "Ustundagi vazifa kartalari ro'yxati")
    private List<TaskCardDto> cards;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TaskCardDto {
        @Schema(description = "Vazifa unikal identifikatori", example = "f47ac10b-58cc-4372-a567-0e02b2c3d479")
        private String id;

        @Schema(description = "Qisqa identifikator", example = "PROJ-1")
        private String publicId;

        @Schema(description = "Vazifa sarlavhasi", example = "Dastlabki sozlash")
        private String title;

        @Schema(description = "LexoRank qiymati", example = "0|hzzzzz:")
        private String lexoRank;

        @Schema(description = "Muhimlik darajasi", example = "HIGH")
        private com.taskcenter.model.Priority priority;

        @Schema(description = "Vazifa turi", example = "TASK")
        private com.taskcenter.model.IssueType issueType;

        @Schema(description = "Muddati (YYYY-MM-DD)", example = "2026-10-10")
        private java.time.LocalDate dueDate;

        @Schema(description = "Story Points bahosi", example = "3")
        private Integer storyPoints;

        @Schema(hidden = true)
        @com.fasterxml.jackson.annotation.JsonIgnore
        private String sprintId;

        @Schema(description = "Vazifaga biriktirilgan teglar")
        private List<LabelDto> labels;

        @Schema(description = "Biriktirilgan ijrochilar")
        private List<UserDto> assignees;

        public static TaskCardDto fromEntity(Task t) {
            return TaskCardDto.builder()
                    .id(t.getId())
                    .publicId(t.getPublicId())
                    .title(t.getTitle())
                    .lexoRank(t.getLexoRank())
                    .priority(t.getPriority() != null ? t.getPriority() : com.taskcenter.model.Priority.MEDIUM)
                    .issueType(t.getIssueType() != null ? t.getIssueType() : com.taskcenter.model.IssueType.TASK)
                    .dueDate(t.getDueDate())
                    .storyPoints(t.getStoryPoints())
                    .sprintId(t.getSprintId())
                    .labels(t.getLabels() != null ? t.getLabels().stream().map(LabelDto::fromEntity).collect(Collectors.toList()) : List.of())
                    .assignees(t.getAssignees() != null ? t.getAssignees().stream().map(UserDto::fromEntity).collect(Collectors.toList()) : List.of())
                    .build();
        }
    }

    public static ColumnWithCardsDto fromEntity(BoardColumn c, List<Task> tasks) {
        return ColumnWithCardsDto.builder()
                .id(c.getId())
                .title(c.getTitle())
                .order(c.getOrder())
                .cards(tasks.stream().map(TaskCardDto::fromEntity).collect(Collectors.toList()))
                .build();
    }
}
