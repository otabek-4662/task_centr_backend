package com.taskcenter.dto.admin;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminDashboardStatsDto {
    private long totalUsers;
    private long totalWorkspaces;
    private long totalTasks;
    private long completedTasks;
    private long openTasks;
    private long telegramLinkedUsers;
    private long totalAttachments;
    private long jvmUsedMemoryMb;
    private long jvmMaxMemoryMb;
    private long uptimeSeconds;
    private String botStatus;
}
