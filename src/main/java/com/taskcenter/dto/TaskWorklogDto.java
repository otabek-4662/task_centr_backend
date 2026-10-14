package com.taskcenter.dto;

import com.taskcenter.model.TaskWorklog;
import com.taskcenter.model.User;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaskWorklogDto {
    @Schema(description = "Worklog yozuvi identifikatori (UUID)")
    private String id;

    @Schema(description = "Vazifa identifikatori")
    private String taskId;

    @Schema(description = "Ishchi maydon identifikatori")
    private String workspaceId;

    @Schema(description = "Foydalanuvchi identifikatori")
    private String userId;

    @Schema(description = "Foydalanuvchi to'liq ismi")
    private String userName;

    @Schema(description = "Sarflangan soatlar", example = "2.5")
    private Double hours;

    @Schema(description = "Bajarilgan ish tavsifi")
    private String description;

    @Schema(description = "Ish bajarilgan sana", example = "2026-10-14")
    private LocalDate workDate;

    @Schema(description = "Yaratilgan vaqt")
    private LocalDateTime createdAt;

    public static TaskWorklogDto fromEntity(TaskWorklog w, User user) {
        if (w == null) return null;
        String displayName = w.getUserId();
        if (user != null) {
            displayName = (user.getFullName() != null && !user.getFullName().isBlank())
                    ? user.getFullName()
                    : user.getName();
        }
        return TaskWorklogDto.builder()
                .id(w.getId())
                .taskId(w.getTaskId())
                .workspaceId(w.getWorkspaceId())
                .userId(w.getUserId())
                .userName(displayName)
                .hours(w.getHours())
                .description(w.getDescription())
                .workDate(w.getWorkDate())
                .createdAt(w.getCreatedAt())
                .build();
    }
}
