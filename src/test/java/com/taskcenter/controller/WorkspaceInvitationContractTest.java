package com.taskcenter.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.taskcenter.model.*;
import com.taskcenter.repository.WorkspaceInvitationRepository;
import com.taskcenter.repository.WorkspaceMemberRepository;
import com.taskcenter.repository.WorkspaceRepository;
import com.taskcenter.repository.UserRepository;
import com.taskcenter.security.JwtTokenProvider;
import com.taskcenter.security.RateLimitingService;
import com.taskcenter.util.InvitationTokenUtil;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestPropertySource(properties = {"ratelimit.auth.enabled=true", "ratelimit.api.enabled=true"})
@Transactional
class WorkspaceInvitationContractTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private WorkspaceRepository workspaceRepository;

    @Autowired
    private WorkspaceMemberRepository memberRepository;

    @Autowired
    private WorkspaceInvitationRepository invitationRepository;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @MockBean
    private RateLimitingService rateLimitingService;

    @Value("${jwt.secret}")
    private String jwtSecret;

    private User admin;
    private User recipient;
    private User otherUser;
    private User telegramDummyUser;
    private Workspace workspace;

    private String adminToken;
    private String recipientToken;
    private String otherToken;
    private String dummyToken;

    private void saveExample(String filename, MvcResult result) {
        try {
            Path dir = Paths.get("target", "contract-examples");
            Files.createDirectories(dir);
            String json = result.getResponse().getContentAsString(StandardCharsets.UTF_8);
            ObjectMapper om = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);
            Object obj = om.readValue(json, Object.class);
            Files.writeString(dir.resolve(filename), om.writeValueAsString(obj), StandardCharsets.UTF_8);
        } catch (Exception e) {
            // non-fatal
        }
    }

    @BeforeEach
    void setUp() {
        when(rateLimitingService.tryConsumeIpLimit(anyString(), anyString(), anyInt(), anyInt())).thenReturn(0L);
        when(rateLimitingService.tryConsumeUserLimit(anyString(), anyString(), anyInt(), anyInt())).thenReturn(0L);

        admin = userRepository.save(User.builder()
                .name("contract_admin_" + UUID.randomUUID().toString().substring(0, 6))
                .fullName("Contract Admin")
                .email("contract_admin@example.com")
                .password("pass")
                .role(User.Role.ADMIN)
                .build());

        recipient = userRepository.save(User.builder()
                .name("contract_rec_" + UUID.randomUUID().toString().substring(0, 6))
                .fullName("Contract Recipient")
                .email("recipient@example.com")
                .password("pass")
                .role(User.Role.USER)
                .build());

        otherUser = userRepository.save(User.builder()
                .name("contract_oth_" + UUID.randomUUID().toString().substring(0, 6))
                .fullName("Other User")
                .email("other@example.com")
                .password("pass")
                .role(User.Role.USER)
                .build());

        telegramDummyUser = userRepository.save(User.builder()
                .name("contract_dummy_" + UUID.randomUUID().toString().substring(0, 6))
                .fullName("Telegram User")
                .email("user_12345@taskcenter.local")
                .password("pass")
                .role(User.Role.USER)
                .build());

        adminToken = jwtTokenProvider.generateTokenFromUser(admin);
        recipientToken = jwtTokenProvider.generateTokenFromUser(recipient);
        otherToken = jwtTokenProvider.generateTokenFromUser(otherUser);
        dummyToken = jwtTokenProvider.generateTokenFromUser(telegramDummyUser);

        workspace = workspaceRepository.save(Workspace.builder()
                .title("Contract Workspace")
                .ownerId(admin.getId())
                .build());

        memberRepository.save(WorkspaceMember.builder()
                .workspaceId(workspace.getId())
                .userId(admin.getId())
                .role(WorkspaceRole.OWNER)
                .build());
    }

    @Test
    @DisplayName("1. INVITE_NOT_FOUND: Mavjud bo'lmagan token uchun 404 va INVITE_NOT_FOUND kodi qaytadi")
    void testInviteNotFound() throws Exception {
        MvcResult result = mvc.perform(get("/api/invitations/non-existent-token-1234567890"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value("INVITE_NOT_FOUND"))
                .andExpect(jsonPath("$.code").value("NOT_FOUND"))
                .andExpect(jsonPath("$.message").isNotEmpty())
                .andExpect(jsonPath("$.status").value(404))
                .andReturn();

        saveExample("invite_not_found.json", result);
    }

    @Test
    @DisplayName("2. INVITE_EXPIRED: Muddati o'tgan taklif qabul qilinganda 400 va INVITE_EXPIRED qaytadi")
    void testInviteExpired() throws Exception {
        String rawToken = InvitationTokenUtil.generateToken();
        String tokenHash = InvitationTokenUtil.hashToken(rawToken);

        invitationRepository.save(WorkspaceInvitation.builder()
                .workspaceId(workspace.getId())
                .senderId(admin.getId())
                .receiverEmail("recipient@example.com")
                .type(InvitationType.EMAIL)
                .tokenHash(tokenHash)
                .role(WorkspaceRole.MEMBER)
                .status(InvitationStatus.PENDING)
                .expiresAt(LocalDateTime.now().minusDays(1))
                .build());

        MvcResult result = mvc.perform(post("/api/invitations/" + rawToken + "/accept")
                        .header("Authorization", "Bearer " + recipientToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value("INVITE_EXPIRED"))
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"))
                .andExpect(jsonPath("$.message").isNotEmpty())
                .andReturn();

        saveExample("invite_expired.json", result);
    }

    @Test
    @DisplayName("3. INVITE_CANCELLED: Bekor qilingan taklif qabul qilinganda 400 va INVITE_CANCELLED qaytadi")
    void testInviteCancelled() throws Exception {
        String rawToken = InvitationTokenUtil.generateToken();
        String tokenHash = InvitationTokenUtil.hashToken(rawToken);

        invitationRepository.save(WorkspaceInvitation.builder()
                .workspaceId(workspace.getId())
                .senderId(admin.getId())
                .receiverEmail("recipient@example.com")
                .type(InvitationType.EMAIL)
                .tokenHash(tokenHash)
                .role(WorkspaceRole.MEMBER)
                .status(InvitationStatus.CANCELLED)
                .expiresAt(LocalDateTime.now().plusDays(5))
                .build());

        MvcResult result = mvc.perform(post("/api/invitations/" + rawToken + "/accept")
                        .header("Authorization", "Bearer " + recipientToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value("INVITE_CANCELLED"))
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"))
                .andReturn();

        saveExample("invite_cancelled.json", result);
    }

    @Test
    @DisplayName("3b. INVITE_REJECTED: Rad etilgan taklif qabul qilinganda 400 va INVITE_REJECTED qaytadi")
    void testInviteRejected() throws Exception {
        String rawToken = InvitationTokenUtil.generateToken();
        String tokenHash = InvitationTokenUtil.hashToken(rawToken);

        invitationRepository.save(WorkspaceInvitation.builder()
                .workspaceId(workspace.getId())
                .senderId(admin.getId())
                .receiverEmail("recipient@example.com")
                .type(InvitationType.EMAIL)
                .tokenHash(tokenHash)
                .role(WorkspaceRole.MEMBER)
                .status(InvitationStatus.REJECTED)
                .expiresAt(LocalDateTime.now().plusDays(5))
                .build());

        MvcResult result = mvc.perform(post("/api/invitations/" + rawToken + "/accept")
                        .header("Authorization", "Bearer " + recipientToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value("INVITE_REJECTED"))
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"))
                .andReturn();

        saveExample("invite_rejected.json", result);
    }

    @Test
    @DisplayName("4. INVITE_ALREADY_USED: Qabul qilingan email taklif qayta qabul qilinganda 400 va INVITE_ALREADY_USED qaytadi")
    void testInviteAlreadyUsed() throws Exception {
        String rawToken = InvitationTokenUtil.generateToken();
        String tokenHash = InvitationTokenUtil.hashToken(rawToken);

        invitationRepository.save(WorkspaceInvitation.builder()
                .workspaceId(workspace.getId())
                .senderId(admin.getId())
                .receiverEmail("recipient@example.com")
                .type(InvitationType.EMAIL)
                .tokenHash(tokenHash)
                .role(WorkspaceRole.MEMBER)
                .status(InvitationStatus.ACCEPTED)
                .expiresAt(LocalDateTime.now().plusDays(5))
                .build());

        MvcResult result = mvc.perform(post("/api/invitations/" + rawToken + "/accept")
                        .header("Authorization", "Bearer " + recipientToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value("INVITE_ALREADY_USED"))
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"))
                .andReturn();

        saveExample("invite_already_used.json", result);
    }

    @Test
    @DisplayName("5. INVITE_LIMIT_REACHED: Havoladan foydalanish limiti to'lganda 400 va INVITE_LIMIT_REACHED qaytadi")
    void testInviteLimitReached() throws Exception {
        String rawToken = InvitationTokenUtil.generateToken();
        String tokenHash = InvitationTokenUtil.hashToken(rawToken);

        invitationRepository.save(WorkspaceInvitation.builder()
                .workspaceId(workspace.getId())
                .senderId(admin.getId())
                .type(InvitationType.LINK)
                .tokenHash(tokenHash)
                .role(WorkspaceRole.MEMBER)
                .status(InvitationStatus.PENDING)
                .maxUses(2)
                .useCount(2)
                .expiresAt(LocalDateTime.now().plusDays(5))
                .build());

        MvcResult result = mvc.perform(post("/api/invitations/" + rawToken + "/accept")
                        .header("Authorization", "Bearer " + recipientToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value("INVITE_LIMIT_REACHED"))
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"))
                .andReturn();

        saveExample("invite_limit_reached.json", result);
    }

    @Test
    @DisplayName("5b. INVITE_LIMIT_REACHED (status ACCEPTED): LINK taklif statusi ACCEPTED bo'lganda ham INVITE_LIMIT_REACHED qaytadi")
    void testLinkInviteAcceptedStatusReturnsLimitReached() throws Exception {
        String rawToken = InvitationTokenUtil.generateToken();
        String tokenHash = InvitationTokenUtil.hashToken(rawToken);

        invitationRepository.save(WorkspaceInvitation.builder()
                .workspaceId(workspace.getId())
                .senderId(admin.getId())
                .type(InvitationType.LINK)
                .tokenHash(tokenHash)
                .role(WorkspaceRole.MEMBER)
                .status(InvitationStatus.ACCEPTED)
                .maxUses(5)
                .useCount(5)
                .expiresAt(LocalDateTime.now().plusDays(5))
                .build());

        MvcResult result = mvc.perform(post("/api/invitations/" + rawToken + "/accept")
                        .header("Authorization", "Bearer " + recipientToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value("INVITE_LIMIT_REACHED"))
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"))
                .andReturn();

        saveExample("invite_limit_reached_accepted.json", result);
    }

    @Test
    @DisplayName("6. INVITE_EMAIL_MISMATCH: Boshqa email egasi taklifni qabul qilganda 403, INVITE_EMAIL_MISMATCH va maskedReceiverEmail qaytadi")
    void testInviteEmailMismatch() throws Exception {
        String rawToken = InvitationTokenUtil.generateToken();
        String tokenHash = InvitationTokenUtil.hashToken(rawToken);

        invitationRepository.save(WorkspaceInvitation.builder()
                .workspaceId(workspace.getId())
                .senderId(admin.getId())
                .receiverEmail("recipient@example.com")
                .type(InvitationType.EMAIL)
                .tokenHash(tokenHash)
                .role(WorkspaceRole.MEMBER)
                .status(InvitationStatus.PENDING)
                .expiresAt(LocalDateTime.now().plusDays(5))
                .build());

        MvcResult result = mvc.perform(post("/api/invitations/" + rawToken + "/accept")
                        .header("Authorization", "Bearer " + otherToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value("INVITE_EMAIL_MISMATCH"))
                .andExpect(jsonPath("$.code").value("FORBIDDEN"))
                .andExpect(jsonPath("$.maskedReceiverEmail").value("r***@example.com"))
                .andExpect(jsonPath("$.message").value(containsString("r***@example.com")))
                .andExpect(jsonPath("$.message").value(not(containsString("recipient@example.com"))))
                .andReturn();

        saveExample("invite_email_mismatch.json", result);
    }

    @Test
    @DisplayName("7. INVITE_DUMMY_EMAIL: Telegram orqali ochilgan dummy user email taklifni qabul qilganda 400 va INVITE_DUMMY_EMAIL qaytadi")
    void testInviteDummyEmail() throws Exception {
        String rawToken = InvitationTokenUtil.generateToken();
        String tokenHash = InvitationTokenUtil.hashToken(rawToken);

        invitationRepository.save(WorkspaceInvitation.builder()
                .workspaceId(workspace.getId())
                .senderId(admin.getId())
                .receiverEmail("user_12345@taskcenter.local")
                .type(InvitationType.EMAIL)
                .tokenHash(tokenHash)
                .role(WorkspaceRole.MEMBER)
                .status(InvitationStatus.PENDING)
                .expiresAt(LocalDateTime.now().plusDays(5))
                .build());

        MvcResult result = mvc.perform(post("/api/invitations/" + rawToken + "/accept")
                        .header("Authorization", "Bearer " + dummyToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value("INVITE_DUMMY_EMAIL"))
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"))
                .andExpect(jsonPath("$.message").value(not(containsString("biriktiring"))))
                .andExpect(jsonPath("$.message").value(containsString("LINK taklif havolasidan foydalaning")))
                .andReturn();

        saveExample("invite_dummy_email.json", result);
    }

    @Test
    @DisplayName("8. ALREADY_MEMBER: Foydalanuvchi allaqachon workspace a'zosi bo'lsa 409 va ALREADY_MEMBER qaytadi")
    void testAlreadyMember() throws Exception {
        memberRepository.save(WorkspaceMember.builder()
                .workspaceId(workspace.getId())
                .userId(recipient.getId())
                .role(WorkspaceRole.MEMBER)
                .build());

        String rawToken = InvitationTokenUtil.generateToken();
        String tokenHash = InvitationTokenUtil.hashToken(rawToken);

        invitationRepository.save(WorkspaceInvitation.builder()
                .workspaceId(workspace.getId())
                .senderId(admin.getId())
                .receiverEmail("recipient@example.com")
                .type(InvitationType.EMAIL)
                .tokenHash(tokenHash)
                .role(WorkspaceRole.MEMBER)
                .status(InvitationStatus.PENDING)
                .expiresAt(LocalDateTime.now().plusDays(5))
                .build());

        MvcResult result = mvc.perform(post("/api/invitations/" + rawToken + "/accept")
                        .header("Authorization", "Bearer " + recipientToken))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value("ALREADY_MEMBER"))
                .andExpect(jsonPath("$.code").value("CONFLICT"))
                .andReturn();

        saveExample("already_member.json", result);
    }

    @Test
    @DisplayName("9. DUPLICATE_PENDING_INVITE: Kutilayotgan taklifi bor foydalanuvchiga qayta taklif yuborilganda 409 va DUPLICATE_PENDING_INVITE qaytadi")
    void testDuplicatePendingInvite() throws Exception {
        invitationRepository.save(WorkspaceInvitation.builder()
                .workspaceId(workspace.getId())
                .senderId(admin.getId())
                .receiverId(recipient.getId())
                .receiverEmail(recipient.getEmail())
                .type(InvitationType.EMAIL)
                .tokenHash("hash123")
                .role(WorkspaceRole.MEMBER)
                .status(InvitationStatus.PENDING)
                .expiresAt(LocalDateTime.now().plusDays(5))
                .build());

        MvcResult result = mvc.perform(post("/api/workspaces/" + workspace.getId() + "/invites")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"usernameOrEmail\":\"" + recipient.getEmail() + "\",\"role\":\"MEMBER\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value("DUPLICATE_PENDING_INVITE"))
                .andExpect(jsonPath("$.code").value("CONFLICT"))
                .andReturn();

        saveExample("duplicate_pending_invite.json", result);
    }

    @Test
    @DisplayName("10. CANNOT_INVITE_OWNER: Taklifda OWNER roli so'ralganda 400 va CANNOT_INVITE_OWNER qaytadi")
    void testCannotInviteOwner() throws Exception {
        MvcResult result = mvc.perform(post("/api/workspaces/" + workspace.getId() + "/invites")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"usernameOrEmail\":\"recipient@example.com\",\"role\":\"OWNER\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value("CANNOT_INVITE_OWNER"))
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"))
                .andReturn();

        saveExample("cannot_invite_owner.json", result);
    }

    @Test
    @DisplayName("11. RATE_LIMITED: So'rovlar soni oshganda 429, Retry-After sarlavhasi va retryAfterSeconds qaytadi")
    void testRateLimited() throws Exception {
        when(rateLimitingService.tryConsumeIpLimit(eq("invite-preview"), anyString(), eq(30), eq(1)))
                .thenReturn(42L);

        MvcResult result = mvc.perform(get("/api/invitations/any-token-check"))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().string("Retry-After", "42"))
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value("RATE_LIMITED"))
                .andExpect(jsonPath("$.code").value("TOO_MANY_REQUESTS"))
                .andExpect(jsonPath("$.retryAfterSeconds").value(42))
                .andReturn();

        saveExample("rate_limited.json", result);
    }

    @Test
    @DisplayName("12. TEST_EMAIL_LIMIT: Sinov email limiti oshganda 429 va TEST_EMAIL_LIMIT qaytadi")
    void testTestEmailLimit() throws Exception {
        when(rateLimitingService.tryConsumeUserLimit(eq("test-email"), eq(admin.getId()), eq(5), eq(60)))
                .thenReturn(55L);

        MvcResult result = mvc.perform(post("/api/workspaces/" + workspace.getId() + "/invites/test-email")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().string("Retry-After", "55"))
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value("TEST_EMAIL_LIMIT"))
                .andExpect(jsonPath("$.code").value("TOO_MANY_REQUESTS"))
                .andExpect(jsonPath("$.retryAfterSeconds").value(55))
                .andReturn();

        saveExample("test_email_limit.json", result);
    }

    @Test
    @DisplayName("13. FORBIDDEN_ROLE: Workspace da ruxsati bo'lmagan foydalanuvchi taklif qilmoqchi bo'lsa 403 va FORBIDDEN_ROLE qaytadi")
    void testForbiddenRole() throws Exception {
        MvcResult result = mvc.perform(post("/api/workspaces/" + workspace.getId() + "/invites")
                        .header("Authorization", "Bearer " + otherToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"usernameOrEmail\":\"recipient@example.com\",\"role\":\"MEMBER\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value("FORBIDDEN_ROLE"))
                .andExpect(jsonPath("$.code").value("FORBIDDEN"))
                .andReturn();

        saveExample("forbidden_role.json", result);
    }

    @Test
    @DisplayName("14. VALIDATION_ERROR: Maydonlar to'ldirilmaganda 400, VALIDATION_ERROR va fieldErrors qaytadi")
    void testValidationError() throws Exception {
        MvcResult result = mvc.perform(post("/api/workspaces/" + workspace.getId() + "/invites")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.fieldErrors").isArray())
                .andExpect(jsonPath("$.fieldErrors", hasSize(greaterThanOrEqualTo(1))))
                .andExpect(jsonPath("$.fieldErrors[0].field").isNotEmpty())
                .andReturn();

        saveExample("validation_error.json", result);
    }

    @Test
    @DisplayName("15. INVALID_INVITE_TYPE: LINK taklifini ID bo'yicha qabul qilmoqchi bo'lsa 400 va INVALID_INVITE_TYPE qaytadi")
    void testInvalidInviteType() throws Exception {
        WorkspaceInvitation linkInv = invitationRepository.save(WorkspaceInvitation.builder()
                .workspaceId(workspace.getId())
                .senderId(admin.getId())
                .type(InvitationType.LINK)
                .tokenHash("linkhash")
                .role(WorkspaceRole.MEMBER)
                .status(InvitationStatus.PENDING)
                .expiresAt(LocalDateTime.now().plusDays(5))
                .build());

        MvcResult result = mvc.perform(post("/api/invitations/by-id/" + linkInv.getId() + "/accept")
                        .header("Authorization", "Bearer " + recipientToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value("INVALID_INVITE_TYPE"))
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"))
                .andReturn();

        saveExample("invalid_invite_type.json", result);
    }

    @Test
    @DisplayName("16. INVITE_NOT_FOR_USER: Boshqa foydalanuvchiga tegishli EMAIL taklifni by-id orqali qabul qilmoqchi bo'lsa 403 va INVITE_NOT_FOR_USER qaytadi")
    void testInviteNotForUser() throws Exception {
        WorkspaceInvitation emailInv = invitationRepository.save(WorkspaceInvitation.builder()
                .workspaceId(workspace.getId())
                .senderId(admin.getId())
                .receiverId(recipient.getId())
                .receiverEmail(recipient.getEmail())
                .type(InvitationType.EMAIL)
                .tokenHash("emailhash")
                .role(WorkspaceRole.MEMBER)
                .status(InvitationStatus.PENDING)
                .expiresAt(LocalDateTime.now().plusDays(5))
                .build());

        MvcResult result = mvc.perform(post("/api/invitations/by-id/" + emailInv.getId() + "/accept")
                        .header("Authorization", "Bearer " + otherToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value("INVITE_NOT_FOR_USER"))
                .andExpect(jsonPath("$.code").value("FORBIDDEN"))
                .andReturn();

        saveExample("invite_not_for_user.json", result);
    }

    @Test
    @DisplayName("17. Spring Security 401 UNAUTHORIZED: Autentifikatsiyasiz so'rovda 401 va UNAUTHORIZED qaytadi")
    void testSecurityUnauthorized() throws Exception {
        MvcResult result = mvc.perform(get("/api/workspaces"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value("UNAUTHORIZED"))
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"))
                .andExpect(jsonPath("$.status").value(401))
                .andReturn();

        saveExample("security_unauthorized.json", result);
    }

    @Test
    @DisplayName("18. Spring Security 401 TOKEN_EXPIRED: Muddati o'tgan JWT yuborilganda 401 va TOKEN_EXPIRED qaytadi")
    void testSecurityTokenExpired() throws Exception {
        String expiredJwt = Jwts.builder()
                .setSubject(recipient.getName())
                .claim("id", recipient.getId())
                .claim("role", recipient.getRole().name())
                .setIssuedAt(new Date(System.currentTimeMillis() - 200000))
                .setExpiration(new Date(System.currentTimeMillis() - 100000))
                .signWith(Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8)), SignatureAlgorithm.HS256)
                .compact();

        MvcResult result = mvc.perform(get("/api/workspaces")
                        .header("Authorization", "Bearer " + expiredJwt))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value("TOKEN_EXPIRED"))
                .andExpect(jsonPath("$.status").value(401))
                .andReturn();

        saveExample("security_token_expired.json", result);
    }

    @Test
    @DisplayName("19. Spring Security 403 FORBIDDEN: Huquqi yetarli bo'lmagan foydalanuvchiga 403 va FORBIDDEN qaytadi")
    void testSecurityAccessDenied() throws Exception {
        MvcResult result = mvc.perform(get("/actuator/metrics")
                        .header("Authorization", "Bearer " + recipientToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value("FORBIDDEN"))
                .andExpect(jsonPath("$.code").value("FORBIDDEN"))
                .andExpect(jsonPath("$.status").value(403))
                .andReturn();

        saveExample("security_forbidden.json", result);
    }

    @Test
    @DisplayName("20. Standard 404 NOT_FOUND: Mavjud bo'lmagan resursda standart NOT_FOUND qaytadi")
    void testStandardNotFound() throws Exception {
        MvcResult result = mvc.perform(get("/api/workspaces/" + UUID.randomUUID())
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value("NOT_FOUND"))
                .andExpect(jsonPath("$.code").value("NOT_FOUND"))
                .andExpect(jsonPath("$.status").value(404))
                .andReturn();

        saveExample("standard_not_found.json", result);
    }

    @Test
    @DisplayName("21. Success: Public preview muvaffaqiyatli ma'lumot qaytaradi")
    void testPublicPreviewSuccess() throws Exception {
        String rawToken = InvitationTokenUtil.generateToken();
        String tokenHash = InvitationTokenUtil.hashToken(rawToken);

        invitationRepository.save(WorkspaceInvitation.builder()
                .workspaceId(workspace.getId())
                .senderId(admin.getId())
                .type(InvitationType.LINK)
                .tokenHash(tokenHash)
                .role(WorkspaceRole.MEMBER)
                .status(InvitationStatus.PENDING)
                .expiresAt(LocalDateTime.now(java.time.ZoneOffset.UTC).plusDays(5))
                .build());

        MvcResult result = mvc.perform(get("/api/invitations/" + rawToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.timestamp").value(matchesRegex("^\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}(\\.\\d+)?Z$")))
                .andExpect(jsonPath("$.data.expiresAt").value(matchesRegex("^\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}(\\.\\d+)?Z$")))
                .andExpect(jsonPath("$.data.workspaceTitle").value("Contract Workspace"))
                .andReturn();

        saveExample("public_preview_success.json", result);
    }

    @Test
    @DisplayName("22. Success: Token orqali qabul qilish muvaffaqiyatli bajariladi")
    void testAcceptSuccess() throws Exception {
        String rawToken = InvitationTokenUtil.generateToken();
        String tokenHash = InvitationTokenUtil.hashToken(rawToken);

        invitationRepository.save(WorkspaceInvitation.builder()
                .workspaceId(workspace.getId())
                .senderId(admin.getId())
                .receiverEmail(recipient.getEmail())
                .type(InvitationType.EMAIL)
                .tokenHash(tokenHash)
                .role(WorkspaceRole.MEMBER)
                .status(InvitationStatus.PENDING)
                .expiresAt(LocalDateTime.now(java.time.ZoneOffset.UTC).plusDays(5))
                .build());

        MvcResult result = mvc.perform(post("/api/invitations/" + rawToken + "/accept")
                        .header("Authorization", "Bearer " + recipientToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.timestamp").value(matchesRegex("^\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}(\\.\\d+)?Z$")))
                .andExpect(jsonPath("$.message").value("Taklif qabul qilindi va jamoaga qo'shildingiz"))
                .andReturn();

        saveExample("accept_success.json", result);
    }

    @Test
    @DisplayName("23. Success: Admin yangi email-taklif yaratadi")
    void testCreateEmailInviteSuccess() throws Exception {
        User freshUser = userRepository.save(User.builder()
                .name("fresh_" + UUID.randomUUID().toString().substring(0, 6))
                .fullName("Fresh User")
                .email("fresh_" + UUID.randomUUID().toString().substring(0, 6) + "@example.com")
                .password("pass")
                .role(User.Role.USER)
                .build());

        MvcResult result = mvc.perform(post("/api/workspaces/" + workspace.getId() + "/invites")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"usernameOrEmail\":\"" + freshUser.getEmail() + "\",\"role\":\"MEMBER\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.timestamp").value(matchesRegex("^\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}(\\.\\d+)?Z$")))
                .andExpect(jsonPath("$.data.createdAt").value(matchesRegex("^\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}(\\.\\d+)?Z$")))
                .andExpect(jsonPath("$.data.expiresAt").value(matchesRegex("^\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}(\\.\\d+)?Z$")))
                .andExpect(jsonPath("$.data.type").value("EMAIL"))
                .andExpect(jsonPath("$.data.token").isNotEmpty())
                .andReturn();

        saveExample("create_email_invite_success.json", result);
    }

    @Test
    @DisplayName("24. Success: Admin yangi havola-taklif (LINK) yaratadi")
    void testCreateLinkInviteSuccess() throws Exception {
        MvcResult result = mvc.perform(post("/api/workspaces/" + workspace.getId() + "/invites/link")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"role\":\"MEMBER\",\"maxUses\":10,\"expireDays\":7}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.timestamp").value(matchesRegex("^\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}(\\.\\d+)?Z$")))
                .andExpect(jsonPath("$.data.createdAt").value(matchesRegex("^\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}(\\.\\d+)?Z$")))
                .andExpect(jsonPath("$.data.expiresAt").value(matchesRegex("^\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}(\\.\\d+)?Z$")))
                .andExpect(jsonPath("$.data.type").value("LINK"))
                .andExpect(jsonPath("$.data.token").isNotEmpty())
                .andReturn();

        saveExample("create_link_invite_success.json", result);
    }

    @Test
    @DisplayName("25. Success: Menga kelgan takliflar ro'yxati (/me)")
    void testGetMyInvitationsSuccess() throws Exception {
        String rawToken = InvitationTokenUtil.generateToken();
        String tokenHash = InvitationTokenUtil.hashToken(rawToken);

        invitationRepository.save(WorkspaceInvitation.builder()
                .workspaceId(workspace.getId())
                .senderId(admin.getId())
                .receiverId(recipient.getId())
                .receiverEmail(recipient.getEmail())
                .type(InvitationType.EMAIL)
                .tokenHash(tokenHash)
                .role(WorkspaceRole.MEMBER)
                .status(InvitationStatus.PENDING)
                .expiresAt(LocalDateTime.now(java.time.ZoneOffset.UTC).plusDays(5))
                .build());

        MvcResult result = mvc.perform(get("/api/invitations/me")
                        .header("Authorization", "Bearer " + recipientToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.timestamp").value(matchesRegex("^\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}(\\.\\d+)?Z$")))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data[0].createdAt").value(matchesRegex("^\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}(\\.\\d+)?Z$")))
                .andExpect(jsonPath("$.data[0].expiresAt").value(matchesRegex("^\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}(\\.\\d+)?Z$")))
                .andReturn();

        saveExample("my_invitations_success.json", result);
    }

    @Test
    @DisplayName("26. ISO-8601 UTC format: Barcha sana maydonlari ISO-8601 'Z' bilan tugaydi")
    void testAllDateFieldsEndWithZ() throws Exception {
        MvcResult result = mvc.perform(post("/api/workspaces/" + workspace.getId() + "/invites/link")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"role\":\"MEMBER\",\"maxUses\":5,\"durationDays\":3}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.timestamp").value(matchesRegex("^\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}(\\.\\d+)?Z$")))
                .andExpect(jsonPath("$.data.createdAt").value(matchesRegex("^\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}(\\.\\d+)?Z$")))
                .andExpect(jsonPath("$.data.expiresAt").value(matchesRegex("^\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}(\\.\\d+)?Z$")))
                .andReturn();

        saveExample("iso_utc_dates_example.json", result);
    }
}
