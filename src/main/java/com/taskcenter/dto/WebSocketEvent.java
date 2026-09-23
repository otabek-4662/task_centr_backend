package com.taskcenter.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WebSocketEvent<T> {
    private String type; // TASK_CREATED, TASK_UPDATED, TASK_DELETED, TASKS_REORDERED
    private String workspaceId;
    private T data;
}
