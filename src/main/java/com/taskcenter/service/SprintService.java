package com.taskcenter.service;

import com.taskcenter.dto.*;
import com.taskcenter.exception.BadRequestException;
import com.taskcenter.exception.ResourceNotFoundException;
import com.taskcenter.model.*;
import com.taskcenter.repository.ColumnRepository;
import com.taskcenter.repository.SprintRepository;
import com.taskcenter.repository.TaskRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import com.taskcenter.dto.WebSocketEvent;

@Service
public class SprintService {

    private final SprintRepository sprintRepository;
    private final TaskRepository taskRepository;
    private final ColumnRepository columnRepository;
    private final WorkspaceAuthorizationService authorizationService;
    private final TaskActivityService activityService;
    private final WebSocketNotifier webSocketNotifier;

    public SprintService(SprintRepository sprintRepository,
                         TaskRepository taskRepository,
                         ColumnRepository columnRepository,
                         WorkspaceAuthorizationService authorizationService,
                         TaskActivityService activityService,
                         WebSocketNotifier webSocketNotifier) {
        this.sprintRepository = sprintRepository;
        this.taskRepository = taskRepository;
        this.columnRepository = columnRepository;
        this.authorizationService = authorizationService;
        this.activityService = activityService;
        this.webSocketNotifier = webSocketNotifier;
    }

    @Transactional(readOnly = true)
    public List<SprintDto> getSprints(String workspaceId, SprintStatus status, User currentUser) {
        authorizationService.checkAccess(workspaceId, currentUser);

        List<Sprint> sprints = (status != null)
                ? sprintRepository.findByWorkspaceIdAndStatusOrderByCreatedAtDesc(workspaceId, status)
                : sprintRepository.findByWorkspaceIdOrderByCreatedAtDesc(workspaceId);

        java.util.Map<String, SprintStatsProjection> statsMap = taskRepository.findSprintStatsByWorkspaceId(workspaceId)
                .stream()
                .collect(Collectors.toMap(SprintStatsProjection::getSprintId, s -> s, (a, b) -> a));

        return sprints.stream()
                .map(s -> {
                    SprintStatsProjection stats = statsMap.get(s.getId());
                    long taskCount = stats != null && stats.getTaskCount() != null ? stats.getTaskCount() : 0L;
                    int storyPoints = stats != null && stats.getTotalStoryPoints() != null ? stats.getTotalStoryPoints() : 0;
                    return SprintDto.fromEntity(s, taskCount, storyPoints);
                })
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public VelocityChartDto getVelocityChart(String workspaceId, User currentUser) {
        authorizationService.checkAccess(workspaceId, currentUser);

        List<Sprint> closedSprints = sprintRepository.findByWorkspaceIdAndStatusOrderByCreatedAtAsc(workspaceId, SprintStatus.CLOSED);

        java.util.Map<String, SprintStatsProjection> statsMap = taskRepository.findSprintStatsByWorkspaceId(workspaceId)
                .stream()
                .collect(Collectors.toMap(SprintStatsProjection::getSprintId, s -> s, (a, b) -> a));

        List<VelocityChartDto.SprintVelocityDto> sprintVelocities = closedSprints.stream()
                .map(s -> {
                    SprintStatsProjection stats = statsMap.get(s.getId());
                    int completedPoints = stats != null && stats.getTotalStoryPoints() != null ? stats.getTotalStoryPoints() : 0;
                    return VelocityChartDto.SprintVelocityDto.builder()
                            .sprintId(s.getId())
                            .sprintName(s.getName())
                            .completedStoryPoints(completedPoints)
                            .build();
                })
                .collect(Collectors.toList());

        return VelocityChartDto.builder()
                .workspaceId(workspaceId)
                .sprints(sprintVelocities)
                .build();
    }

    @Transactional(readOnly = true)
    public SprintDto getSprintById(String sprintId, User currentUser) {
        Sprint sprint = sprintRepository.findById(sprintId)
                .orElseThrow(() -> new ResourceNotFoundException("Sprint topilmadi: " + sprintId));

        authorizationService.checkAccess(sprint.getWorkspaceId(), currentUser);

        long taskCount = taskRepository.countBySprintId(sprint.getId());
        Integer storyPoints = taskRepository.sumStoryPointsBySprintId(sprint.getId());
        return SprintDto.fromEntity(sprint, taskCount, storyPoints != null ? storyPoints : 0);
    }

    @Transactional
    public SprintDto createSprint(String workspaceId, SprintCreateRequest req, User currentUser) {
        authorizationService.checkCanEdit(workspaceId, currentUser);

        validateDates(req.getStartDate(), req.getEndDate());

        Sprint sprint = Sprint.builder()
                .workspaceId(workspaceId)
                .name(req.getName())
                .goal(req.getGoal())
                .status(SprintStatus.FUTURE)
                .startDate(req.getStartDate())
                .endDate(req.getEndDate())
                .build();

        Sprint saved = sprintRepository.save(sprint);
        return SprintDto.fromEntity(saved, 0L, 0);
    }

    @Transactional
    public SprintDto updateSprint(String sprintId, SprintUpdateRequest req, User currentUser) {
        Sprint sprint = sprintRepository.findById(sprintId)
                .orElseThrow(() -> new ResourceNotFoundException("Sprint topilmadi: " + sprintId));

        authorizationService.checkCanEdit(sprint.getWorkspaceId(), currentUser);

        if (req.getName() != null) {
            sprint.setName(req.getName());
        }
        if (req.getGoal() != null) {
            sprint.setGoal(req.getGoal());
        }

        LocalDate startDate = req.getStartDate() != null ? req.getStartDate() : sprint.getStartDate();
        LocalDate endDate = req.getEndDate() != null ? req.getEndDate() : sprint.getEndDate();
        validateDates(startDate, endDate);

        if (req.getStartDate() != null) {
            sprint.setStartDate(req.getStartDate());
        }
        if (req.getEndDate() != null) {
            sprint.setEndDate(req.getEndDate());
        }

        Sprint saved = sprintRepository.save(sprint);
        long taskCount = taskRepository.countBySprintId(saved.getId());
        Integer storyPoints = taskRepository.sumStoryPointsBySprintId(saved.getId());
        return SprintDto.fromEntity(saved, taskCount, storyPoints != null ? storyPoints : 0);
    }

    @Transactional
    public SprintDto startSprint(String sprintId, User currentUser) {
        Sprint sprint = sprintRepository.findById(sprintId)
                .orElseThrow(() -> new ResourceNotFoundException("Sprint topilmadi: " + sprintId));

        authorizationService.checkCanEdit(sprint.getWorkspaceId(), currentUser);

        if (sprint.getStatus() == SprintStatus.ACTIVE) {
            throw new BadRequestException("Ushbu sprint allaqachon faol");
        }
        if (sprint.getStatus() == SprintStatus.CLOSED) {
            throw new BadRequestException("Yopilgan sprintni qayta boshlab bo'lmaydi");
        }

        Optional<Sprint> activeOpt = sprintRepository.findByWorkspaceIdAndStatus(sprint.getWorkspaceId(), SprintStatus.ACTIVE);
        if (activeOpt.isPresent()) {
            throw new BadRequestException("Workspaceda allaqachon faol sprint mavjud: " + activeOpt.get().getName() + ". Yangi sprint boshlashdan oldin joriy faol sprintni yakunlang.");
        }

        sprint.setStatus(SprintStatus.ACTIVE);
        if (sprint.getStartDate() == null) {
            sprint.setStartDate(LocalDate.now());
        }

        Sprint saved = sprintRepository.save(sprint);
        long taskCount = taskRepository.countBySprintId(saved.getId());
        Integer storyPoints = taskRepository.sumStoryPointsBySprintId(saved.getId());
        SprintDto dto = SprintDto.fromEntity(saved, taskCount, storyPoints != null ? storyPoints : 0);
        
        webSocketNotifier.notifyWorkspace(sprint.getWorkspaceId(), WebSocketEvent.builder()
                .type("SPRINT_STARTED")
                .workspaceId(sprint.getWorkspaceId())
                .data(dto)
                .build());
                
        return dto;
    }

    @Transactional
    public SprintDto completeSprint(String sprintId, CompleteSprintRequest req, User currentUser) {
        Sprint sprint = sprintRepository.findById(sprintId)
                .orElseThrow(() -> new ResourceNotFoundException("Sprint topilmadi: " + sprintId));

        authorizationService.checkCanEdit(sprint.getWorkspaceId(), currentUser);

        if (sprint.getStatus() != SprintStatus.ACTIVE) {
            throw new BadRequestException("Faqat faol (ACTIVE) sprintni yakunlash mumkin");
        }

        String targetSprintId = null;
        String targetSprintName = "Backlog";
        if (req != null && req.getMoveToSprintId() != null && !req.getMoveToSprintId().isBlank()) {
            Sprint targetSprint = sprintRepository.findById(req.getMoveToSprintId())
                    .orElseThrow(() -> new ResourceNotFoundException("Ko'chiriladigan sprint topilmadi: " + req.getMoveToSprintId()));
            if (!sprint.getWorkspaceId().equals(targetSprint.getWorkspaceId())) {
                throw new BadRequestException("Ko'chiriladigan sprint bir xil workspaceda bo'lishi kerak");
            }
            if (targetSprint.getStatus() == SprintStatus.CLOSED) {
                throw new BadRequestException("Vazifalarni yopilgan sprintga ko'chirib bo'lmaydi");
            }
            targetSprintId = targetSprint.getId();
            targetSprintName = targetSprint.getName();
        }

        sprint.setStatus(SprintStatus.CLOSED);
        if (sprint.getEndDate() == null) {
            sprint.setEndDate(LocalDate.now());
        }

        // Identify done columns to keep finished tasks in this sprint history
        List<BoardColumn> columns = columnRepository.findByWorkspaceIdOrderByOrderAsc(sprint.getWorkspaceId());
        Set<String> doneColumnIds = columns.stream()
                .filter(c -> c.getTitle().trim().equalsIgnoreCase("Done")
                        || c.getTitle().trim().equalsIgnoreCase("Completed")
                        || c.getTitle().trim().equalsIgnoreCase("Tugallandi")
                        || c.getTitle().trim().equalsIgnoreCase("Bajarildi"))
                .map(BoardColumn::getId)
                .collect(Collectors.toSet());

        // Agar nomidan topilmasa, eng oxirgi ustunni tugallangan deb hisoblaymiz (agar ustunlar bo'lsa)
        if (doneColumnIds.isEmpty() && !columns.isEmpty()) {
            doneColumnIds.add(columns.get(columns.size() - 1).getId());
        }

        // Bajarilmagan tasklarni yangi sprintga yoki backlogga ko'chirish
        List<Task> tasks = taskRepository.findBySprintId(sprintId);
        List<Task> tasksToMove = new java.util.ArrayList<>();
        for (Task task : tasks) {
            if (!doneColumnIds.contains(task.getColumnId())) {
                task.setSprintId(targetSprintId);
                tasksToMove.add(task);
                if (targetSprintId != null) {
                    activityService.logActivity(task.getId(), currentUser, TaskActivityType.SPRINT_ASSIGNED, "sprint", sprint.getName(), targetSprintName);
                } else {
                    activityService.logActivity(task.getId(), currentUser, TaskActivityType.SPRINT_REMOVED, "sprint", sprint.getName(), "Backlog");
                }
            }
        }
        if (!tasksToMove.isEmpty()) {
            taskRepository.saveAll(tasksToMove);
        }

        Sprint saved = sprintRepository.save(sprint);
        long taskCount = taskRepository.countBySprintId(saved.getId());
        Integer storyPoints = taskRepository.sumStoryPointsBySprintId(saved.getId());
        SprintDto dto = SprintDto.fromEntity(saved, taskCount, storyPoints != null ? storyPoints : 0);
        
        webSocketNotifier.notifyWorkspace(sprint.getWorkspaceId(), WebSocketEvent.builder()
                .type("SPRINT_COMPLETED")
                .workspaceId(sprint.getWorkspaceId())
                .data(dto)
                .build());
                
        return dto;
    }

    @Transactional
    public void deleteSprint(String sprintId, User currentUser) {
        Sprint sprint = sprintRepository.findById(sprintId)
                .orElseThrow(() -> new ResourceNotFoundException("Sprint topilmadi: " + sprintId));

        authorizationService.checkCanEdit(sprint.getWorkspaceId(), currentUser);

        // Sprintdagi tasklarni backlogga qaytarish
        List<Task> tasks = taskRepository.findBySprintId(sprintId);
        for (Task task : tasks) {
            task.setSprintId(null);
            activityService.logActivity(task.getId(), currentUser, TaskActivityType.SPRINT_REMOVED, "sprint", sprint.getName(), "Backlog");
        }
        if (!tasks.isEmpty()) {
            taskRepository.saveAll(tasks);
        }

        sprintRepository.delete(sprint);
    }

    @Transactional(readOnly = true)
    public List<TaskDto> getSprintTasks(String sprintId, User currentUser) {
        Sprint sprint = sprintRepository.findById(sprintId)
                .orElseThrow(() -> new ResourceNotFoundException("Sprint topilmadi: " + sprintId));

        authorizationService.checkAccess(sprint.getWorkspaceId(), currentUser);

        return taskRepository.findBySprintIdOrderByLexoRankAsc(sprintId)
                .stream()
                .map(TaskDto::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional
    public List<TaskDto> addTasksToSprint(String sprintId, SprintTaskMoveRequest req, User currentUser) {
        Sprint sprint = sprintRepository.findById(sprintId)
                .orElseThrow(() -> new ResourceNotFoundException("Sprint topilmadi: " + sprintId));

        authorizationService.checkCanEdit(sprint.getWorkspaceId(), currentUser);

        if (sprint.getStatus() == SprintStatus.CLOSED) {
            throw new BadRequestException("Yopilgan sprintga yangi vazifalar qo'shib bo'lmaydi");
        }

        List<Task> tasksToSave = new java.util.ArrayList<>();
        for (String taskId : req.getTaskIds()) {
            Task task = taskRepository.findById(taskId)
                    .orElseThrow(() -> new ResourceNotFoundException("Task topilmadi: " + taskId));

            if (!sprint.getWorkspaceId().equals(task.getWorkspaceId())) {
                throw new BadRequestException("Task va sprint bir xil workspaceda bo'lishi kerak");
            }

            if (sprintId.equals(task.getSprintId())) {
                continue;
            }

            String oldSprintId = task.getSprintId();
            task.setSprintId(sprintId);
            tasksToSave.add(task);
            activityService.logActivity(task.getId(), currentUser, TaskActivityType.SPRINT_ASSIGNED, "sprint", oldSprintId, sprint.getName());
        }
        if (!tasksToSave.isEmpty()) {
            taskRepository.saveAll(tasksToSave);
        }

        List<TaskDto> result = taskRepository.findBySprintIdOrderByLexoRankAsc(sprintId)
                .stream()
                .map(TaskDto::fromEntity)
                .collect(Collectors.toList());
                
        webSocketNotifier.notifyWorkspace(sprint.getWorkspaceId(), WebSocketEvent.builder()
                .type("SPRINT_TASKS_ADDED")
                .workspaceId(sprint.getWorkspaceId())
                .data(java.util.Map.of("sprintId", sprintId, "tasks", result))
                .build());
                
        return result;
    }

    @Transactional
    public void removeTaskFromSprint(String sprintId, String taskId, User currentUser) {
        Sprint sprint = sprintRepository.findById(sprintId)
                .orElseThrow(() -> new ResourceNotFoundException("Sprint topilmadi: " + sprintId));

        authorizationService.checkCanEdit(sprint.getWorkspaceId(), currentUser);

        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new ResourceNotFoundException("Task topilmadi: " + taskId));

        if (!sprintId.equals(task.getSprintId())) {
            throw new BadRequestException("Task ushbu sprintga tegishli emas");
        }

        task.setSprintId(null);
        taskRepository.save(task);
        activityService.logActivity(task.getId(), currentUser, TaskActivityType.SPRINT_REMOVED, "sprint", sprint.getName(), "Backlog");
    }

    @Transactional(readOnly = true)
    public BacklogDto getBacklog(String workspaceId, int page, int size, User currentUser) {
        authorizationService.checkAccess(workspaceId, currentUser);

        if (page < 0) {
            page = 0;
        }
        if (size <= 0) {
            size = 20;
        }
        if (size > 100) {
            size = 100;
        }

        Pageable pageable = PageRequest.of(page, size);
        Page<Task> taskPage = taskRepository.findBacklogTasks(workspaceId, pageable);
        Integer totalStoryPoints = taskRepository.sumStoryPointsInBacklog(workspaceId);

        List<TaskDto> taskDtos = taskPage.getContent()
                .stream()
                .map(TaskDto::fromEntity)
                .collect(Collectors.toList());

        return BacklogDto.builder()
                .tasks(taskDtos)
                .totalTasks(taskPage.getTotalElements())
                .totalStoryPoints(totalStoryPoints != null ? totalStoryPoints : 0)
                .currentPage(page)
                .totalPages(taskPage.getTotalPages())
                .build();
    }

    private void validateDates(LocalDate startDate, LocalDate endDate) {
        if (startDate != null && endDate != null && endDate.isBefore(startDate)) {
            throw new BadRequestException("Tugash sanasi boshlanish sanasidan oldin bo'lishi mumkin emas");
        }
    }
}
