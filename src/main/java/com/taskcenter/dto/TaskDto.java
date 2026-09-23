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
    private String sprintId;
    private List<LabelDto> labels;
    private List<UserDto> assignees;

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
                .sprintId(t.getSprintId())
                .labels(t.getLabels() != null ? t.getLabels().stream().map(LabelDto::fromEntity).collect(Collectors.toList()) : List.of())
                .assignees(t.getAssignees() != null ? t.getAssignees().stream().map(UserDto::fromEntity).collect(Collectors.toList()) : List.of())
                .build();
    }
}
