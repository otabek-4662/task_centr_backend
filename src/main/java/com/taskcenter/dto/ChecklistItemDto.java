package com.taskcenter.dto;

import com.taskcenter.model.TaskChecklistItem;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ChecklistItemDto {
    private String id;
    private String taskId;
    private String title;
    private Boolean isCompleted;
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
