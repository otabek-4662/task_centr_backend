package com.taskcenter.service;

import com.taskcenter.dto.WorkspaceMemberInviteRequest;
import com.taskcenter.dto.WorkspaceMemberResponseDto;
import com.taskcenter.dto.WorkspaceMemberRoleUpdateRequest;
import com.taskcenter.exception.BadRequestException;
import com.taskcenter.exception.ConflictException;
import com.taskcenter.exception.ForbiddenException;
import com.taskcenter.model.User;
import com.taskcenter.model.Workspace;
import com.taskcenter.model.WorkspaceMember;
import com.taskcenter.model.WorkspaceRole;
import com.taskcenter.repository.UserRepository;
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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WorkspaceMemberServiceTest {

    @Mock
    private WorkspaceMemberRepository memberRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private WorkspaceRepository workspaceRepository;

    @Mock
    private WorkspaceAuthorizationService authorizationService;

    @InjectMocks
    private WorkspaceMemberService memberService;

    private final String workspaceId = "ws-1";
    private final String ownerId = "owner-1";
    private final String memberId = "user-2";
    private Workspace workspace;
    private User owner;
    private User newMember;

    @BeforeEach
    void setUp() {
        workspace = Workspace.builder()
                .id(workspaceId)
                .title("Test Workspace")
                .ownerId(ownerId)
                .build();

        owner = User.builder().id(ownerId).name("owner").email("owner@test.com").build();
        newMember = User.builder().id(memberId).name("ali").email("ali@test.com").build();
    }

    @Test
    @DisplayName("Yangi a'zo muvaffaqiyatli qo'shiladi")
    void addMember_success() {
        when(workspaceRepository.findById(workspaceId)).thenReturn(Optional.of(workspace));
        when(userRepository.findByNameOrEmail("ali")).thenReturn(Optional.of(newMember));
        when(memberRepository.existsByWorkspaceIdAndUserId(workspaceId, memberId)).thenReturn(false);

        WorkspaceMemberInviteRequest request = WorkspaceMemberInviteRequest.builder()
                .usernameOrEmail("ali")
                .role(WorkspaceRole.MEMBER)
                .build();

        WorkspaceMemberResponseDto response = memberService.addMember(workspaceId, request, owner);

        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(memberId);
        assertThat(response.getRole()).isEqualTo(WorkspaceRole.MEMBER);
        verify(memberRepository, times(1)).save(any(WorkspaceMember.class));
    }

    @Test
    @DisplayName("Allaqachon a'zo bo'lgan foydalanuvchini qayta qo'shishda ConflictException beradi")
    void addMember_alreadyExists_throwsConflict() {
        when(workspaceRepository.findById(workspaceId)).thenReturn(Optional.of(workspace));
        when(userRepository.findByNameOrEmail("ali")).thenReturn(Optional.of(newMember));
        when(memberRepository.existsByWorkspaceIdAndUserId(workspaceId, memberId)).thenReturn(true);

        WorkspaceMemberInviteRequest request = WorkspaceMemberInviteRequest.builder()
                .usernameOrEmail("ali")
                .role(WorkspaceRole.MEMBER)
                .build();

        assertThatThrownBy(() -> memberService.addMember(workspaceId, request, owner))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("allaqachon workspace a'zosi");
    }

    @Test
    @DisplayName("A'zoning rolini muvaffaqiyatli o'zgartirish")
    void updateMemberRole_success() {
        when(workspaceRepository.findById(workspaceId)).thenReturn(Optional.of(workspace));
        when(authorizationService.isOwner(workspaceId, ownerId)).thenReturn(true);

        WorkspaceMember existing = WorkspaceMember.builder()
                .workspaceId(workspaceId)
                .userId(memberId)
                .role(WorkspaceRole.MEMBER)
                .build();

        when(memberRepository.findByWorkspaceIdAndUserId(workspaceId, memberId)).thenReturn(Optional.of(existing));
        when(userRepository.findById(memberId)).thenReturn(Optional.of(newMember));

        WorkspaceMemberRoleUpdateRequest request = WorkspaceMemberRoleUpdateRequest.builder()
                .role(WorkspaceRole.ADMIN)
                .build();

        WorkspaceMemberResponseDto response = memberService.updateMemberRole(workspaceId, memberId, request, owner);

        assertThat(response.getRole()).isEqualTo(WorkspaceRole.ADMIN);
        verify(memberRepository, times(1)).save(existing);
    }

    @Test
    @DisplayName("Workspace egasining rolini o'zgartirmoqchi bo'lganda BadRequestException beradi")
    void updateMemberRole_cannotChangeOwner() {
        when(workspaceRepository.findById(workspaceId)).thenReturn(Optional.of(workspace));

        WorkspaceMemberRoleUpdateRequest request = WorkspaceMemberRoleUpdateRequest.builder()
                .role(WorkspaceRole.ADMIN)
                .build();

        assertThatThrownBy(() -> memberService.updateMemberRole(workspaceId, ownerId, request, owner))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Workspace egasining rolini o'zgartirib bo'lmaydi");
    }

    @Test
    @DisplayName("A'zoni jamoadan muvaffaqiyatli chiqarish")
    void removeMember_success() {
        when(workspaceRepository.findById(workspaceId)).thenReturn(Optional.of(workspace));
        when(authorizationService.isOwner(workspaceId, ownerId)).thenReturn(true);

        WorkspaceMember existing = WorkspaceMember.builder()
                .workspaceId(workspaceId)
                .userId(memberId)
                .role(WorkspaceRole.MEMBER)
                .build();

        when(memberRepository.findByWorkspaceIdAndUserId(workspaceId, memberId)).thenReturn(Optional.of(existing));

        memberService.removeMember(workspaceId, memberId, owner);

        verify(memberRepository, times(1)).delete(existing);
    }
}
