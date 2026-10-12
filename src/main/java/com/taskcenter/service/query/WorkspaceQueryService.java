package com.taskcenter.service.query;

import com.taskcenter.dto.WorkspaceDto;
import com.taskcenter.dto.WorkspaceListDto;
import com.taskcenter.exception.ForbiddenException;
import com.taskcenter.model.User;
import com.taskcenter.model.Workspace;
import com.taskcenter.repository.WorkspaceRepository;
import com.taskcenter.service.WorkspaceAuthorizationService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * RealWorld CQRS (Command Query Responsibility Segregation) tamoyiliga binoan,
 * faqat ma'lumotlarni o'qish (Query / Read Model) bilan shug'ullanuvchi xizmat.
 *
 * Yozish (Command) operatsiyalari WorkspaceService da saqlanadi.
 */
@Service
@Transactional(readOnly = true)
public class WorkspaceQueryService {

    private final WorkspaceRepository workspaceRepository;
    private final WorkspaceAuthorizationService authorizationService;

    public WorkspaceQueryService(WorkspaceRepository workspaceRepository,
                                 WorkspaceAuthorizationService authorizationService) {
        this.workspaceRepository = workspaceRepository;
        this.authorizationService = authorizationService;
    }

    /**
     * Joriy foydalanuvchining a'zo bo'lgan barcha workspace larini sahifalab (pagination) olish.
     */
    public Page<WorkspaceListDto> getWorkspaces(User currentUser, Pageable pageable) {
        if (currentUser == null) {
            throw new ForbiddenException("Foydalanuvchi tizimga kirmagan");
        }

        Page<Workspace> workspacePage = workspaceRepository.findByOwnerIdOrMemberUserIdPaginated(currentUser.getId(), pageable);
        return workspacePage.map(WorkspaceListDto::fromEntity);
    }

    /**
     * ID orqali bitta workspace ma'lumotlarini huquqni tekshirgan holda o'qish.
     */
    public WorkspaceDto getWorkspaceById(String id, User currentUser) {
        Workspace workspace = authorizationService.checkAccess(id, currentUser);
        return WorkspaceDto.fromEntity(workspace);
    }
}
