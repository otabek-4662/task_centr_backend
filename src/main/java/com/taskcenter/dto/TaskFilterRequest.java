package com.taskcenter.dto;

import com.taskcenter.model.Priority;
import lombok.Data;

@Data
public class TaskFilterRequest {
    private String status;
    private Priority priority;
    private String assigneeId;
    private String columnId;
    private Boolean isOverdue;
    private Boolean dueToday;
    private Boolean assignedToMe;
    private Boolean dueThisWeek;
    private Boolean includeArchived;
    private String currentUserId;
    private java.time.LocalDate endOfWeek;
}
