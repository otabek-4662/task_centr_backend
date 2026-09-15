package com.taskcenter.service;

import com.taskcenter.exception.EntityNotFoundException;
import com.taskcenter.exception.ForbiddenException;
import com.taskcenter.model.User;
import com.taskcenter.model.Workspace;
import com.taskcenter.model.WorkspaceMember;
import com.taskcenter.model.WorkspaceRole;
import com.taskcenter.repository.WorkspaceMemberRepository;
import com.taskcenter.repository.WorkspaceRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WorkspaceAuthorizationServiceTest {

    @Mock
    private WorkspaceRepository workspaceRepository;
    @Mock
    private WorkspaceMemberRepository memberRepository;

    @InjectMocks
    private WorkspaceAuthorizationService authorizationService;

    private Workspace workspace() {
        return Workspace.builder().id("ws-1").title("WS").ownerId("owner-1").keyPrefix("W").build();
    }

    private User user(String id) {
        return User.builder().id(id).name("u-" + id).email(id + "@test.local").password("x").build();
    }

    private WorkspaceMember member(String userId, WorkspaceRole role) {
        return WorkspaceMember.builder().workspaceId("ws-1").userId(userId).role(role).build();
    }

    @Test
    void getRole_owner_returnsOwnerWithoutMemberLookup() {
        when(workspaceRepository.findById("ws-1")).thenReturn(Optional.of(workspace()));

        assertThat(authorizationService.getRole("ws-1", "owner-1")).contains(WorkspaceRole.OWNER);
        verify(memberRepository, never()).findByWorkspaceIdAndUserId(any(), any());
    }

    @Test
    void getRole_member_returnsMemberRole() {
        when(workspaceRepository.findById("ws-1")).thenReturn(Optional.of(workspace()));
        when(memberRepository.findByWorkspaceIdAndUserId("ws-1", "user-2"))
                .thenReturn(Optional.of(member("user-2", WorkspaceRole.MEMBER)));

        assertThat(authorizationService.getRole("ws-1", "user-2")).contains(WorkspaceRole.MEMBER);
    }

    @Test
    void getRole_unknownWorkspace_returnsEmpty() {
        when(workspaceRepository.findById("ws-1")).thenReturn(Optional.empty());

        assertThat(authorizationService.getRole("ws-1", "user-2")).isEmpty();
    }

    @Test
    void getRole_nullUser_returnsEmpty() {
        when(workspaceRepository.findById("ws-1")).thenReturn(Optional.of(workspace()));

        assertThat(authorizationService.getRole("ws-1", null)).isEmpty();
    }

    @Test
    void hasAccess_member_returnsTrue() {
        when(workspaceRepository.findById("ws-1")).thenReturn(Optional.of(workspace()));
        when(memberRepository.findByWorkspaceIdAndUserId("ws-1", "user-2"))
                .thenReturn(Optional.of(member("user-2", WorkspaceRole.MEMBER)));

        assertThat(authorizationService.hasAccess("ws-1", "user-2")).isTrue();
    }

    @Test
    void hasAccess_stranger_returnsFalse() {
        when(workspaceRepository.findById("ws-1")).thenReturn(Optional.of(workspace()));
        when(memberRepository.findByWorkspaceIdAndUserId("ws-1", "stranger")).thenReturn(Optional.empty());

        assertThat(authorizationService.hasAccess("ws-1", "stranger")).isFalse();
    }

    @Test
    void isOwnerOrAdmin_admin_returnsTrue() {
        when(workspaceRepository.findById("ws-1")).thenReturn(Optional.of(workspace()));
        when(memberRepository.findByWorkspaceIdAndUserId("ws-1", "admin-1"))
                .thenReturn(Optional.of(member("admin-1", WorkspaceRole.ADMIN)));

        assertThat(authorizationService.isOwnerOrAdmin("ws-1", "admin-1")).isTrue();
    }

    @Test
    void isUserMemberOfWorkspace_delegatesToRepository() {
        when(memberRepository.existsByWorkspaceIdAndUserId("ws-1", "user-2")).thenReturn(true, false);

        assertThat(authorizationService.isUserMemberOfWorkspace("ws-1", "user-2")).isTrue();
        assertThat(authorizationService.isUserMemberOfWorkspace("ws-1", "user-2")).isFalse();
    }

    @Test
    void checkAccess_stranger_throwsForbidden() {
        when(workspaceRepository.findById("ws-1")).thenReturn(Optional.of(workspace()));
        when(memberRepository.findByWorkspaceIdAndUserId("ws-1", "stranger")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authorizationService.checkAccess("ws-1", user("stranger")))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void checkAccess_missingWorkspace_throwsEntityNotFound() {
        when(workspaceRepository.findById("ws-1")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authorizationService.checkAccess("ws-1", user("owner-1")))
                .isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    void checkOwnerOrAdmin_member_throwsWithMessage() {
        when(workspaceRepository.findById("ws-1")).thenReturn(Optional.of(workspace()));
        when(memberRepository.findByWorkspaceIdAndUserId("ws-1", "user-2"))
                .thenReturn(Optional.of(member("user-2", WorkspaceRole.MEMBER)));

        assertThatThrownBy(() -> authorizationService.checkOwnerOrAdmin("ws-1", user("user-2")))
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("admin");
    }
}
