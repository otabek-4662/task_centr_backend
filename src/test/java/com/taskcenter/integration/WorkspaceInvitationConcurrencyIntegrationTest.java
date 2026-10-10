package com.taskcenter.integration;

import com.taskcenter.exception.ConflictException;
import com.taskcenter.exception.BadRequestException;
import com.taskcenter.model.*;
import com.taskcenter.repository.UserRepository;
import com.taskcenter.repository.WorkspaceInvitationRepository;
import com.taskcenter.repository.WorkspaceMemberRepository;
import com.taskcenter.repository.WorkspaceRepository;
import com.taskcenter.service.WorkspaceInvitationService;
import com.taskcenter.util.InvitationTokenUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Testcontainers
@ActiveProfiles("test")
@EnabledIf("isDockerAvailable")
public class WorkspaceInvitationConcurrencyIntegrationTest {

    static boolean isDockerAvailable() {
        boolean isCi = "true".equalsIgnoreCase(System.getenv("CI")) || "true".equalsIgnoreCase(System.getProperty("CI"));
        try {
            boolean available = DockerClientFactory.instance().isDockerAvailable();
            if (!available && isCi) {
                org.junit.jupiter.api.Assertions.fail("CI muhitida (CI=true) Docker mavjud bo'lishi shart, lekin Testcontainers Docker'ni topa olmadi");
            }
            return available;
        } catch (org.opentest4j.AssertionFailedError afe) {
            throw afe;
        } catch (Throwable t) {
            if (isCi) {
                org.junit.jupiter.api.Assertions.fail("CI muhitida (CI=true) Docker mavjud bo'lishi shart, lekin xatolik yuz berdi: " + t.getMessage());
            }
            return false;
        }
    }

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine")
            .withDatabaseName("testdb")
            .withUsername("testuser")
            .withPassword("testpass");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
        registry.add("spring.jpa.properties.hibernate.dialect", () -> "org.hibernate.dialect.PostgreSQLDialect");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
        registry.add("spring.flyway.enabled", () -> "true");
        registry.add("flyway.enabled", () -> "true");
    }

    @Autowired
    private WorkspaceInvitationRepository invitationRepository;

    @Autowired
    private WorkspaceMemberRepository memberRepository;

    @Autowired
    private WorkspaceRepository workspaceRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private WorkspaceInvitationService invitationService;

    @Autowired
    private org.springframework.transaction.support.TransactionTemplate transactionTemplate;

    private User owner;
    private Workspace workspace;

    @BeforeEach
    void setUp() {
        String randomSuffix = UUID.randomUUID().toString().substring(0, 8);
        owner = userRepository.save(User.builder()
                .name("owner_" + randomSuffix)
                .fullName("Owner User")
                .password("pass123")
                .role(User.Role.USER)
                .build());

        workspace = workspaceRepository.save(Workspace.builder()
                .title("Concurrency Test WS")
                .ownerId(owner.getId())
                .build());
    }

    @Test
    @DisplayName("7. Real DB: incrementUseCountAtomic parallel 20 ta oqimda chaqirilganda max_uses (5) dan oshib ketmasligi")
    void testAtomicIncrementUseCountParallel_enforcesLimitStrictly() throws InterruptedException {
        int maxUses = 5;
        int totalThreads = 20;

        String rawToken = InvitationTokenUtil.generateToken();
        String tokenHash = InvitationTokenUtil.hashToken(rawToken);

        WorkspaceInvitation invitation = invitationRepository.save(WorkspaceInvitation.builder()
                .workspaceId(workspace.getId())
                .senderId(owner.getId())
                .type(InvitationType.LINK)
                .role(WorkspaceRole.MEMBER)
                .status(InvitationStatus.PENDING)
                .tokenHash(tokenHash)
                .maxUses(maxUses)
                .useCount(0)
                .emailDeliveryStatus(EmailDeliveryStatus.NOT_APPLICABLE)
                .expiresAt(LocalDateTime.now().plusDays(7))
                .build());

        ExecutorService executor = Executors.newFixedThreadPool(totalThreads);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch finishLatch = new CountDownLatch(totalThreads);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);

        for (int i = 0; i < totalThreads; i++) {
            executor.submit(() -> {
                try {
                    startLatch.await(); // Barcha oqimlar bir vaqtning o'zida boshlaydi
                    Integer rows = transactionTemplate.execute(status ->
                            invitationRepository.incrementUseCountAtomic(invitation.getId(), LocalDateTime.now())
                    );
                    if (rows != null && rows == 1) {
                        successCount.incrementAndGet();
                    } else {
                        failureCount.incrementAndGet();
                    }
                } catch (Exception e) {
                    failureCount.incrementAndGet();
                } finally {
                    finishLatch.countDown();
                }
            });
        }

        startLatch.countDown(); // Start trigger
        assertTrue(finishLatch.await(10, TimeUnit.SECONDS), "Parallel so'rovlar belgilangan vaqtda yakunlanishi kerak");
        executor.shutdown();

        assertEquals(maxUses, successCount.get(), "Faqat roppa-rosa max_uses (5) ta urinish 1 (muvaffaqiyat) qaytarishi kerak");
        assertEquals(totalThreads - maxUses, failureCount.get(), "Qolgan 15 ta urinish 0 qaytarishi kerak");

        WorkspaceInvitation reloaded = invitationRepository.findById(invitation.getId()).orElseThrow();
        assertEquals(maxUses, reloaded.getUseCount(), "Bazada saqlangan use_count roppa-rosa 5 bo'lishi shart");
        assertEquals(InvitationStatus.ACCEPTED, reloaded.getStatus(), "Limit to'lganda status ACCEPTED ga o'tishi kerak");
    }

    @Test
    @DisplayName("3 & 7. Real DB: acceptInvitation bir nechta har xil userlar bilan parallel qabul qilinganda max_uses (3) ta a'zo qo'shiladi")
    void testAcceptInvitationServiceConcurrency_realDatabase() throws InterruptedException {
        int maxUses = 3;
        int totalUsers = 8;

        String rawToken = InvitationTokenUtil.generateToken();
        String tokenHash = InvitationTokenUtil.hashToken(rawToken);

        WorkspaceInvitation invitation = invitationRepository.save(WorkspaceInvitation.builder()
                .workspaceId(workspace.getId())
                .senderId(owner.getId())
                .type(InvitationType.LINK)
                .role(WorkspaceRole.MEMBER)
                .status(InvitationStatus.PENDING)
                .tokenHash(tokenHash)
                .maxUses(maxUses)
                .useCount(0)
                .emailDeliveryStatus(EmailDeliveryStatus.NOT_APPLICABLE)
                .expiresAt(LocalDateTime.now().plusDays(7))
                .build());

        List<User> testUsers = new ArrayList<>();
        for (int i = 0; i < totalUsers; i++) {
            String suffix = UUID.randomUUID().toString().substring(0, 8);
            User u = userRepository.save(User.builder()
                    .name("par_user_" + i + "_" + suffix)
                    .fullName("Parallel User " + i)
                    .password("pass123")
                    .role(User.Role.USER)
                    .build());
            testUsers.add(u);
        }

        ExecutorService executor = Executors.newFixedThreadPool(totalUsers);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch finishLatch = new CountDownLatch(totalUsers);

        AtomicInteger acceptedCount = new AtomicInteger(0);
        AtomicInteger rejectedCount = new AtomicInteger(0);

        for (User user : testUsers) {
            executor.submit(() -> {
                try {
                    startLatch.await();
                    invitationService.acceptInvitation(rawToken, user);
                    acceptedCount.incrementAndGet();
                } catch (BadRequestException e) {
                    rejectedCount.incrementAndGet();
                } catch (Exception e) {
                    rejectedCount.incrementAndGet();
                } finally {
                    finishLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        assertTrue(finishLatch.await(10, TimeUnit.SECONDS));
        executor.shutdown();

        assertEquals(maxUses, acceptedCount.get(), "Roppa-rosa 3 ta user muvaffaqiyatli a'zo bo'lishi kerak");
        assertEquals(totalUsers - maxUses, rejectedCount.get(), "Qolgan 5 ta user limit tugaganligi sabab rad etilishi kerak");

        WorkspaceInvitation finalInv = invitationRepository.findById(invitation.getId()).orElseThrow();
        assertEquals(maxUses, finalInv.getUseCount());

        long memberCount = memberRepository.findAll().stream()
                .filter(m -> m.getWorkspaceId().equals(workspace.getId()))
                .count();
        assertEquals(maxUses, memberCount, "Ishchi maydonga roppa-rosa 3 ta yangi a'zo qo'shilgan bo'lishi kerak");
    }

    @Test
    @DisplayName("3. Real DB: acceptInvitation - foydalanuvchi allaqachon a'zo bo'lsa use_count oshirilmaydi")
    void testAcceptInvitation_alreadyMember_doesNotIncrementUseCount() {
        String rawToken = InvitationTokenUtil.generateToken();
        String tokenHash = InvitationTokenUtil.hashToken(rawToken);

        WorkspaceInvitation invitation = invitationRepository.save(WorkspaceInvitation.builder()
                .workspaceId(workspace.getId())
                .senderId(owner.getId())
                .type(InvitationType.LINK)
                .role(WorkspaceRole.MEMBER)
                .status(InvitationStatus.PENDING)
                .tokenHash(tokenHash)
                .maxUses(10)
                .useCount(2)
                .emailDeliveryStatus(EmailDeliveryStatus.NOT_APPLICABLE)
                .expiresAt(LocalDateTime.now().plusDays(7))
                .build());

        User memberUser = userRepository.save(User.builder()
                .name("existing_member_" + UUID.randomUUID().toString().substring(0, 6))
                .fullName("Existing Member")
                .password("pass123")
                .role(User.Role.USER)
                .build());

        memberRepository.save(WorkspaceMember.builder()
                .workspaceId(workspace.getId())
                .userId(memberUser.getId())
                .role(WorkspaceRole.MEMBER)
                .build());

        assertThrows(ConflictException.class, () -> invitationService.acceptInvitation(rawToken, memberUser));

        WorkspaceInvitation unchanged = invitationRepository.findById(invitation.getId()).orElseThrow();
        assertEquals(2, unchanged.getUseCount(), "Foydalanuvchi allaqachon a'zo bo'lganda use_count o'zgarmasligi shart (2 qolishi kerak)");
    }
}
