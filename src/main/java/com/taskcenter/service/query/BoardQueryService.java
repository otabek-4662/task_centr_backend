package com.taskcenter.service.query;

import com.taskcenter.dto.BoardInitColumnDto;
import com.taskcenter.dto.ColumnWithCardsDto;
import com.taskcenter.model.BoardColumn;
import com.taskcenter.model.Sprint;
import com.taskcenter.model.SprintStatus;
import com.taskcenter.model.Task;
import com.taskcenter.model.User;
import com.taskcenter.repository.ColumnRepository;
import com.taskcenter.repository.SprintRepository;
import com.taskcenter.repository.TaskRepository;
import com.taskcenter.service.WorkspaceAuthorizationService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * oldboyxx/jira_clone Kanban board modeli va gothinkster/spring-boot-realworld-example-app
 * CQRS Read Model standartlariga mos Kanban Board Query xizmati.
 *
 * Vazifasi: Doska ustunlari va ularga tegishli kartochkalarni (tasks)
 * LexoRank bo'yicha saralangan holda bitta tranzaksiyada tezkor o'qib berish.
 */
@Service
@Transactional(readOnly = true)
public class BoardQueryService {

    private final ColumnRepository columnRepository;
    private final TaskRepository taskRepository;
    private final SprintRepository sprintRepository;
    private final WorkspaceAuthorizationService authorizationService;

    public BoardQueryService(ColumnRepository columnRepository,
                             TaskRepository taskRepository,
                             SprintRepository sprintRepository,
                             WorkspaceAuthorizationService authorizationService) {
        this.columnRepository = columnRepository;
        this.taskRepository = taskRepository;
        this.sprintRepository = sprintRepository;
        this.authorizationService = authorizationService;
    }

    /**
     * Kanban doskasini ustunlar va ularning kartochkalari bilan birga olish.
     * Kartochkalar LexoRank bo'yicha o'sish tartibida saralanadi.
     */
    public List<ColumnWithCardsDto> getKanbanBoard(String workspaceId, String sprintId, User currentUser) {
        authorizationService.checkAccess(workspaceId, currentUser);

        String effectiveSprintId = resolveSprintId(workspaceId, sprintId);
        List<BoardColumn> cols = columnRepository.findByWorkspaceIdOrderByOrderAsc(workspaceId);

        if (effectiveSprintId == null) {
            // Faol sprint bo'lmasa, ustunlarni bo'sh qaytaramiz yoki backlog
            return cols.stream()
                    .map(c -> ColumnWithCardsDto.fromEntity(c, Collections.emptyList()))
                    .collect(Collectors.toList());
        }

        List<Task> allSprintTasks = taskRepository.findBySprintId(effectiveSprintId);

        return cols.stream().map(col -> {
            List<Task> columnTasks = allSprintTasks.stream()
                    .filter(t -> col.getId().equals(t.getColumnId()) && Boolean.FALSE.equals(t.getIsArchived()))
                    .sorted(Comparator.comparing(Task::getLexoRank, Comparator.nullsLast(Comparator.naturalOrder())))
                    .collect(Collectors.toList());
            return ColumnWithCardsDto.fromEntity(col, columnTasks);
        }).collect(Collectors.toList());
    }

    /**
     * Yangi BoardInitColumnDto formati bo'yicha doska ma'lumotlarini olish.
     */
    public List<BoardInitColumnDto> getBoardInit(String workspaceId, String sprintId, User currentUser) {
        authorizationService.checkAccess(workspaceId, currentUser);

        String effectiveSprintId = resolveSprintId(workspaceId, sprintId);
        List<BoardColumn> cols = columnRepository.findByWorkspaceIdOrderByOrderAsc(workspaceId);

        if (effectiveSprintId == null) {
            return cols.stream()
                    .map(c -> BoardInitColumnDto.fromEntity(c, Collections.emptyList()))
                    .collect(Collectors.toList());
        }

        List<Task> allSprintTasks = taskRepository.findBySprintId(effectiveSprintId);

        return cols.stream().map(col -> {
            List<Task> columnTasks = allSprintTasks.stream()
                    .filter(t -> col.getId().equals(t.getColumnId()) && Boolean.FALSE.equals(t.getIsArchived()))
                    .sorted(Comparator.comparing(Task::getLexoRank, Comparator.nullsLast(Comparator.naturalOrder())))
                    .collect(Collectors.toList());
            return BoardInitColumnDto.fromEntity(col, columnTasks);
        }).collect(Collectors.toList());
    }

    private String resolveSprintId(String workspaceId, String requestedSprintId) {
        if (requestedSprintId != null && !requestedSprintId.isBlank()) {
            return requestedSprintId;
        }
        Optional<Sprint> activeOpt = sprintRepository.findByWorkspaceIdAndStatus(workspaceId, SprintStatus.ACTIVE);
        return activeOpt.map(Sprint::getId).orElse(null);
    }
}
