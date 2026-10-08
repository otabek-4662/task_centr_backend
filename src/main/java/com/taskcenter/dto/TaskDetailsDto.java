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
public class TaskDetailsDto {
    @Schema(description = "Vazifa ma'lumotlari")
    private TaskDto task;

    @Schema(description = "Vazifa izohlari")
    private List<CommentDto> comments;

    @Schema(description = "Checklist elementlari")
    private List<ChecklistItemDto> checklists;
}
