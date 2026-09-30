package com.taskcenter.dto;

import java.time.LocalDate;

public interface TelegramReminderTaskProjection {
    String getTaskId();
    String getTitle();
    String getWorkspaceId();
    LocalDate getDueDate();
    String getUserId();
    Long getTelegramChatId();
}
