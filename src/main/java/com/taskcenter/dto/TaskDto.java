package com.taskcenter.dto;

import com.taskcenter.model.IssueType;
import com.taskcenter.model.Priority;
import com.taskcenter.model.Task;
import lombok.*;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaskDto {
    private String id;
    private String publicId;
    private String columnId;
    private String workspaceId;
    private String title;
    private String description;
    private String lexoRank;
    private Priority priority;
    private IssueType issueType;
    private LocalDate dueDate;
    private Integer storyPoints;
    private Double estimatedHours;
    private Double loggedHours;
    private Boolean isOverdue;
    private Integer totalChecklistItems;
    private Integer completedChecklistItems;
    @io.swagger.v3.oas.annotations.media.Schema(hidden = true)
    @com.fasterxml.jackson.annotation.JsonIgnore
    private String sprintId;
    private List<LabelDto> labels;
    private List<UserDto> assignees;
    private List<UserDto> watchers;
    private Boolean isArchived;

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
                .assignees(t.getAssignees() != null ? t.getAssignees().stream().map(UserDto::fromEntity).collect(Collectors.toList()) : List.of())
                .watchers(t.getWatchers() != null ? t.getWatchers().stream().map(UserDto::fromEntity).collect(Collectors.toList()) : List.of())
                .isArchived(t.getIsArchived())
                .build();
    }
}
