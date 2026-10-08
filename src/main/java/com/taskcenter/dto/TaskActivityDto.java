package com.taskcenter.dto;

import com.taskcenter.model.TaskActivity;
import com.taskcenter.model.TaskActivityType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Vazifa o'zgarishlar tarixi (Audit log / Activity)")
public class TaskActivityDto {
    @Schema(description = "Faoliyat yozuvi unikal ID si", example = "act-12345")
    private String id;

    @Schema(description = "Vazifa ID si", example = "task-uuid-123")
    private String taskId;

    @Schema(description = "Amalni bajargan foydalanuvchi ID si", example = "usr-uuid-123")
    private String userId;

    @Schema(description = "Foydalanuvchi logini", example = "xusanboy")
    private String userName;

    @Schema(description = "Foydalanuvchi to'liq ismi", example = "Xusanboy Abdullayev")
    private String userFullName;

    @Schema(description = "Amal turi", example = "STATUS_CHANGED")
    private TaskActivityType actionType;

    @Schema(description = "O'zgargan maydon nomi", example = "status")
    private String fieldName;

    @Schema(description = "Eski qiymat", example = "IN_PROGRESS")
    private String oldValue;

    @Schema(description = "Yangi qiymat", example = "DONE")
    private String newValue;

    @Schema(description = "Amal bajarilgan vaqt (ISO-8601)", example = "2026-10-08T15:30:00")
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
