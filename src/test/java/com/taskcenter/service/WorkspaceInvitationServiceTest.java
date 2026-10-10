package com.taskcenter.service;

import com.taskcenter.dto.*;
import com.taskcenter.exception.BadRequestException;
import com.taskcenter.exception.ConflictException;
import com.taskcenter.exception.ForbiddenException;
import com.taskcenter.exception.RateLimitException;
import com.taskcenter.exception.ResourceNotFoundException;
import com.taskcenter.model.*;
import com.taskcenter.repository.*;
import com.taskcenter.security.RateLimitingService;
import com.taskcenter.util.InvitationTokenUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WorkspaceInvitationServiceTest {

    @Mock
    private WorkspaceInvitationRepository invitationRepository;

    @Mock
    private WorkspaceMemberRepository memberRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private WorkspaceRepository workspaceRepository;

    @Mock
    private WorkspaceAuthorizationService authorizationService;

    @Mock
    private NotificationService notificationService;

    @Mock
    private EmailService emailService;

    @Mock
    private TelegramNotificationService telegramNotificationService;

    @Mock
    private PendingTelegramChatInviteRepository pendingChatInviteRepository;

    @Mock
    private RateLimitingService rateLimitingService;

    @InjectMocks
    private WorkspaceInvitationService invitationService;

    private User currentUser;
    private Workspace workspace;

    @BeforeEach
    void setUp() {
        currentUser = User.builder().id("admin-1").name("admin").email("admin@test.com").build();
        workspace = Workspace.builder().id("ws-1").title("Super Loyiha").ownerId("admin-1").build();

        ReflectionTestUtils.setField(invitationService, "botUsername", "test_bot");
        ReflectionTestUtils.setField(invitationService, "frontendUrl", "https://task-center.uz");
        ReflectionTestUtils.setField(invitationService, "miniappShortName", "app");

        lenient().when(authorizationService.checkAdmin(eq("ws-1"), any(User.class))).thenReturn(workspace);
    }

    @Test
    @DisplayName("Email orqali taklif yuborish - muvaffaqiyatli va Brevo orqali jo'natiladi")
    void inviteUser_byEmail_success() {
        InviteRequestDto request = new InviteRequestDto();
        request.setUsernameOrEmail("hamkasb@example.com");
        request.setRole(WorkspaceRole.MEMBER);

        when(userRepository.findByNameOrEmail("hamkasb@example.com")).thenReturn(Optional.empty());
        when(invitationRepository.findByWorkspaceIdAndReceiverEmailIgnoreCaseAndStatus("ws-1", "hamkasb@example.com", InvitationStatus.PENDING))
                .thenReturn(Optional.empty());
        when(invitationRepository.countByWorkspaceIdAndStatus("ws-1", InvitationStatus.PENDING)).thenReturn(2L);
        when(invitationRepository.save(any(WorkspaceInvitation.class))).thenAnswer(inv -> {
            WorkspaceInvitation saved = inv.getArgument(0);
            saved.setId("inv-123");
            return saved;
        });
        when(emailService.sendInvitationEmailDirect(anyString(), anyString(), anyString(), anyString(), anyString()))
                .thenReturn(new EmailService.EmailDeliveryResult(EmailDeliveryStatus.SENT, null));

        WorkspaceInvitationDto result = invitationService.inviteUser("ws-1", request, currentUser);

        assertNotNull(result);
        assertEquals("inv-123", result.getId());
        assertEquals("hamkasb@example.com", result.getReceiverEmail());
        assertEquals(InvitationType.EMAIL, result.getType());
        assertEquals(EmailDeliveryStatus.SENT, result.getEmailDeliveryStatus());
        assertNotNull(result.getToken());
        assertTrue(result.getToken().length() >= 32);
        assertTrue(result.getTelegramInviteLink().contains("start=inv_" + result.getToken()));
        assertTrue(result.getTelegramMiniappLink().contains("startapp=inv_" + result.getToken()));
        assertTrue(result.getInviteLink().contains("/invite/" + result.getToken()));

        verify(emailService, times(1)).sendInvitationEmailDirect(
                eq("hamkasb@example.com"), eq("Super Loyiha"), eq("admin"), eq("MEMBER"), eq(result.getToken())
        );
    }

    @Test
    @DisplayName("Takroriy taklif yuborish - allaqachon pending bo'lsa ConflictException")
    void inviteUser_duplicatePendingInvite_throwsConflict() {
        InviteRequestDto request = new InviteRequestDto();
        request.setUsernameOrEmail("pending@example.com");
        request.setRole(WorkspaceRole.MEMBER);

        WorkspaceInvitation pendingInv = WorkspaceInvitation.builder()
                .id("inv-existing")
                .receiverEmail("pending@example.com")
                .status(InvitationStatus.PENDING)
                .build();

        when(userRepository.findByNameOrEmail("pending@example.com")).thenReturn(Optional.empty());
        when(invitationRepository.findByWorkspaceIdAndReceiverEmailIgnoreCaseAndStatus("ws-1", "pending@example.com", InvitationStatus.PENDING))
                .thenReturn(Optional.of(pendingInv));

        assertThrows(ConflictException.class, () -> invitationService.inviteUser("ws-1", request, currentUser));
    }

    @Test
    @DisplayName("Allaqachon a'zo bo'lgan foydalanuvchiga taklif yuborish - ConflictException")
    void inviteUser_alreadyMember_throwsConflict() {
        User existingUser = User.builder().id("u-member").name("member").email("member@example.com").build();

        InviteRequestDto request = new InviteRequestDto();
        request.setUsernameOrEmail("member@example.com");
        request.setRole(WorkspaceRole.MEMBER);

        when(userRepository.findByNameOrEmail("member@example.com")).thenReturn(Optional.of(existingUser));
        when(memberRepository.existsByWorkspaceIdAndUserId("ws-1", "u-member")).thenReturn(true);

        assertThrows(ConflictException.class, () -> invitationService.inviteUser("ws-1", request, currentUser));
    }

    @Test
    @DisplayName("OWNER rolini taklif qilib bo'lmaydi - BadRequestException")
    void inviteUser_withOwnerRole_throwsBadRequest() {
        InviteRequestDto request = new InviteRequestDto();
        request.setUsernameOrEmail("someone@example.com");
        request.setRole(WorkspaceRole.OWNER);

        assertThrows(BadRequestException.class, () -> invitationService.inviteUser("ws-1", request, currentUser));
    }

    @Test
    @DisplayName("Havola-taklif yaratish - yangi havola bo'lsa rawToken qaytadi")
    void createLinkInvite_success() {
        CreateLinkInviteRequest request = new CreateLinkInviteRequest();
        request.setRole(WorkspaceRole.MEMBER);
        request.setMaxUses(10);
        request.setDurationDays(7);

        when(invitationRepository.findByWorkspaceIdAndTypeAndStatus("ws-1", InvitationType.LINK, InvitationStatus.PENDING))
                .thenReturn(Optional.empty());
        when(invitationRepository.countByWorkspaceIdAndStatus("ws-1", InvitationStatus.PENDING)).thenReturn(1L);
        when(invitationRepository.save(any(WorkspaceInvitation.class))).thenAnswer(inv -> {
            WorkspaceInvitation saved = inv.getArgument(0);
            saved.setId("inv-link-1");
            return saved;
        });

        WorkspaceInvitationDto result = invitationService.createOrGetLinkInvite("ws-1", request, currentUser);

        assertNotNull(result);
        assertEquals("inv-link-1", result.getId());
        assertEquals(InvitationType.LINK, result.getType());
        assertEquals(10, result.getMaxUses());
        assertEquals(0, result.getUseCount());
        assertNotNull(result.getToken());
        assertTrue(result.getTelegramInviteLink().contains("start=inv_" + result.getToken()));
        assertTrue(result.getTelegramMiniappLink().contains("startapp=inv_" + result.getToken()));
        assertTrue(result.getInviteLink().contains("/invite/" + result.getToken()));
    }

    @Test
    @DisplayName("Havola-taklif olish: mavjud faol havola bo'lsa xom token xavfsizlik uchun yashiriladi (token = null)")
    void createLinkInvite_existingActiveLink_returnsWithoutRawToken() {
        WorkspaceInvitation existing = WorkspaceInvitation.builder()
                .id("inv-existing-link")
                .workspaceId("ws-1")
                .type(InvitationType.LINK)
                .status(InvitationStatus.PENDING)
                .role(WorkspaceRole.MEMBER)
                .maxUses(10)
                .useCount(2)
                .expiresAt(LocalDateTime.now().plusDays(5))
                .build();

        when(invitationRepository.findByWorkspaceIdAndTypeAndStatus("ws-1", InvitationType.LINK, InvitationStatus.PENDING))
                .thenReturn(Optional.of(existing));

        WorkspaceInvitationDto result = invitationService.createOrGetLinkInvite("ws-1", new CreateLinkInviteRequest(), currentUser);

        assertNotNull(result);
        assertEquals("inv-existing-link", result.getId());
        assertNull(result.getToken(), "Bazada hash saqlangani uchun xom token mavjud taklifda qaytarilmasligi kerak");
        assertNull(result.getInviteLink());
    }

    @Test
    @DisplayName("Havolani yangilash (regenerate) - eski bekor qilinadi va yangi xom token yaratiladi")
    void regenerateLinkInvite_success() {
        WorkspaceInvitation oldLink = WorkspaceInvitation.builder()
                .id("old-link-id")
                .workspaceId("ws-1")
                .type(InvitationType.LINK)
                .status(InvitationStatus.PENDING)
                .role(WorkspaceRole.MEMBER)
                .maxUses(5)
                .build();

        when(invitationRepository.findByWorkspaceIdAndStatus("ws-1", InvitationStatus.PENDING))
                .thenReturn(List.of(oldLink));
        when(invitationRepository.save(any(WorkspaceInvitation.class))).thenAnswer(inv -> {
            WorkspaceInvitation saved = inv.getArgument(0);
            if (saved.getId() == null) {
                saved.setId("new-link-id");
            }
            return saved;
        });

        CreateLinkInviteRequest request = new CreateLinkInviteRequest();
        request.setRole(WorkspaceRole.ADMIN);
        request.setMaxUses(20);

        WorkspaceInvitationDto result = invitationService.regenerateLinkInvite("ws-1", request, currentUser);

        assertNotNull(result);
        assertEquals("new-link-id", result.getId());
        assertEquals(InvitationStatus.CANCELLED, oldLink.getStatus());
        assertEquals(20, result.getMaxUses());
        assertNotNull(result.getToken(), "Regenerate qilinganda yangi xom token qaytishi shart");
    }

    @Test
    @DisplayName("Email taklifni to'g'ri egasi qabul qilishi - atomik qabul muvaffaqiyatli")
    void acceptInvitation_emailInvite_success() {
        String rawToken = InvitationTokenUtil.generateToken();
        String tokenHash = InvitationTokenUtil.hashToken(rawToken);

        WorkspaceInvitation inv = WorkspaceInvitation.builder()
                .id("inv-email-1")
                .workspaceId("ws-1")
                .senderId("admin-1")
                .receiverEmail("target@test.com")
                .type(InvitationType.EMAIL)
                .role(WorkspaceRole.MEMBER)
                .status(InvitationStatus.PENDING)
                .tokenHash(tokenHash)
                .expiresAt(LocalDateTime.now().plusDays(2))
                .build();

        User recipient = User.builder().id("user-99").name("targetUser").email("target@test.com").build();

        when(invitationRepository.findByTokenHash(tokenHash)).thenReturn(Optional.of(inv));
        when(memberRepository.existsByWorkspaceIdAndUserId("ws-1", "user-99")).thenReturn(false);
        when(invitationRepository.acceptEmailInvitationAtomic(eq("inv-email-1"), eq("user-99"), any(LocalDateTime.class))).thenReturn(1);
        when(workspaceRepository.findById("ws-1")).thenReturn(Optional.of(workspace));
        when(userRepository.findById("admin-1")).thenReturn(Optional.of(currentUser));

        invitationService.acceptInvitation(rawToken, recipient);

        assertEquals(InvitationStatus.ACCEPTED, inv.getStatus());
        assertEquals("user-99", inv.getReceiverId());
        verify(memberRepository, times(1)).save(any(WorkspaceMember.class));
        verify(authorizationService, times(1)).evictRole("ws-1", "user-99");
    }

    @Test
    @DisplayName("Noto'g'ri email bilan taklifni qabul qilish - ForbiddenException")
    void acceptInvitation_wrongEmail_throwsForbidden() {
        String rawToken = InvitationTokenUtil.generateToken();
        String tokenHash = InvitationTokenUtil.hashToken(rawToken);

        WorkspaceInvitation inv = WorkspaceInvitation.builder()
                .id("inv-email-1")
                .workspaceId("ws-1")
                .receiverEmail("owner@test.com")
                .type(InvitationType.EMAIL)
                .role(WorkspaceRole.MEMBER)
                .status(InvitationStatus.PENDING)
                .tokenHash(tokenHash)
                .expiresAt(LocalDateTime.now().plusDays(2))
                .build();

        User intruder = User.builder().id("intruder").name("intruder").email("wrong@test.com").build();

        when(invitationRepository.findByTokenHash(tokenHash)).thenReturn(Optional.of(inv));

        assertThrows(ForbiddenException.class, () -> invitationService.acceptInvitation(rawToken, intruder));
    }

    @Test
    @DisplayName("Telegram dummy email (user_xxx@taskcenter.local) bilan email-taklifni qabul qilib bo'lmaydi")
    void acceptInvitation_dummyEmail_throwsBadRequest() {
        String rawToken = InvitationTokenUtil.generateToken();
        String tokenHash = InvitationTokenUtil.hashToken(rawToken);

        WorkspaceInvitation inv = WorkspaceInvitation.builder()
                .id("inv-email-1")
                .workspaceId("ws-1")
                .receiverEmail("owner@test.com")
                .type(InvitationType.EMAIL)
                .role(WorkspaceRole.MEMBER)
                .status(InvitationStatus.PENDING)
                .tokenHash(tokenHash)
                .expiresAt(LocalDateTime.now().plusDays(2))
                .build();

        User dummyTgUser = User.builder()
                .id("tg-user-1")
                .name("tg_user")
                .email("user_12345@taskcenter.local")
                .build();

        when(invitationRepository.findByTokenHash(tokenHash)).thenReturn(Optional.of(inv));

        BadRequestException ex = assertThrows(BadRequestException.class, () -> invitationService.acceptInvitation(rawToken, dummyTgUser));
        assertFalse(ex.getMessage().contains("emailingizni tasdiqlang"), "Mavjud bo'lmagan email tasdiqlash oqimi ko'rsatilmasligi kerak");
    }

    @Test
    @DisplayName("Muddati o'tgan taklif tokeni - BadRequestException va EXPIRED holatiga o'tadi")
    void acceptInvitation_expiredToken_throwsBadRequest() {
        String rawToken = InvitationTokenUtil.generateToken();
        String tokenHash = InvitationTokenUtil.hashToken(rawToken);

        WorkspaceInvitation inv = WorkspaceInvitation.builder()
                .id("inv-exp")
                .workspaceId("ws-1")
                .receiverEmail("user@test.com")
                .type(InvitationType.EMAIL)
                .role(WorkspaceRole.MEMBER)
                .status(InvitationStatus.PENDING)
                .tokenHash(tokenHash)
                .expiresAt(LocalDateTime.now().minusHours(1))
                .build();

        User user = User.builder().id("user-1").name("user").email("user@test.com").build();

        when(invitationRepository.findByTokenHash(tokenHash)).thenReturn(Optional.of(inv));

        assertThrows(BadRequestException.class, () -> invitationService.acceptInvitation(rawToken, user));
        assertEquals(InvitationStatus.EXPIRED, inv.getStatus());
    }

    @Test
    @DisplayName("Atomik use_count oshirish: limit to'lganda parallel qabul qilish xatolik berishi")
    void acceptInvitation_maxUsesExhausted_throwsBadRequest() {
        String rawToken = InvitationTokenUtil.generateToken();
        String tokenHash = InvitationTokenUtil.hashToken(rawToken);

        WorkspaceInvitation inv = WorkspaceInvitation.builder()
                .id("inv-link-max")
                .workspaceId("ws-1")
                .type(InvitationType.LINK)
                .role(WorkspaceRole.MEMBER)
                .status(InvitationStatus.PENDING)
                .tokenHash(tokenHash)
                .maxUses(3)
                .useCount(3)
                .expiresAt(LocalDateTime.now().plusDays(1))
                .build();

        User user = User.builder().id("user-1").name("user").email("user@test.com").build();

        when(invitationRepository.findByTokenHash(tokenHash)).thenReturn(Optional.of(inv));

        assertThrows(BadRequestException.class, () -> invitationService.acceptInvitation(rawToken, user));
    }

    @Test
    @DisplayName("Parallel so'rovlar: 10 ta oqimdan 2 ta bo'sh o'rinli havolani atomik band qilish (concurrency test)")
    void acceptInvitation_parallelConcurrentAcceptance_atomicLimitEnforced() throws Exception {
        String rawToken = InvitationTokenUtil.generateToken();
        String tokenHash = InvitationTokenUtil.hashToken(rawToken);

        WorkspaceInvitation inv = WorkspaceInvitation.builder()
                .id("inv-concurrent")
                .workspaceId("ws-1")
                .senderId("admin-1")
                .type(InvitationType.LINK)
                .role(WorkspaceRole.MEMBER)
                .status(InvitationStatus.PENDING)
                .tokenHash(tokenHash)
                .maxUses(2)
                .useCount(0)
                .expiresAt(LocalDateTime.now().plusDays(1))
                .build();

        when(invitationRepository.findByTokenHash(tokenHash)).thenReturn(Optional.of(inv));
        when(workspaceRepository.findById("ws-1")).thenReturn(Optional.of(workspace));
        when(userRepository.findById("admin-1")).thenReturn(Optional.of(currentUser));
        when(memberRepository.existsByWorkspaceIdAndUserId(eq("ws-1"), anyString())).thenReturn(false);

        // Atomik DB UPDATE simulyatsiyasi: faqat birinchi 2 ta so'rov 1 qaytaradi, qolganlari 0
        AtomicInteger remainingSeats = new AtomicInteger(2);
        when(invitationRepository.incrementUseCountAtomic(eq("inv-concurrent"), any(LocalDateTime.class)))
                .thenAnswer(invocation -> remainingSeats.decrementAndGet() >= 0 ? 1 : 0);

        int threads = 10;
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threads);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);

        for (int i = 0; i < threads; i++) {
            final int index = i;
            executor.submit(() -> {
                try {
                    startLatch.await();
                    User parallelUser = User.builder().id("u-" + index).name("user-" + index).email("user" + index + "@test.com").build();
                    invitationService.acceptInvitation(rawToken, parallelUser);
                    successCount.incrementAndGet();
                } catch (BadRequestException e) {
                    failureCount.incrementAndGet();
                } catch (Exception e) {
                    // unexpected
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        assertTrue(doneLatch.await(5, TimeUnit.SECONDS));
        executor.shutdown();

        assertEquals(2, successCount.get(), "Faqat 2 ta parallel so'rov atomik ravishda muvaffaqiyatli qabul qilinishi kerak");
        assertEquals(8, failureCount.get(), "Qolgan 8 ta so'rov limit to'lganligi sababli BadRequestException olishi kerak");
    }

    @Test
    @DisplayName("LINK taklifda reject umumiy havolani bekor qilmasligi (havola ochiq qoladi)")
    void rejectInvitation_linkInvite_doesNotCancelSharedLink() {
        String rawToken = InvitationTokenUtil.generateToken();
        String tokenHash = InvitationTokenUtil.hashToken(rawToken);

        WorkspaceInvitation linkInv = WorkspaceInvitation.builder()
                .id("inv-link-reject")
                .workspaceId("ws-1")
                .type(InvitationType.LINK)
                .status(InvitationStatus.PENDING)
                .tokenHash(tokenHash)
                .build();

        User user = User.builder().id("user-5").name("user5").email("user5@test.com").build();

        when(invitationRepository.findByTokenHash(tokenHash)).thenReturn(Optional.of(linkInv));

        invitationService.rejectInvitation(rawToken, user);

        assertEquals(InvitationStatus.PENDING, linkInv.getStatus(), "LINK taklif holati PENDING bo'lib qolishi shart");
        verify(invitationRepository, never()).save(any());
        verify(notificationService, never()).notifyUser(anyString(), anyString(), anyString());
    }

    @Test
    @DisplayName("EMAIL taklifda reject statusni REJECTED qilishi va xabar jo'natishi")
    void rejectInvitation_emailInvite_setsStatusRejected() {
        String rawToken = InvitationTokenUtil.generateToken();
        String tokenHash = InvitationTokenUtil.hashToken(rawToken);

        WorkspaceInvitation emailInv = WorkspaceInvitation.builder()
                .id("inv-email-reject")
                .workspaceId("ws-1")
                .senderId("admin-1")
                .receiverEmail("target@test.com")
                .type(InvitationType.EMAIL)
                .status(InvitationStatus.PENDING)
                .tokenHash(tokenHash)
                .build();

        User targetUser = User.builder().id("target-1").name("target").email("target@test.com").build();

        when(invitationRepository.findByTokenHash(tokenHash)).thenReturn(Optional.of(emailInv));
        when(workspaceRepository.findById("ws-1")).thenReturn(Optional.of(workspace));
        when(userRepository.findById("admin-1")).thenReturn(Optional.of(currentUser));

        invitationService.rejectInvitation(rawToken, targetUser);

        assertEquals(InvitationStatus.REJECTED, emailInv.getStatus());
        verify(invitationRepository, times(1)).save(emailInv);
        verify(notificationService, times(1)).notifyUser(eq("admin-1"), eq("Taklif rad etildi"), anyString());
    }

    @Test
    @DisplayName("Public invitation preview (minimal xavfsiz ma'lumot)")
    void getPublicInvitationPreview_success() {
        String rawToken = InvitationTokenUtil.generateToken();
        String tokenHash = InvitationTokenUtil.hashToken(rawToken);

        WorkspaceInvitation inv = WorkspaceInvitation.builder()
                .id("inv-pub")
                .workspaceId("ws-1")
                .senderId("admin-1")
                .type(InvitationType.LINK)
                .role(WorkspaceRole.MEMBER)
                .status(InvitationStatus.PENDING)
                .tokenHash(tokenHash)
                .expiresAt(LocalDateTime.now().plusDays(5))
                .build();

        when(invitationRepository.findByTokenHash(tokenHash)).thenReturn(Optional.of(inv));
        when(workspaceRepository.findById("ws-1")).thenReturn(Optional.of(workspace));
        when(userRepository.findById("admin-1")).thenReturn(Optional.of(currentUser));

        PublicInvitationDto preview = invitationService.getPublicInvitation(rawToken);

        assertNotNull(preview);
        assertEquals("Super Loyiha", preview.getWorkspaceTitle());
        assertEquals("admin", preview.getInviterName());
        assertEquals(WorkspaceRole.MEMBER, preview.getRole());
        assertEquals(InvitationType.LINK, preview.getType());
        assertFalse(preview.isExpired());
    }

    @Test
    @DisplayName("Pending Telegram chat taklifini DB da saqlash va auto join oqimi")
    void pendingChatInvite_dbPersistenceAndAutoJoin() {
        Long chatId = 998877L;
        String rawToken = InvitationTokenUtil.generateToken();
        String tokenHash = InvitationTokenUtil.hashToken(rawToken);

        WorkspaceInvitation linkInv = WorkspaceInvitation.builder()
                .id("inv-pending-flow")
                .workspaceId("ws-1")
                .senderId("admin-1")
                .type(InvitationType.LINK)
                .role(WorkspaceRole.MEMBER)
                .status(InvitationStatus.PENDING)
                .tokenHash(tokenHash)
                .useCount(0)
                .maxUses(10)
                .expiresAt(LocalDateTime.now().plusDays(7))
                .build();

        invitationService.storePendingChatInvite(chatId, rawToken);
        verify(pendingChatInviteRepository, times(1)).save(any(PendingTelegramChatInvite.class));

        PendingTelegramChatInvite dbInvite = PendingTelegramChatInvite.builder()
                .chatId(chatId)
                .token(rawToken)
                .expiresAt(LocalDateTime.now().plusHours(24))
                .build();

        when(pendingChatInviteRepository.findActiveByChatId(eq(chatId), any(LocalDateTime.class)))
                .thenReturn(Optional.of(dbInvite));

        assertEquals(rawToken, invitationService.getPendingChatInvite(chatId));

        User newlyLinkedUser = User.builder()
                .id("new-user-1")
                .name("New Telegram User")
                .email("user_998877@taskcenter.local")
                .telegramChatId(chatId)
                .build();

        when(invitationRepository.findByTokenHash(tokenHash)).thenReturn(Optional.of(linkInv));
        when(invitationRepository.incrementUseCountAtomic(eq("inv-pending-flow"), any(LocalDateTime.class))).thenReturn(1);
        when(memberRepository.existsByWorkspaceIdAndUserId("ws-1", "new-user-1")).thenReturn(false);
        when(workspaceRepository.findById("ws-1")).thenReturn(Optional.of(workspace));
        when(userRepository.findById("admin-1")).thenReturn(Optional.of(currentUser));

        String pendingToken = invitationService.getPendingChatInvite(chatId);
        invitationService.acceptInvitation(pendingToken, newlyLinkedUser);
        invitationService.removePendingChatInvite(chatId);

        verify(pendingChatInviteRepository, times(1)).deleteById(chatId);
        verify(memberRepository, times(1)).save(any(WorkspaceMember.class));
    }

    @Test
    @DisplayName("Test email yuborish - faqat adminning o'z emailiga yuboriladi va limit tekshiriladi")
    void testEmail_success() {
        when(rateLimitingService.tryConsumeUserLimit("test-email", "admin-1", 5, 60)).thenReturn(0L);
        when(emailService.sendTestEmail("admin@test.com"))
                .thenReturn(new TestEmailResponse(true, "SENT", "OK"));

        TestEmailResponse response = invitationService.testEmail("ws-1", currentUser);

        assertNotNull(response);
        assertEquals("SENT", response.getStatus());
        assertTrue(response.isSuccess());
        verify(emailService, times(1)).sendTestEmail(eq("admin@test.com"));
    }

    @Test
    @DisplayName("Test email: soatiga 5 ta limit oshib ketganda RateLimitException (429)")
    void testEmail_rateLimitExceeded_throwsRateLimitException() {
        when(rateLimitingService.tryConsumeUserLimit("test-email", "admin-1", 5, 60)).thenReturn(1800L);

        RateLimitException ex = assertThrows(RateLimitException.class, () -> invitationService.testEmail("ws-1", currentUser));
        assertEquals(1800L, ex.getRetryAfterSeconds());
        verify(emailService, never()).sendTestEmail(anyString());
    }

    @Test
    @DisplayName("Test email: dummy email ega foydalanuvchi sinov emailini yubora olmaydi")
    void testEmail_dummyEmail_throwsBadRequest() {
        User dummyUser = User.builder().id("admin-dummy").name("admin").email("user_123@taskcenter.local").build();

        assertThrows(BadRequestException.class, () -> invitationService.testEmail("ws-1", dummyUser));
        verify(emailService, never()).sendTestEmail(anyString());
    }

    @Test
    @DisplayName("GET /api/invitations/me: Faqat uning aniq userId'siga kelgan takliflar chiqadi")
    void getMyPendingInvitations_onlyQueriesReceiverId() {
        User someUser = User.builder().id("u-123").name("u123").email("real@test.com").build();

        WorkspaceInvitation inv = WorkspaceInvitation.builder()
                .id("inv-direct")
                .workspaceId("ws-1")
                .senderId("admin-1")
                .receiverId("u-123")
                .role(WorkspaceRole.MEMBER)
                .status(InvitationStatus.PENDING)
                .build();

        when(invitationRepository.findByReceiverIdAndStatusOrderByCreatedAtDesc("u-123", InvitationStatus.PENDING))
                .thenReturn(List.of(inv));
        when(workspaceRepository.findById("ws-1")).thenReturn(Optional.of(workspace));
        when(userRepository.findById("admin-1")).thenReturn(Optional.of(currentUser));

        List<WorkspaceInvitationDto> result = invitationService.getMyPendingInvitations(someUser);

        assertEquals(1, result.size());
        assertEquals("inv-direct", result.get(0).getId());
        verify(invitationRepository, never()).findByReceiverEmailAndStatus(anyString(), any());
    }

    @Test
    @DisplayName("acceptInvitationById: receiverId mos kelganda qabul qilinadi")
    void acceptInvitationById_success() {
        User receiver = User.builder().id("user-target").name("target").email("target@test.com").build();

        WorkspaceInvitation inv = WorkspaceInvitation.builder()
                .id("inv-by-id")
                .workspaceId("ws-1")
                .senderId("admin-1")
                .receiverId("user-target")
                .type(InvitationType.EMAIL)
                .role(WorkspaceRole.MEMBER)
                .status(InvitationStatus.PENDING)
                .expiresAt(LocalDateTime.now().plusDays(2))
                .build();

        when(invitationRepository.findById("inv-by-id")).thenReturn(Optional.of(inv));
        when(memberRepository.existsByWorkspaceIdAndUserId("ws-1", "user-target")).thenReturn(false);
        when(invitationRepository.acceptEmailInvitationAtomic(eq("inv-by-id"), eq("user-target"), any(LocalDateTime.class))).thenReturn(1);
        when(workspaceRepository.findById("ws-1")).thenReturn(Optional.of(workspace));
        when(userRepository.findById("admin-1")).thenReturn(Optional.of(currentUser));

        invitationService.acceptInvitationById("inv-by-id", receiver);

        verify(memberRepository, times(1)).save(any(WorkspaceMember.class));
        verify(authorizationService, times(1)).evictRole("ws-1", "user-target");
    }

    @Test
    @DisplayName("acceptInvitationById: boshqa foydalanuvchi chaqirganda ForbiddenException")
    void acceptInvitationById_wrongReceiver_throwsForbidden() {
        User wrongUser = User.builder().id("user-wrong").name("wrong").email("wrong@test.com").build();

        WorkspaceInvitation inv = WorkspaceInvitation.builder()
                .id("inv-by-id")
                .workspaceId("ws-1")
                .senderId("admin-1")
                .receiverId("user-target")
                .type(InvitationType.EMAIL)
                .role(WorkspaceRole.MEMBER)
                .status(InvitationStatus.PENDING)
                .expiresAt(LocalDateTime.now().plusDays(2))
                .build();

        when(invitationRepository.findById("inv-by-id")).thenReturn(Optional.of(inv));

        assertThrows(ForbiddenException.class, () -> invitationService.acceptInvitationById("inv-by-id", wrongUser));
        verify(memberRepository, never()).save(any());
    }

    @Test
    @DisplayName("rejectInvitationById: receiverId mos kelganda rad etiladi va status REJECTED bo'ladi")
    void rejectInvitationById_success() {
        User receiver = User.builder().id("user-target").name("target").email("target@test.com").build();

        WorkspaceInvitation inv = WorkspaceInvitation.builder()
                .id("inv-by-id-reject")
                .workspaceId("ws-1")
                .senderId("admin-1")
                .receiverId("user-target")
                .type(InvitationType.EMAIL)
                .role(WorkspaceRole.MEMBER)
                .status(InvitationStatus.PENDING)
                .expiresAt(LocalDateTime.now().plusDays(2))
                .build();

        when(invitationRepository.findById("inv-by-id-reject")).thenReturn(Optional.of(inv));
        when(workspaceRepository.findById("ws-1")).thenReturn(Optional.of(workspace));
        when(userRepository.findById("admin-1")).thenReturn(Optional.of(currentUser));

        invitationService.rejectInvitationById("inv-by-id-reject", receiver);

        assertEquals(InvitationStatus.REJECTED, inv.getStatus());
        verify(invitationRepository, times(1)).save(inv);
    }

    @Test
    @DisplayName("2. acceptInvitationById: LINK turidagi taklif bo'lsa 400 (BadRequestException) qaytaradi")
    void acceptInvitationById_linkType_throwsBadRequest() {
        User receiver = User.builder().id("user-target").name("target").build();

        WorkspaceInvitation linkInv = WorkspaceInvitation.builder()
                .id("inv-link-123")
                .workspaceId("ws-1")
                .senderId("admin-1")
                .receiverId("user-target")
                .type(InvitationType.LINK)
                .role(WorkspaceRole.MEMBER)
                .status(InvitationStatus.PENDING)
                .expiresAt(LocalDateTime.now().plusDays(2))
                .build();

        when(invitationRepository.findById("inv-link-123")).thenReturn(Optional.of(linkInv));

        assertThrows(BadRequestException.class, () -> invitationService.acceptInvitationById("inv-link-123", receiver));
        verify(memberRepository, never()).save(any());
    }

    @Test
    @DisplayName("2. rejectInvitationById: LINK turidagi taklif bo'lsa 400 (BadRequestException) qaytaradi")
    void rejectInvitationById_linkType_throwsBadRequest() {
        User receiver = User.builder().id("user-target").name("target").build();

        WorkspaceInvitation linkInv = WorkspaceInvitation.builder()
                .id("inv-link-123")
                .workspaceId("ws-1")
                .senderId("admin-1")
                .receiverId("user-target")
                .type(InvitationType.LINK)
                .role(WorkspaceRole.MEMBER)
                .status(InvitationStatus.PENDING)
                .expiresAt(LocalDateTime.now().plusDays(2))
                .build();

        when(invitationRepository.findById("inv-link-123")).thenReturn(Optional.of(linkInv));

        assertThrows(BadRequestException.class, () -> invitationService.rejectInvitationById("inv-link-123", receiver));
        verify(invitationRepository, never()).save(any());
    }

    @Test
    @DisplayName("3. validateReceiverOwnership: receiverId == null bo'lsa email mos kelsa ham ForbiddenException tashlanadi")
    void acceptInvitationById_nullReceiverId_throwsForbiddenEvenIfEmailMatches() {
        User user = User.builder().id("user-target").name("target").email("target@test.com").build();

        WorkspaceInvitation inv = WorkspaceInvitation.builder()
                .id("inv-email-null-receiver")
                .workspaceId("ws-1")
                .senderId("admin-1")
                .receiverId(null) // receiverId yo'q
                .receiverEmail("target@test.com") // lekin email bir xil
                .type(InvitationType.EMAIL)
                .role(WorkspaceRole.MEMBER)
                .status(InvitationStatus.PENDING)
                .expiresAt(LocalDateTime.now().plusDays(2))
                .build();

        when(invitationRepository.findById("inv-email-null-receiver")).thenReturn(Optional.of(inv));

        assertThrows(ForbiddenException.class, () -> invitationService.acceptInvitationById("inv-email-null-receiver", user));
        verify(memberRepository, never()).save(any());
    }
}
