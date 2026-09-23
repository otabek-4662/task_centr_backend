package com.taskcenter.dto;

import com.taskcenter.model.BoardColumn;
import com.taskcenter.model.Task;
import lombok.*;
import java.util.List;
import java.util.stream.Collectors;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ColumnWithCardsDto {
    private String id;
    private String title;
    private Integer order;
    private List<TaskCardDto> cards;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TaskCardDto {
        private String id;
        private String publicId;
        private String title;
        private String lexoRank;
        private com.taskcenter.model.Priority priority;
        private com.taskcenter.model.IssueType issueType;
        private java.time.LocalDate dueDate;
        private Integer storyPoints;
        private String sprintId;
        private List<LabelDto> labels;
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
