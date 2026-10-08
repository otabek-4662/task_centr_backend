package com.taskcenter.dto;

import com.taskcenter.model.TaskChecklistItem;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@Schema(description = "Checklist bandi ma'lumotlari")
public class ChecklistItemDto {
    @Schema(description = "Checklist identifikatori (UUID)", example = "chk-123")
    private String id;

    @Schema(description = "Tegishli vazifa identifikatori (UUID)", example = "tsk-123")
    private String taskId;

    @Schema(description = "Checklist matni", example = "Integratsion test yozish")
    private String title;

    @Schema(description = "Bajarilganmi", example = "false")
    private Boolean isCompleted;

    @Schema(description = "Tartib raqami", example = "1")
    private Integer orderIndex;

    public static ChecklistItemDto fromEntity(TaskChecklistItem item) {
        return ChecklistItemDto.builder()
                .id(item.getId())
                .taskId(item.getTaskId())
                .title(item.getTitle())
                .isCompleted(item.getIsCompleted())
                .orderIndex(item.getOrderIndex())
                .build();
    }
}
