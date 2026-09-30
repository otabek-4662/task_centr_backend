package com.taskcenter.service;

import com.taskcenter.dto.WorkspaceCreateRequest;
import com.taskcenter.dto.WorkspaceDto;
import com.taskcenter.dto.WorkspaceListDto;
import com.taskcenter.exception.BadRequestException;
import com.taskcenter.exception.ForbiddenException;
import com.taskcenter.model.BoardColumn;
import com.taskcenter.model.User;
import com.taskcenter.model.Workspace;
import com.taskcenter.repository.ColumnRepository;
import com.taskcenter.repository.SprintRepository;
import com.taskcenter.repository.TaskRepository;
import com.taskcenter.repository.WorkspaceRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class WorkspaceService {

    private final WorkspaceRepository workspaceRepository;
    private final WorkspaceAuthorizationService authorizationService;
    private final ColumnRepository columnRepository;
    private final TaskRepository taskRepository;
    private final SprintRepository sprintRepository;

    public WorkspaceService(WorkspaceRepository workspaceRepository,
                            WorkspaceAuthorizationService authorizationService,
                            ColumnRepository columnRepository,
                            TaskRepository taskRepository,
                            SprintRepository sprintRepository) {
        this.workspaceRepository = workspaceRepository;
        this.authorizationService = authorizationService;
        this.columnRepository = columnRepository;
        this.taskRepository = taskRepository;
        this.sprintRepository = sprintRepository;
    }

    @Transactional(readOnly = true)
    public Page<WorkspaceListDto> getWorkspaces(User currentUser, Pageable pageable) {
        if (currentUser == null) {
            throw new ForbiddenException("Foydalanuvchi tizimga kirmagan");
        }

        Page<Workspace> workspacePage = workspaceRepository.findByOwnerIdOrMemberUserIdPaginated(currentUser.getId(), pageable);
        return workspacePage.map(WorkspaceListDto::fromEntity);
    }

    // Kesh (@Cacheable) ishlatilmaydi: kesh urilganda metod chaqirilmaydi va checkAccess o'tkazib yuboriladi
    @Transactional(readOnly = true)
    public WorkspaceDto getWorkspaceById(String id, User currentUser) {
        Workspace workspace = authorizationService.checkAccess(id, currentUser);
        return WorkspaceDto.fromEntity(workspace);
    }

    @Transactional
    public WorkspaceDto createWorkspace(WorkspaceCreateRequest request, User currentUser) {
        String basePrefix = request.getTitle().replaceAll("[^a-zA-Z0-9]", "").toUpperCase();
        if (basePrefix.length() > 3) {
            basePrefix = basePrefix.substring(0, 3);
        } else if (basePrefix.isEmpty()) {
            basePrefix = "WS";
        }
        String uniqueKeyPrefix = basePrefix + "-" + java.util.UUID.randomUUID().toString().substring(0, 4).toUpperCase();

        Workspace workspace = Workspace.builder()
                .title(request.getTitle())
                .bgColor(request.getBgColor())
                .description(request.getDescription())
                .ownerId(currentUser.getId())
                .keyPrefix(uniqueKeyPrefix)
                .build();

        Workspace saved = workspaceRepository.save(workspace);

        if (request.getInitDefaultColumns() == null || request.getInitDefaultColumns()) {
            columnRepository.save(BoardColumn.builder()
                    .workspaceId(saved.getId())
                    .title("Dushanbadan")
                    .order(1)
                    .isDefault(true)
                    .isDone(false)
                    .build());

            columnRepository.save(BoardColumn.builder()
                    .workspaceId(saved.getId())
                    .title("Jumagacha bitadi")
                    .order(2)
                    .isDefault(true)
                    .isDone(false)
                    .build());

            columnRepository.save(BoardColumn.builder()
                    .workspaceId(saved.getId())
                    .title("Ko'z tegmasin")
                    .order(3)
                    .isDefault(true)
                    .isDone(true)
                    .build());
        }

        return WorkspaceDto.fromEntity(saved);
    }

    @Transactional
    public WorkspaceDto updateWorkspace(String id, WorkspaceCreateRequest request, User currentUser) {
        Workspace workspace = authorizationService.checkOwner(id, currentUser);

        if (request.getTitle() != null) {
            workspace.setTitle(request.getTitle());
        }
        if (request.getBgColor() != null) {
            workspace.setBgColor(request.getBgColor());
        }
        if (request.getDescription() != null) {
            workspace.setDescription(request.getDescription());
        }

        Workspace updated = workspaceRepository.save(workspace);
        return WorkspaceDto.fromEntity(updated);
    }

    @Transactional
    public void deleteWorkspace(String id, User currentUser) {
        authorizationService.checkOwner(id, currentUser);

        taskRepository.deleteByWorkspaceId(id);
        sprintRepository.deleteByWorkspaceId(id);
        columnRepository.deleteByWorkspaceId(id);
        workspaceRepository.deleteById(id);
    }
}
