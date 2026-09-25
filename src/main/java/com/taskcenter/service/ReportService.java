package com.taskcenter.service;

import com.taskcenter.dto.UserWorkloadDto;
import com.taskcenter.dto.WorkspaceReportDto;
import com.taskcenter.model.BoardColumn;
import com.taskcenter.model.Task;
import com.taskcenter.model.User;
import com.taskcenter.repository.ColumnRepository;
import com.taskcenter.repository.TaskRepository;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class ReportService {

    private final TaskRepository taskRepository;
    private final ColumnRepository columnRepository;
    private final WorkspaceAuthorizationService authorizationService;
    private final com.taskcenter.repository.SprintRepository sprintRepository;

    public ReportService(TaskRepository taskRepository,
                         ColumnRepository columnRepository,
                         WorkspaceAuthorizationService authorizationService,
                         com.taskcenter.repository.SprintRepository sprintRepository) {
        this.taskRepository = taskRepository;
        this.columnRepository = columnRepository;
        this.authorizationService = authorizationService;
        this.sprintRepository = sprintRepository;
    }

    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public WorkspaceReportDto getWorkspaceSummary(String workspaceId, User currentUser) {
        // Ruxsatni tekshirish
        authorizationService.checkAccess(workspaceId, currentUser);

        // Workspace ustunlari
        List<BoardColumn> columns = columnRepository.findByWorkspaceIdOrderByOrderAsc(workspaceId);
        
        String todoColumnId = null;
        String inProgressColumnId = null;
        String doneColumnId = null;

        for (BoardColumn col : columns) {
            if (Boolean.TRUE.equals(col.getIsDefault())) {
                if (col.getOrder() == 1) todoColumnId = col.getId();
                else if (col.getOrder() == 2) inProgressColumnId = col.getId();
                else if (col.getOrder() == 3) doneColumnId = col.getId();
            }
        }

        // Barcha vazifalar
        List<Task> tasks = taskRepository.findByWorkspaceIdWithAssignees(workspaceId);

        long totalTasks = tasks.size();
        long todoTasks = 0;
        long inProgressTasks = 0;
        long doneTasks = 0;

        // Xodimlar bo'yicha hisob
        Map<String, UserWorkloadDto> workloadMap = new HashMap<>();

        for (Task task : tasks) {
            String colId = task.getColumnId();
            if (colId.equals(todoColumnId)) todoTasks++;
            else if (colId.equals(inProgressColumnId)) inProgressTasks++;
            else if (colId.equals(doneColumnId)) doneTasks++;

            boolean isDone = colId.equals(doneColumnId);

            // Har bir assigneeni hisoblash
            for (User assignee : task.getAssignees()) {
                UserWorkloadDto wDto = workloadMap.computeIfAbsent(assignee.getId(), id -> 
                    UserWorkloadDto.builder()
                        .userId(assignee.getId())
                        .userName(assignee.getUsername())
                        .userFullName(assignee.getFullName())
                        .totalAssignedTasks(0)
                        .completedTasks(0)
                        .activeTasks(0)
                        .build()
                );

                wDto.setTotalAssignedTasks(wDto.getTotalAssignedTasks() + 1);
                if (isDone) {
                    wDto.setCompletedTasks(wDto.getCompletedTasks() + 1);
                } else {
                    wDto.setActiveTasks(wDto.getActiveTasks() + 1);
                }
            }
        }

        double completionPercentage = 0.0;
        if (totalTasks > 0) {
            completionPercentage = Math.round(((double) doneTasks / totalTasks) * 1000.0) / 10.0;
        }

        return WorkspaceReportDto.builder()
                .totalTasks(totalTasks)
                .todoTasks(todoTasks)
                .inProgressTasks(inProgressTasks)
                .doneTasks(doneTasks)
                .completionPercentage(completionPercentage)
                .workload(new ArrayList<>(workloadMap.values()))
                .build();
    }

    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public com.taskcenter.dto.SprintReportDto getSprintSummary(String workspaceId, String sprintId, User currentUser) {
        authorizationService.checkAccess(workspaceId, currentUser);

        com.taskcenter.model.Sprint sprint = sprintRepository.findById(sprintId)
                .orElseThrow(() -> new com.taskcenter.exception.ResourceNotFoundException("Sprint topilmadi"));

        if (!sprint.getWorkspaceId().equals(workspaceId)) {
            throw new com.taskcenter.exception.BadRequestException("Sprint ushbu loyihaga tegishli emas");
        }

        List<BoardColumn> columns = columnRepository.findByWorkspaceIdOrderByOrderAsc(workspaceId);
        String doneColumnId = null;
        for (BoardColumn col : columns) {
            if (Boolean.TRUE.equals(col.getIsDefault()) && col.getOrder() == 3) {
                doneColumnId = col.getId();
                break;
            }
        }

        List<Task> sprintTasks = taskRepository.findBySprintId(sprintId);

        long totalTasks = sprintTasks.size();
        long completedTasks = 0;
        int totalStoryPoints = 0;
        int completedStoryPoints = 0;

        for (Task task : sprintTasks) {
            boolean isDone = task.getColumnId().equals(doneColumnId);
            int points = task.getStoryPoints() != null ? task.getStoryPoints() : 0;

            totalStoryPoints += points;
            if (isDone) {
                completedTasks++;
                completedStoryPoints += points;
            }
        }

        double completionPercentage = 0.0;
        if (totalStoryPoints > 0) {
            completionPercentage = Math.round(((double) completedStoryPoints / totalStoryPoints) * 1000.0) / 10.0;
        } else if (totalTasks > 0) {
            // Agar story point kiritilmagan bo'lsa, vazifalar soniga qarab hisoblaymiz
            completionPercentage = Math.round(((double) completedTasks / totalTasks) * 1000.0) / 10.0;
        }

        return com.taskcenter.dto.SprintReportDto.builder()
                .sprintId(sprint.getId())
                .sprintName(sprint.getName())
                .status(sprint.getStatus().name())
                .startDate(sprint.getStartDate())
                .endDate(sprint.getEndDate())
                .totalTasks(totalTasks)
                .completedTasks(completedTasks)
                .totalStoryPoints(totalStoryPoints)
                .completedStoryPoints(completedStoryPoints)
                .completionPercentage(completionPercentage)
                .build();
    }
}
