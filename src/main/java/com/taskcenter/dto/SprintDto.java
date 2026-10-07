package com.taskcenter.dto;

import com.taskcenter.model.Sprint;
import com.taskcenter.model.SprintStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SprintDto {
    @Schema(description = "Sprint identifikatori (UUID)", example = "spr-12345")
    private String id;

    @Schema(description = "Ishchi maydon identifikatori (UUID)", example = "ws-12345")
    private String workspaceId;

    @Schema(description = "Sprint nomi", example = "Sprint 1 — MVP")
    private String name;

    @Schema(description = "Sprint maqsadi (Goal)", example = "Asosiy funksionallikni ishga tushirish")
    private String goal;

    @Schema(description = "Sprint holati (PLANNED, ACTIVE, COMPLETED)", example = "ACTIVE")
    private SprintStatus status;

    @Schema(description = "Boshlanish sanasi (YYYY-MM-DD)", example = "2026-10-01")
    private LocalDate startDate;

    @Schema(description = "Tugash sanasi (YYYY-MM-DD)", example = "2026-10-14")
    private LocalDate endDate;

    @Schema(description = "Sprintdagi vazifalar soni", example = "12")
    private Long taskCount;

    @Schema(description = "Sprintdagi umumiy Story Points", example = "45")
    private Integer totalStoryPoints;

    @Schema(description = "Yaratilgan vaqti", example = "2026-09-30T10:00:00")
    private LocalDateTime createdAt;

    @Schema(description = "Oxirgi yangilangan vaqti", example = "2026-10-01T15:00:00")
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
