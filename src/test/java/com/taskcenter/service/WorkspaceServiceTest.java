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
import com.taskcenter.model.BoardColumn;
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

        org.springframework.data.domain.Page<com.taskcenter.dto.WorkspaceListDto> result = workspaceService.getWorkspaces(testUser(), org.springframework.data.domain.PageRequest.of(0, 20));

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getTitle()).isEqualTo("Test Workspace");
    }

    @Test
    void getWorkspaces_nullUser_throwsForbidden() {
        assertThatThrownBy(() -> workspaceService.getWorkspaces(null, org.springframework.data.domain.PageRequest.of(0, 20)))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void getWorkspaces_negativePage_throwsBadRequest() {
        assertThatThrownBy(() -> workspaceService.getWorkspaces(testUser(), org.springframework.data.domain.PageRequest.of(-1, 20)))
                .isInstanceOf(RuntimeException.class);
    }

    // ===== getWorkspaceById =====

    @Test
    void getWorkspaceById_returnsDto() {
        // checkAccess ruxsatni tekshiradi va workspace ni qaytaradi
        when(authorizationService.checkAccess(eq("ws1"), any())).thenReturn(testWorkspace());

        WorkspaceDto result = workspaceService.getWorkspaceById("ws1", testUser());

        assertThat(result.getTitle()).isEqualTo("Test Workspace");
    }

    @Test
    void getWorkspaceById_notFound_throws() {
        when(authorizationService.checkAccess(eq("ws999"), any()))
                .thenThrow(new ResourceNotFoundException("Workspace topilmadi: ws999"));

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
        // WorkspaceService 3 ta default ustun yaratadi
        verify(columnRepository, times(3)).save(any());
    }

    @Test
    void createWorkspace_createsExactThreeDefaultColumns() {
        WorkspaceCreateRequest req = new WorkspaceCreateRequest();
        req.setTitle("New WS");
        req.setInitDefaultColumns(true);

        when(workspaceRepository.save(any(Workspace.class))).thenAnswer(inv -> {
            Workspace ws = inv.getArgument(0);
            ws.setId("new-ws-id");
            return ws;
        });

        workspaceService.createWorkspace(req, testUser());

        org.mockito.ArgumentCaptor<BoardColumn> columnCaptor = org.mockito.ArgumentCaptor.forClass(BoardColumn.class);
        verify(columnRepository, times(3)).save(columnCaptor.capture());

        List<BoardColumn> capturedColumns = columnCaptor.getAllValues();
        assertThat(capturedColumns).hasSize(3);

        assertThat(capturedColumns.get(0).getTitle()).isEqualTo("Dushanbadan");
        assertThat(capturedColumns.get(0).getOrder()).isEqualTo(1);
        assertThat(capturedColumns.get(0).getIsDone()).isFalse();
        
        assertThat(capturedColumns.get(1).getTitle()).isEqualTo("Jumagacha bitadi");
        assertThat(capturedColumns.get(1).getOrder()).isEqualTo(2);
        assertThat(capturedColumns.get(1).getIsDone()).isFalse();

        assertThat(capturedColumns.get(2).getTitle()).isEqualTo("Ko'z tegmasin");
        assertThat(capturedColumns.get(2).getOrder()).isEqualTo(3);
        assertThat(capturedColumns.get(2).getIsDone()).isTrue();
    }

    // ===== deleteWorkspace =====

    @Test
    void deleteWorkspace_ownerCanDelete() {
        workspaceService.deleteWorkspace("ws1", testUser());

        verify(taskRepository).deleteByWorkspaceId("ws1");
        verify(sprintRepository).deleteByWorkspaceId("ws1");
        verify(columnRepository).deleteByWorkspaceId("ws1");
        verify(workspaceRepository).deleteById("ws1");
    }

    @Test
    void deleteWorkspace_notFound_throws() {
        // Workspace mavjudligini endi checkOwner tekshiradi (requireWorkspace)
        when(authorizationService.checkOwner(eq("ws999"), any()))
                .thenThrow(new ResourceNotFoundException("Workspace topilmadi: ws999"));

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
