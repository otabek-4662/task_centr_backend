package com.taskcenter.dto;

public interface UserTaskStatsProjection {
    Long getTotalAssigned();
    Long getCompletedCount();
    Long getActiveCount();
    Long getOverdueCount();
    Long getDueTodayCount();
}
