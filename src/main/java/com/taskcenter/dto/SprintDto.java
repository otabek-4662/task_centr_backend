package com.taskcenter.dto;

import com.taskcenter.model.Sprint;
import com.taskcenter.model.SprintStatus;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SprintDto {
    private String id;
    private String workspaceId;
    private String name;
    private String goal;
    private SprintStatus status;
    private LocalDate startDate;
    private LocalDate endDate;
    private Long taskCount;
    private Integer totalStoryPoints;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static SprintDto fromEntity(Sprint s) {
        return SprintDto.builder()
                .id(s.getId())
                .workspaceId(s.getWorkspaceId())
                .name(s.getName())
                .goal(s.getGoal())
                .status(s.getStatus())
                .startDate(s.getStartDate())
                .endDate(s.getEndDate())
                .createdAt(s.getCreatedAt())
                .updatedAt(s.getUpdatedAt())
                .build();
    }

    public static SprintDto fromEntity(Sprint s, long taskCount, int totalStoryPoints) {
        return SprintDto.builder()
                .id(s.getId())
                .workspaceId(s.getWorkspaceId())
                .name(s.getName())
                .goal(s.getGoal())
                .status(s.getStatus())
                .startDate(s.getStartDate())
                .endDate(s.getEndDate())
                .taskCount(taskCount)
                .totalStoryPoints(totalStoryPoints)
                .createdAt(s.getCreatedAt())
                .updatedAt(s.getUpdatedAt())
                .build();
    }
}
