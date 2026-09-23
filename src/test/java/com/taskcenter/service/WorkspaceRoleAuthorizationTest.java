package com.taskcenter.service;

import com.taskcenter.exception.ForbiddenException;
import com.taskcenter.model.User;
import com.taskcenter.model.Workspace;
import com.taskcenter.model.WorkspaceMember;
import com.taskcenter.model.WorkspaceRole;
import com.taskcenter.repository.WorkspaceMemberRepository;
import com.taskcenter.repository.WorkspaceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WorkspaceRoleAuthorizationTest {

    @Mock
    private WorkspaceRepository workspaceRepository;

    @Mock
    private WorkspaceMemberRepository memberRepository;

    @InjectMocks
    private WorkspaceAuthorizationService authorizationService;

    private final String workspaceId = "ws-100";
    private final String ownerId = "user-owner";
    private final String adminId = "user-admin";
    private final String memberId = "user-member";
    private final String viewerId = "user-viewer";
    private final String strangerId = "user-stranger";

    private Workspace workspace;

    @BeforeEach
    void setUp() {
        workspace = Workspace.builder()
                .id(workspaceId)
                .title("Jira Test Workspace")
                .ownerId(ownerId)
                .build();
    }

    @Test
    @DisplayName("1. Administrator (Owner yoki Admin) to'liq kirish va tahrirlash huquqiga ega")
    void admin_hasFullAccessAndEditPermissions() {
        when(workspaceRepository.findById(workspaceId)).thenReturn(Optional.of(workspace));
        when(memberRepository.findByWorkspaceIdAndUserId(workspaceId, adminId))
                .thenReturn(Optional.of(WorkspaceMember.builder().workspaceId(workspaceId).userId(adminId).role(WorkspaceRole.ADMIN).build()));

        User adminUser = User.builder().id(adminId).build();

        // O'qish huquqi
        assertThat(authorizationService.hasAccess(workspaceId, adminId)).isTrue();
        assertThatCode(() -> authorizationService.checkAccess(workspaceId, adminUser)).doesNotThrowAnyException();

        // Tahrirlash huquqi
        assertThat(authorizationService.canEdit(workspaceId, adminId)).isTrue();
        assertThatCode(() -> authorizationService.checkCanEdit(workspaceId, adminUser)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("2. Muharrir (Member) o'qish va tahrirlash huquqiga ega, lekin admin emas")
    void member_hasEditPermissions_butNotAdmin() {
        when(workspaceRepository.findById(workspaceId)).thenReturn(Optional.of(workspace));
        when(memberRepository.findByWorkspaceIdAndUserId(workspaceId, memberId))
                .thenReturn(Optional.of(WorkspaceMember.builder().workspaceId(workspaceId).userId(memberId).role(WorkspaceRole.MEMBER).build()));

        User memberUser = User.builder().id(memberId).build();

        // O'qish huquqi
        assertThat(authorizationService.hasAccess(workspaceId, memberId)).isTrue();
        assertThatCode(() -> authorizationService.checkAccess(workspaceId, memberUser)).doesNotThrowAnyException();

        // Tahrirlash huquqi
        assertThat(authorizationService.canEdit(workspaceId, memberId)).isTrue();
        assertThatCode(() -> authorizationService.checkCanEdit(workspaceId, memberUser)).doesNotThrowAnyException();

        // Admin emas
        assertThat(authorizationService.isOwnerOrAdmin(workspaceId, memberId)).isFalse();
    }

    @Test
    @DisplayName("3. Kuzatuvchi (Viewer) faqat o'qiy oladi, tahrirlashda ForbiddenException oladi")
    void viewer_canOnlyRead_cannotEdit() {
        when(workspaceRepository.findById(workspaceId)).thenReturn(Optional.of(workspace));
        when(memberRepository.findByWorkspaceIdAndUserId(workspaceId, viewerId))
                .thenReturn(Optional.of(WorkspaceMember.builder().workspaceId(workspaceId).userId(viewerId).role(WorkspaceRole.VIEWER).build()));

        User viewerUser = User.builder().id(viewerId).build();

        // O'qish huquqi (GET ruxsat)
        assertThat(authorizationService.hasAccess(workspaceId, viewerId)).isTrue();
        assertThat(authorizationService.isViewer(workspaceId, viewerId)).isTrue();
        assertThatCode(() -> authorizationService.checkAccess(workspaceId, viewerUser)).doesNotThrowAnyException();

        // Tahrirlash taqiqlangan (POST/PUT/DELETE taqiq)
        assertThat(authorizationService.canEdit(workspaceId, viewerId)).isFalse();
        assertThatThrownBy(() -> authorizationService.checkCanEdit(workspaceId, viewerUser))
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("Kuzatuvchi (Viewer) roli ma'lumotlarni o'zgartira olmaydi");
    }

    @Test
    @DisplayName("4. Workspace a'zosi bo'lmagan foydalanuvchiga umuman ruxsat yo'q")
    void stranger_hasNoAccessAtAll() {
        when(workspaceRepository.findById(workspaceId)).thenReturn(Optional.of(workspace));
        when(memberRepository.findByWorkspaceIdAndUserId(workspaceId, strangerId))
                .thenReturn(Optional.empty());

        User strangerUser = User.builder().id(strangerId).build();

        assertThat(authorizationService.hasAccess(workspaceId, strangerId)).isFalse();
        assertThatThrownBy(() -> authorizationService.checkAccess(workspaceId, strangerUser))
                .isInstanceOf(ForbiddenException.class);
    }
}
