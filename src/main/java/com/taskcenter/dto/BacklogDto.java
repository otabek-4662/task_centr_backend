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
public class BacklogDto {
    @Schema(description = "Beklogdagi vazifalar ro'yxati")
    private List<TaskDto> tasks;

    @Schema(description = "Beklogdagi jami vazifalar soni", example = "25")
    private long totalTasks;

    @Schema(description = "Beklogdagi umumiy Story Points", example = "80")
    private int totalStoryPoints;

    @Schema(description = "Hozirgi sahifa raqami", example = "0")
    private int currentPage;

    @Schema(description = "Jami sahifalar soni", example = "2")
    private int totalPages;
}
