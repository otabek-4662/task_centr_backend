package com.taskcenter.dto;

import com.taskcenter.model.TaskActivity;
import com.taskcenter.model.TaskActivityType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaskActivityDto {
    private String id;
    private String taskId;
    private String userId;
    private String userName;
    private String userFullName;
    private TaskActivityType actionType;
    private String fieldName;
    private String oldValue;
    private String newValue;
    private LocalDateTime createdAt;

    public static TaskActivityDto fromEntity(TaskActivity a) {
        String userName = a.getUser() != null ? a.getUser().getName() : null;
        String userFullName = a.getUser() != null ? a.getUser().getFullName() : null;
        return TaskActivityDto.builder()
                .id(a.getId())
                .taskId(a.getTaskId())
                .userId(a.getUserId())
                .userName(userName)
                .userFullName(userFullName != null ? userFullName : userName)
                .actionType(a.getActionType())
                .fieldName(a.getFieldName())
                .oldValue(a.getOldValue())
                .newValue(a.getNewValue())
                .createdAt(a.getCreatedAt())
                .build();
    }
}
