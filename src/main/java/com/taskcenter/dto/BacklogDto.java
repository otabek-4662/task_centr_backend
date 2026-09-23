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
public class BacklogDto {
    private List<TaskDto> tasks;
    private long totalTasks;
    private int totalStoryPoints;
    private int currentPage;
    private int totalPages;
}
