package com.taskcenter.service;

import com.taskcenter.dto.WorkspaceCreateRequest;
import com.taskcenter.dto.WorkspaceDto;
import com.taskcenter.dto.WorkspaceListDto;
import com.taskcenter.exception.ForbiddenException;
import com.taskcenter.exception.ResourceNotFoundException;
import com.taskcenter.model.User;
import com.taskcenter.model.Workspace;
import com.taskcenter.repository.ColumnRepository;
import com.taskcenter.repository.SprintRepository;
import com.taskcenter.repository.TaskRepository;
import com.taskcenter.repository.WorkspaceRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WorkspaceServiceTest {

    @Mock
    private WorkspaceRepository workspaceRepository;
    @Mock
    private WorkspaceAuthorizationService authorizationService;
    @Mock
    private ColumnRepository columnRepository;
    @Mock
    private TaskRepository taskRepository;
    @Mock
    private SprintRepository sprintRepository;

    @InjectMocks
    private WorkspaceService workspaceService;

    private final User testUser = User.builder().id("user1").name("elshod").role(User.Role.USER).build();

    private User testUser() {
        return testUser;
    }

    private Workspace testWorkspace() {
        return Workspace.builder()
                .id("ws1")
                .title("Test Workspace")
                .bgColor("#abc")
                .ownerId("user1")
                .build();
    }

    // ===== getWorkspaces =====

    @Test
    void getWorkspaces_returnsListForUser() {
        Workspace ws = testWorkspace();
        Page<Workspace> page = new PageImpl<>(List.of(ws));
        when(workspaceRepository.findByOwnerIdOrMemberUserIdPaginated(eq("user1"), any(Pageable.class)))
                .thenReturn(page);

        List<WorkspaceListDto> result = workspaceService.getWorkspaces(testUser(), 0, 20);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getTitle()).isEqualTo("Test Workspace");
    }

    @Test
    void getWorkspaces_nullUser_throwsForbidden() {
        assertThatThrownBy(() -> workspaceService.getWorkspaces(null, 0, 20))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void getWorkspaces_negativePage_throwsBadRequest() {
        assertThatThrownBy(() -> workspaceService.getWorkspaces(testUser(), -1, 20))
                .isInstanceOf(RuntimeException.class);
    }

    // ===== getWorkspaceById =====

    @Test
    void getWorkspaceById_returnsDto() {
        org.mockito.Mockito.lenient().when(authorizationService.checkAccess("ws1", testUser())).thenReturn(new com.taskcenter.model.Workspace());
        when(workspaceRepository.findById("ws1")).thenReturn(Optional.of(testWorkspace()));

        WorkspaceDto result = workspaceService.getWorkspaceById("ws1", testUser());

        assertThat(result.getTitle()).isEqualTo("Test Workspace");
    }

    @Test
    void getWorkspaceById_notFound_throws() {
        org.mockito.Mockito.lenient().when(authorizationService.checkAccess("ws999", testUser())).thenReturn(new com.taskcenter.model.Workspace());
        when(workspaceRepository.findById("ws999")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> workspaceService.getWorkspaceById("ws999", testUser()))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void getWorkspaceById_noAccess_throwsForbidden() {
        User stranger = User.builder().id("stranger").name("notmember").role(User.Role.USER).build();
        doThrow(new ForbiddenException("Ruxsat yo'q"))
                .when(authorizationService).checkAccess("ws1", stranger);

        assertThatThrownBy(() -> workspaceService.getWorkspaceById("ws1", stranger))
                .isInstanceOf(ForbiddenException.class);
    }

    // ===== createWorkspace =====

    @Test
    void createWorkspace_savesAndReturnsDto() {
        WorkspaceCreateRequest req = new WorkspaceCreateRequest();
        req.setTitle("New WS");
        req.setBgColor("#fff");
        req.setInitDefaultColumns(true);

        when(workspaceRepository.save(any(Workspace.class))).thenAnswer(inv -> {
            Workspace ws = inv.getArgument(0);
            ws.setId("new-id");
            return ws;
        });

        WorkspaceDto result = workspaceService.createWorkspace(req, testUser());

        assertThat(result.getTitle()).isEqualTo("New WS");
        verify(workspaceRepository).save(any(Workspace.class));
        verify(columnRepository, times(3)).save(any());
    }

    // ===== deleteWorkspace =====

    @Test
    void deleteWorkspace_ownerCanDelete() {
        org.mockito.Mockito.lenient().when(authorizationService.checkOwner("ws1", testUser())).thenReturn(new com.taskcenter.model.Workspace());
        when(workspaceRepository.existsById("ws1")).thenReturn(true);

        workspaceService.deleteWorkspace("ws1", testUser());

        verify(taskRepository).deleteByWorkspaceId("ws1");
        verify(sprintRepository).deleteByWorkspaceId("ws1");
        verify(columnRepository).deleteByWorkspaceId("ws1");
        verify(workspaceRepository).deleteById("ws1");
    }

    @Test
    void deleteWorkspace_notFound_throws() {
        org.mockito.Mockito.lenient().when(authorizationService.checkOwner("ws999", testUser())).thenReturn(new com.taskcenter.model.Workspace());
        when(workspaceRepository.existsById("ws999")).thenReturn(false);

        assertThatThrownBy(() -> workspaceService.deleteWorkspace("ws999", testUser()))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void deleteWorkspace_nonOwner_throwsForbidden() {
        User stranger = User.builder().id("stranger").name("other").role(User.Role.USER).build();
        doThrow(new ForbiddenException("Faqat egasi"))
                .when(authorizationService).checkOwner("ws1", stranger);

        assertThatThrownBy(() -> workspaceService.deleteWorkspace("ws1", stranger))
                .isInstanceOf(ForbiddenException.class);
    }
}
