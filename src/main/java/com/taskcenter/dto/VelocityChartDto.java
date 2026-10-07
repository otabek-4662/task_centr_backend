package com.taskcenter.dto;

import io.swagger.v3.oas.annotations.media.Schema;
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
    @Schema(description = "Ishchi maydon identifikatori (UUID)", example = "ws-12345")
    private String workspaceId;

    @Schema(description = "Sprintlar bo'yicha tezlik statistikasi ro'yxati")
    private List<SprintVelocityDto> sprints;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SprintVelocityDto {
        @Schema(description = "Sprint identifikatori (UUID)", example = "spr-12345")
        private String sprintId;

        @Schema(description = "Sprint nomi", example = "Sprint 1")
        private String sprintName;

        @Schema(description = "Bajarilgan Story Points ballari", example = "35")
        private int completedStoryPoints;
    }
}
