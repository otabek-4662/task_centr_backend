package com.taskcenter.dto;

public interface SprintStatsProjection {
    String getSprintId();
    Long getTaskCount();
    Integer getTotalStoryPoints();
}
