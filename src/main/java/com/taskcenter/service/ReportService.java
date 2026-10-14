package com.taskcenter.service;

import com.taskcenter.dto.ColumnTaskCountProjection;
import com.taskcenter.dto.SprintReportDto;
import com.taskcenter.dto.UserWorkloadDto;
import com.taskcenter.dto.UserWorkloadProjection;
import com.taskcenter.dto.WorkspaceReportDto;
import com.taskcenter.exception.BadRequestException;
import com.taskcenter.exception.ResourceNotFoundException;
import com.taskcenter.model.BoardColumn;
import com.taskcenter.model.Sprint;
import com.taskcenter.model.Task;
import com.taskcenter.model.User;
import com.taskcenter.repository.ColumnRepository;
import com.taskcenter.repository.SprintRepository;
import com.taskcenter.repository.TaskRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
public class ReportService {

    private final TaskRepository taskRepository;
    private final ColumnRepository columnRepository;
    private final WorkspaceAuthorizationService authorizationService;
    private final SprintRepository sprintRepository;

    public ReportService(TaskRepository taskRepository,
                         ColumnRepository columnRepository,
                         WorkspaceAuthorizationService authorizationService,
                         SprintRepository sprintRepository) {
        this.taskRepository = taskRepository;
        this.columnRepository = columnRepository;
        this.authorizationService = authorizationService;
        this.sprintRepository = sprintRepository;
    }

    @Transactional(readOnly = true)
    public WorkspaceReportDto getWorkspaceSummary(String workspaceId, User currentUser) {
        authorizationService.checkAccess(workspaceId, currentUser);

        List<BoardColumn> columns = columnRepository.findByWorkspaceIdOrderByOrderAsc(workspaceId);

        String todoColumnId = null;
        String inProgressColumnId = null;
        String doneColumnId = null;

        for (BoardColumn col : columns) {
            if (Boolean.TRUE.equals(col.getIsDefault())) {
                if (col.getOrder() == 1) todoColumnId = col.getId();
                else if (col.getOrder() == 2) inProgressColumnId = col.getId();
            }
            if (Boolean.TRUE.equals(col.getIsDone())) {
                doneColumnId = col.getId();
            }
        }

        List<ColumnTaskCountProjection> taskCounts = taskRepository.getWorkspaceTaskCountsByColumn(workspaceId);

        long totalTasks = 0;
        long todoTasks = 0;
        long inProgressTasks = 0;
        long doneTasks = 0;

        for (ColumnTaskCountProjection proj : taskCounts) {
            long count = proj.getTaskCount() != null ? proj.getTaskCount() : 0L;
            totalTasks += count;

            String colId = proj.getColumnId();
            if (colId.equals(todoColumnId)) todoTasks += count;
            else if (colId.equals(inProgressColumnId)) inProgressTasks += count;
            else if (colId.equals(doneColumnId)) doneTasks += count;
        }

        List<UserWorkloadProjection> workloadProjections = taskRepository.getWorkspaceWorkloadStats(workspaceId, doneColumnId);
        List<UserWorkloadDto> workloadList = new ArrayList<>();

        for (UserWorkloadProjection wp : workloadProjections) {
            workloadList.add(UserWorkloadDto.builder()
                    .userId(wp.getUserId())
                    .userName(wp.getUserName())
                    .userFullName(wp.getUserFullName())
                    .totalAssignedTasks(wp.getTotalTasks() != null ? wp.getTotalTasks().intValue() : 0)
                    .completedTasks(wp.getCompletedTasks() != null ? wp.getCompletedTasks().intValue() : 0)
                    .activeTasks(wp.getActiveTasks() != null ? wp.getActiveTasks().intValue() : 0)
                    .build());
        }

        double completionPercentage = 0.0;
        if (totalTasks > 0) {
            completionPercentage = Math.round(((double) doneTasks / totalTasks) * 100.0 * 10.0) / 10.0;
        }

        return WorkspaceReportDto.builder()
                .totalTasks(totalTasks)
                .todoTasks(todoTasks)
                .inProgressTasks(inProgressTasks)
                .doneTasks(doneTasks)
                .completionPercentage(completionPercentage)
                .workload(workloadList)
                .build();
    }

    @Transactional(readOnly = true)
    public SprintReportDto getSprintSummary(String workspaceId, String sprintId, User currentUser) {
        authorizationService.checkAccess(workspaceId, currentUser);

        Sprint sprint = sprintRepository.findById(sprintId)
                .orElseThrow(() -> new ResourceNotFoundException("Sprint topilmadi"));

        if (!sprint.getWorkspaceId().equals(workspaceId)) {
            throw new BadRequestException("Sprint ushbu g'alvaga tegishli emas");
        }

        List<BoardColumn> columns = columnRepository.findByWorkspaceIdOrderByOrderAsc(workspaceId);
        String doneColumnId = null;
        for (BoardColumn col : columns) {
            if (Boolean.TRUE.equals(col.getIsDone())) {
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
            completionPercentage = Math.round(((double) completedTasks / totalTasks) * 1000.0) / 10.0;
        }

        return SprintReportDto.builder()
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
