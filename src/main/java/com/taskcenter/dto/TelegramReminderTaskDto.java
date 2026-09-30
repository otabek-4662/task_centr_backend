package com.taskcenter.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TelegramReminderTaskDto implements TelegramReminderTaskProjection {
    private String taskId;
    private String title;
    private String workspaceId;
    private LocalDate dueDate;
    private String userId;
    private Long telegramChatId;

    public String taskId() {
        return taskId;
    }

    public String title() {
        return title;
    }

    public String workspaceId() {
        return workspaceId;
    }

    public LocalDate dueDate() {
        return dueDate;
    }

    public String userId() {
        return userId;
    }

    public Long telegramChatId() {
        return telegramChatId;
    }
}
