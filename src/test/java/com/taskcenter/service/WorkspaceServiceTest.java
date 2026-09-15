package com.taskcenter.service;

import com.taskcenter.dto.WorkspaceCreateRequest;
import com.taskcenter.dto.WorkspaceDto;
import com.taskcenter.dto.WorkspaceListDto;
import com.taskcenter.exception.EntityNotFoundException;
import com.taskcenter.model.User;
import com.taskcenter.model.Workspace;
import com.taskcenter.model.WorkspaceMember;
import com.taskcenter.model.WorkspaceRole;
import com.taskcenter.repository.UserRepository;
import com.taskcenter.repository.WorkspaceMemberRepository;
import com.taskcenter.repository.WorkspaceRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WorkspaceServiceTest {

    @Mock
    private WorkspaceRepository workspaceRepository;
    @Mock
    private WorkspaceMemberRepository memberRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private WorkspaceAuthorizationService authorizationService;

    @InjectMocks
    private WorkspaceService workspaceService;

    private User owner() {
        return User.builder().id("owner-1").name("owner").email("owner@test.local").password("x").build();
    }

    private Workspace workspace() {
        return Workspace.builder()
                .id("ws-1")
                .title("Team Alpha")
                .bgColor("#ff0000")
                .description("desc")
                .ownerId("owner-1")
                .keyPrefix("TA")
                .taskCounter(0)
                .build();
    }

    private WorkspaceCreateRequest req(String title) {
        WorkspaceCreateRequest r = new WorkspaceCreateRequest();
        r.setTitle(title);
        return r;
    }

    @Test
    void createWorkspace_savesOwnerMembershipAndReturnsDto() {
        WorkspaceCreateRequest r = req("Team Alpha");
        r.setBgColor("#ff0000");
        r.setDescription("desc");
        when(workspaceRepository.existsByKeyPrefix("TA")).thenReturn(false);
        when(workspaceRepository.save(any(Workspace.class))).thenAnswer(inv -> inv.getArgument(0));

        WorkspaceDto dto = workspaceService.createWorkspace(r, owner());

        assertThat(dto.getTitle()).isEqualTo("Team Alpha");
        assertThat(dto.getOwnerId()).isEqualTo("owner-1");
        ArgumentCaptor<Workspace> wsCap = ArgumentCaptor.forClass(Workspace.class);
        verify(workspaceRepository).save(wsCap.capture());
        assertThat(wsCap.getValue().getKeyPrefix()).isEqualTo("TA");
        ArgumentCaptor<WorkspaceMember> cap = ArgumentCaptor.forClass(WorkspaceMember.class);
        verify(memberRepository).save(cap.capture());
        assertThat(cap.getValue().getUserId()).isEqualTo("owner-1");
        assertThat(cap.getValue().getRole()).isEqualTo(WorkspaceRole.OWNER);
    }

    @Test
    void createWorkspace_blankTitle_defaultsPrefixToWS() {
        when(workspaceRepository.existsByKeyPrefix("WS")).thenReturn(false);
        when(workspaceRepository.save(any(Workspace.class))).thenAnswer(inv -> inv.getArgument(0));

        workspaceService.createWorkspace(req("   "), owner());

        ArgumentCaptor<Workspace> wsCap = ArgumentCaptor.forClass(Workspace.class);
        verify(workspaceRepository).save(wsCap.capture());
        assertThat(wsCap.getValue().getKeyPrefix()).isEqualTo("WS");
    }

    @Test
    void getWorkspaceById_returnsDto() {
        when(workspaceRepository.findById("ws-1")).thenReturn(Optional.of(workspace()));

        WorkspaceDto dto = workspaceService.getWorkspaceById("ws-1", owner());

        assertThat(dto.getId()).isEqualTo("ws-1");
        assertThat(dto.getTitle()).isEqualTo("Team Alpha");
    }

    @Test
    void getWorkspaceById_missing_throwsEntityNotFound() {
        when(workspaceRepository.findById("ws-1")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> workspaceService.getWorkspaceById("ws-1", owner()))
                .isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    void updateWorkspace_partialUpdateKeepsOtherFields() {
        Workspace ws = workspace();
        when(workspaceRepository.findById("ws-1")).thenReturn(Optional.of(ws));

        WorkspaceCreateRequest r = req("New Title");
        WorkspaceDto dto = workspaceService.updateWorkspace("ws-1", r, owner());

        assertThat(dto.getTitle()).isEqualTo("New Title");
        assertThat(dto.getBgColor()).isEqualTo("#ff0000");
        assertThat(dto.getDescription()).isEqualTo("desc");
        verify(workspaceRepository).save(ws);
    }

    @Test
    void updateWorkspace_missing_throwsEntityNotFound() {
        when(workspaceRepository.findById("ws-1")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> workspaceService.updateWorkspace("ws-1", req("New"), owner()))
                .isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    void deleteWorkspace_deletesById() {
        workspaceService.deleteWorkspace("ws-1", owner());

        verify(workspaceRepository).deleteById("ws-1");
    }

    @Test
    void addMember_savesMemberAndReturnsUser() {
        User member = User.builder().id("user-2").name("mem").email("mem@test.local").password("x").build();
        when(userRepository.findById("user-2")).thenReturn(Optional.of(member));

        var dto = workspaceService.addMemberToWorkspace("ws-1", "user-2", owner());

        assertThat(dto.getId()).isEqualTo("user-2");
        ArgumentCaptor<WorkspaceMember> cap = ArgumentCaptor.forClass(WorkspaceMember.class);
        verify(memberRepository).save(cap.capture());
        assertThat(cap.getValue().getRole()).isEqualTo(WorkspaceRole.MEMBER);
    }

    @Test
    void removeMember_existing_deletes() {
        WorkspaceMember member = WorkspaceMember.builder()
                .workspaceId("ws-1").userId("user-2").role(WorkspaceRole.MEMBER).build();
        when(memberRepository.findByWorkspaceIdAndUserId("ws-1", "user-2")).thenReturn(Optional.of(member));

        workspaceService.removeMemberFromWorkspace("ws-1", "user-2", owner());

        verify(memberRepository).delete(member);
    }

    @Test
    void removeMember_missing_throwsEntityNotFound() {
        when(memberRepository.findByWorkspaceIdAndUserId("ws-1", "user-2")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> workspaceService.removeMemberFromWorkspace("ws-1", "user-2", owner()))
                .isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    void getWorkspaceList_unpaged_returnsAll() {
        when(workspaceRepository.findByOwnerIdOrMemberUserId("owner-1")).thenReturn(List.of(workspace()));

        List<WorkspaceListDto> list = workspaceService.getWorkspaceList(owner(), -1, 0);

        assertThat(list).hasSize(1);
        assertThat(list.get(0).getTitle()).isEqualTo("Team Alpha");
    }

    @Test
    void getWorkspaceList_paged_returnsPageContent() {
        when(workspaceRepository.findByOwnerIdOrMemberUserIdPaginated(any(), any()))
                .thenReturn(new PageImpl<>(List.of(workspace())));

        List<WorkspaceListDto> list = workspaceService.getWorkspaceList(owner(), 0, 2);

        assertThat(list).hasSize(1);
    }
}
