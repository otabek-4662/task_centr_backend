package com.taskcenter.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VelocityChartDto {
    private String workspaceId;
    private List<SprintVelocityDto> sprints;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SprintVelocityDto {
        private String sprintId;
        private String sprintName;
        private int completedStoryPoints;
    }
}
