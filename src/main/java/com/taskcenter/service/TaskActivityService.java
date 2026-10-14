package com.taskcenter.service;

import com.taskcenter.dto.TaskActivityDto;
import com.taskcenter.exception.ResourceNotFoundException;
import com.taskcenter.model.TaskActivity;
import com.taskcenter.model.TaskActivityType;
import com.taskcenter.model.User;
import com.taskcenter.repository.TaskActivityRepository;
import com.taskcenter.repository.TaskRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class TaskActivityService {

    private final TaskActivityRepository activityRepository;
    private final TaskRepository taskRepository;
    private final WorkspaceAuthorizationService authorizationService;

    public TaskActivityService(TaskActivityRepository activityRepository,
                               TaskRepository taskRepository,
                               WorkspaceAuthorizationService authorizationService) {
        this.activityRepository = activityRepository;
        this.taskRepository = taskRepository;
        this.authorizationService = authorizationService;
    }

    @Transactional(readOnly = true)
    public Page<TaskActivityDto> getActivities(String taskId, Pageable pageable, User currentUser) {
        String workspaceId = taskRepository.findWorkspaceIdById(taskId)
                .orElseThrow(() -> new ResourceNotFoundException("Task topilmadi: " + taskId));

        authorizationService.checkAccess(workspaceId, currentUser);

        return activityRepository.findByTaskIdOrderByCreatedAtDesc(taskId, pageable)
                .map(TaskActivityDto::fromEntity);
    }

    @Transactional(readOnly = true)
    public Page<TaskActivityDto> getWorkspaceActivities(String workspaceId, Pageable pageable, User currentUser) {
        authorizationService.checkAccess(workspaceId, currentUser);

        return activityRepository.findByWorkspaceIdOrderByCreatedAtDesc(workspaceId, pageable)
                .map(TaskActivityDto::fromEntity);
    }

    @Transactional
    public void logActivity(String taskId, User user, TaskActivityType actionType,
                            String fieldName, String oldValue, String newValue) {
        TaskActivity activity = TaskActivity.builder()
                .taskId(taskId)
                .userId(user.getId())
                .user(user)
                .actionType(actionType)
                .fieldName(fieldName)
                .oldValue(oldValue)
                .newValue(newValue)
                .build();

        activityRepository.save(activity);
    }

    @Transactional
    public void logActivities(List<TaskActivity> activities) {
        if (activities != null && !activities.isEmpty()) {
            activityRepository.saveAll(activities);
        }
    }
}
