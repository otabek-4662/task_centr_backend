package com.taskcenter.service;

import com.taskcenter.dto.TaskActivityDto;
import com.taskcenter.exception.ResourceNotFoundException;
import com.taskcenter.model.Task;
import com.taskcenter.model.TaskActivity;
import com.taskcenter.model.TaskActivityType;
import com.taskcenter.model.User;
import com.taskcenter.repository.TaskActivityRepository;
import com.taskcenter.repository.TaskRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new ResourceNotFoundException("Task topilmadi: " + taskId));

        authorizationService.checkAccess(task.getWorkspaceId(), currentUser);

        return activityRepository.findByTaskIdOrderByCreatedAtDesc(taskId, pageable)
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
}
