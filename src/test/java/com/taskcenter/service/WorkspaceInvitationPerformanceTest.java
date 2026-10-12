package com.taskcenter.service;

import com.taskcenter.dto.CreateLinkInviteRequest;
import com.taskcenter.dto.WorkspaceInvitationDto;
import com.taskcenter.model.*;
import com.taskcenter.repository.*;
import com.taskcenter.security.RateLimitingService;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import jakarta.persistence.EntityManager;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

@DataJpaTest
@ActiveProfiles("test")
public class WorkspaceInvitationPerformanceTest {

    @Autowired
    private WorkspaceInvitationRepository invitationRepository;

    @Autowired
    private WorkspaceMemberRepository memberRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private WorkspaceRepository workspaceRepository;

    @Autowired
    private PendingTelegramChatInviteRepository pendingChatInviteRepository;

    @Autowired
    private EntityManager em;

    private WorkspaceInvitationService invitationService;
    private Statistics stats;

    @BeforeEach
    void setUp() {
        WorkspaceAuthorizationService authService = new WorkspaceAuthorizationService(workspaceRepository, memberRepository);
        NotificationService notificationService = mock(NotificationService.class);
        EmailService emailService = mock(EmailService.class);
        TelegramNotificationService telegramNotificationService = mock(TelegramNotificationService.class);
        RateLimitingService rateLimitingService = mock(RateLimitingService.class);

        invitationService = new WorkspaceInvitationService(
                invitationRepository,
                memberRepository,
                userRepository,
                workspaceRepository,
                authService,
                notificationService,
                emailService,
                telegramNotificationService,
                pendingChatInviteRepository,
                rateLimitingService
        );

        SessionFactory sessionFactory = em.getEntityManagerFactory().unwrap(SessionFactory.class);
        stats = sessionFactory.getStatistics();
        stats.setStatisticsEnabled(true);
    }

    @Test
    @DisplayName("getMyPendingInvitations: 5 ta taklif uchun 1+2N o'rniga faqat 3 ta so'rov bajarilishi")
    void getMyPendingInvitations_executesBoundedQueries() {
        User receiver = userRepository.save(User.builder().id("recv-1").name("receiver").password("p").role(User.Role.USER).build());

        for (int i = 1; i <= 5; i++) {
            User sender = userRepository.save(User.builder().id("sender-" + i).name("sender" + i).fullName("Sender " + i).password("p").role(User.Role.USER).build());
            Workspace ws = workspaceRepository.save(Workspace.builder().id("ws-" + i).title("Workspace " + i).ownerId(sender.getId()).build());
            invitationRepository.save(WorkspaceInvitation.builder()
                    .id("inv-" + i)
                    .workspaceId(ws.getId())
                    .senderId(sender.getId())
                    .receiverId(receiver.getId())
                    .type(InvitationType.EMAIL)
                    .tokenHash("hash-" + i)
                    .status(InvitationStatus.PENDING)
                    .role(WorkspaceRole.MEMBER)
                    .expiresAt(LocalDateTime.now(ZoneOffset.UTC).plusDays(7))
                    .build());
        }

        em.flush();
        em.clear();

        stats.clear();
        long startCount = stats.getPrepareStatementCount();

        List<WorkspaceInvitationDto> result = invitationService.getMyPendingInvitations(receiver);

        long endCount = stats.getPrepareStatementCount();
        long queriesExecuted = endCount - startCount;

        assertThat(result).hasSize(5);
        // Eski mantiqda: 1 (takliflar) + 5 (workspaces) + 5 (senders) = 11 ta so'rov bo'lardi.
        // Batch loading bilan: 1 (takliflar) + 1 (workspaces IN) + 1 (senders IN) = 3 ta so'rov!
        assertThat(queriesExecuted).isLessThanOrEqualTo(3);
    }

    @Test
    @DisplayName("getWorkspaceInvitations: 5 ta taklif uchun 1+N o'rniga faqat 3 ta so'rov bajarilishi")
    void getWorkspaceInvitations_executesBoundedQueries() {
        User admin = userRepository.save(User.builder().id("admin-1").name("admin1").fullName("Admin One").password("p").role(User.Role.USER).build());
        Workspace ws = workspaceRepository.save(Workspace.builder().id("ws-shared").title("Shared WS").ownerId(admin.getId()).build());

        for (int i = 1; i <= 5; i++) {
            User sender = userRepository.save(User.builder().id("sender-ws-" + i).name("s" + i).fullName("Sender WS " + i).password("p").role(User.Role.USER).build());
            invitationRepository.save(WorkspaceInvitation.builder()
                    .id("inv-ws-" + i)
                    .workspaceId(ws.getId())
                    .senderId(sender.getId())
                    .type(InvitationType.EMAIL)
                    .tokenHash("hash-ws-" + i)
                    .status(InvitationStatus.PENDING)
                    .role(WorkspaceRole.MEMBER)
                    .expiresAt(LocalDateTime.now(ZoneOffset.UTC).plusDays(7))
                    .build());
        }

        em.flush();
        em.clear();

        stats.clear();
        long startCount = stats.getPrepareStatementCount();

        List<WorkspaceInvitationDto> result = invitationService.getWorkspaceInvitations(ws.getId(), admin);

        long endCount = stats.getPrepareStatementCount();
        long queriesExecuted = endCount - startCount;

        assertThat(result).hasSize(5);
        // Eski mantiqda: 1 (admin tekshirish) + 1 (takliflar) + 5 (senders) = 7 ta so'rov bo'lardi.
        // Optimallashtirilgandan keyin: 1 (admin) + 1 (takliflar) + 1 (senders IN) = 3 ta so'rov!
        assertThat(queriesExecuted).isLessThanOrEqualTo(3);
    }

    @Test
    @DisplayName("regenerateLinkInvite: Eski havola takliflarni bekor qilish bitta bulk UPDATE da bajarilishi")
    void regenerateLinkInvite_bulkUpdateExecutesSingleUpdateStatement() {
        User admin = userRepository.save(User.builder().id("admin-reg").name("adminreg").fullName("Admin Reg").password("p").role(User.Role.USER).build());
        Workspace ws = workspaceRepository.save(Workspace.builder().id("ws-reg").title("Reg WS").ownerId(admin.getId()).build());

        for (int i = 1; i <= 5; i++) {
            invitationRepository.save(WorkspaceInvitation.builder()
                    .id("inv-link-" + i)
                    .workspaceId(ws.getId())
                    .senderId(admin.getId())
                    .type(InvitationType.LINK)
                    .tokenHash("hash-link-" + i)
                    .status(InvitationStatus.PENDING)
                    .role(WorkspaceRole.MEMBER)
                    .expiresAt(LocalDateTime.now(ZoneOffset.UTC).plusDays(7))
                    .build());
        }

        em.flush();
        em.clear();

        stats.clear();
        long startCount = stats.getPrepareStatementCount();

        CreateLinkInviteRequest req = CreateLinkInviteRequest.builder().role(WorkspaceRole.MEMBER).durationDays(7).build();
        WorkspaceInvitationDto newInvite = invitationService.regenerateLinkInvite(ws.getId(), req, admin);

        long endCount = stats.getPrepareStatementCount();
        long queriesExecuted = endCount - startCount;

        assertThat(newInvite).isNotNull();
        // Eski mantiqda: 1 (checkAdmin) + 1 (SELECT 5 invites) + 5 (individual UPDATE) + 1 (duplicate checkAdmin) + 1 (INSERT) = 9 ta so'rov.
        // Bulk update bilan: 1 (checkAdmin) + 1 (bulk UPDATE) + 1 (INSERT) = 3 ta so'rov!
        assertThat(queriesExecuted).isLessThanOrEqualTo(4);

        // Barcha 5 ta eski link taklif CANCELLED bo'lganini tekshirish
        List<WorkspaceInvitation> cancelled = invitationRepository.findByWorkspaceIdAndStatus(ws.getId(), InvitationStatus.CANCELLED);
        assertThat(cancelled).hasSize(5);
    }
}
