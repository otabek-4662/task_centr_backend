package com.taskcenter.service;

import com.taskcenter.dto.TaskActivityDto;
import com.taskcenter.dto.admin.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface AdminService {
    AdminDashboardStatsDto getDashboardStats();

    Page<AdminUserDto> getUsers(String search, Pageable pageable);

    AdminUserDto updateUserRole(String userId, AdminUserRoleRequest request);

    void deleteUser(String userId);

    Page<AdminWorkspaceDto> getWorkspaces(String search, Pageable pageable);

    void deleteWorkspace(String workspaceId);

    Page<TaskActivityDto> getAuditLogs(Pageable pageable);

    AdminBroadcastResultDto broadcastTelegramMessage(AdminBroadcastRequest request);

    List<AdminSystemJobDto> getSystemJobs();

    void triggerSystemJob(String jobKey);

    void evictAllCaches();
}
