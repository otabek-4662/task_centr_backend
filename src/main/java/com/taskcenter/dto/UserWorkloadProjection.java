package com.taskcenter.dto;

public interface UserWorkloadProjection {
    String getUserId();
    String getUserName();
    String getUserFullName();
    Long getTotalTasks();
    Long getCompletedTasks();
    Long getActiveTasks();
}
