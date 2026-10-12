package com.taskcenter.service;

import com.taskcenter.dto.InviteRequestDto;
import com.taskcenter.dto.WorkspaceInvitationDto;
import com.taskcenter.dto.WorkspaceMemberInviteRequest;
import com.taskcenter.dto.WorkspaceMemberResponseDto;
import com.taskcenter.exception.BadRequestException;
import com.taskcenter.exception.ConflictException;
import com.taskcenter.model.User;
import com.taskcenter.model.Workspace;
import com.taskcenter.model.WorkspaceRole;
import com.taskcenter.repository.UserRepository;
import com.taskcenter.repository.WorkspaceInvitationRepository;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WorkspaceMemberEmailInviteDelegationTest {

    @Mock
    private WorkspaceMemberRepository memberRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private WorkspaceRepository workspaceRepository;

    @Mock
    private WorkspaceAuthorizationService authorizationService;

    @Mock
    private WorkspaceInvitationRepository invitationRepository;

    @Mock
    private EmailService emailService;

    @Mock
    private NotificationService notificationService;

    @Mock
    private TelegramNotificationService telegramNotificationService;

    @Mock
    private WorkspaceInvitationService invitationService;

    @InjectMocks
    private WorkspaceMemberService memberService;

    private User adminUser;
    private Workspace workspace;
    private final String workspaceId = "ws-delegation-1";

    @BeforeEach
    void setUp() {
        adminUser = User.builder().id("admin-1").name("Admin").email("admin@test.uz").build();
        workspace = Workspace.builder().id(workspaceId).title("Test WS").ownerId("admin-1").build();
        lenient().when(authorizationService.checkAdmin(eq(workspaceId), any(User.class))).thenReturn(workspace);
    }

    @Test
    @DisplayName("Email taklifda OWNER roli berilsa BadRequestException tashlanishi kerak")
    void addMember_emailBranch_ownerRoleRejected() {
        String email = "invitee@example.com";
        when(userRepository.findByNameOrEmail(email)).thenReturn(Optional.empty());

        WorkspaceMemberInviteRequest request = WorkspaceMemberInviteRequest.builder()
                .usernameOrEmail(email)
                .role(WorkspaceRole.OWNER)
                .build();

        when(invitationService.inviteUser(eq(workspaceId), any(InviteRequestDto.class), eq(adminUser)))
                .thenThrow(new BadRequestException("INVALID_ROLE", "OWNER roli bilan taklif yuborib bo'lmaydi"));

        assertThatThrownBy(() -> memberService.addMember(workspaceId, request, adminUser))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("OWNER");

        verify(invitationService, times(1)).inviteUser(eq(workspaceId), argThat(req ->
                req.getUsernameOrEmail().equals(email) && req.getRole() == WorkspaceRole.OWNER
        ), eq(adminUser));
    }

    @Test
    @DisplayName("Allaqachon kutilayotgan (PENDING) taklif bo'lsa 409 ConflictException tashlanishi kerak")
    void addMember_emailBranch_duplicatePendingInvite_throwsConflict() {
        String email = "duplicate@example.com";
        when(userRepository.findByNameOrEmail(email)).thenReturn(Optional.empty());

        WorkspaceMemberInviteRequest request = WorkspaceMemberInviteRequest.builder()
                .usernameOrEmail(email)
                .role(WorkspaceRole.MEMBER)
                .build();

        when(invitationService.inviteUser(eq(workspaceId), any(InviteRequestDto.class), eq(adminUser)))
                .thenThrow(new ConflictException("DUPLICATE_PENDING_INVITE", "Ushbu email manziliga allaqachon kutilayotgan taklif yuborilgan"));

        assertThatThrownBy(() -> memberService.addMember(workspaceId, request, adminUser))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("allaqachon");

        verify(invitationService, times(1)).inviteUser(eq(workspaceId), argThat(req ->
                req.getUsernameOrEmail().equals(email) && req.getRole() == WorkspaceRole.MEMBER
        ), eq(adminUser));
    }

    @Test
    @DisplayName("To'g'ri email berilganda WorkspaceInvitationService ga delegatsiya qilinib MemberDto qaytishi kerak")
    void addMember_emailBranch_validInvite_delegatesSuccessfully() {
        String email = "newworker@example.com";
        when(userRepository.findByNameOrEmail(email)).thenReturn(Optional.empty());

        WorkspaceMemberInviteRequest request = WorkspaceMemberInviteRequest.builder()
                .usernameOrEmail(email)
                .role(WorkspaceRole.MEMBER)
                .build();

        WorkspaceInvitationDto inviteDto = WorkspaceInvitationDto.builder()
                .id("inv-delegated-1")
                .receiverEmail(email)
                .role(WorkspaceRole.MEMBER)
                .token("raw-token-abc-123")
                .build();

        when(invitationService.inviteUser(eq(workspaceId), any(InviteRequestDto.class), eq(adminUser)))
                .thenReturn(inviteDto);

        WorkspaceMemberResponseDto response = memberService.addMember(workspaceId, request, adminUser);

        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo("inv-delegated-1");
        assertThat(response.getEmail()).isEqualTo(email);
        assertThat(response.getRole()).isEqualTo(WorkspaceRole.MEMBER);
        assertThat(response.getName()).isEqualTo("Taklif etilgan");
        assertThat(response.getFullName()).isEqualTo("Taklif etilgan (" + email + ")");

        verify(invitationService, times(1)).inviteUser(eq(workspaceId), argThat(req ->
                req.getUsernameOrEmail().equals(email) && req.getRole() == WorkspaceRole.MEMBER
        ), eq(adminUser));
    }
}
