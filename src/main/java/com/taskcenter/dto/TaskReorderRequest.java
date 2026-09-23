package com.taskcenter.dto;

import lombok.Data;

@Data
public class TaskReorderRequest {
    private String columnId; // if moving to a different column
    private String prevRank;
    private String nextRank;
}
