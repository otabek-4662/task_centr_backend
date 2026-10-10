package com.taskcenter.controller;

import com.jayway.jsonpath.JsonPath;
import com.taskcenter.model.*;
import com.taskcenter.repository.UserRepository;
import com.taskcenter.repository.WorkspaceInvitationRepository;
import com.taskcenter.repository.WorkspaceRepository;
import com.taskcenter.security.RateLimitingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class WorkspaceInvitationControllerTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private WorkspaceRepository workspaceRepository;

    @Autowired
    private WorkspaceInvitationRepository invitationRepository;

    @Autowired
    private com.taskcenter.security.JwtTokenProvider jwtTokenProvider;

    @MockBean
    private RateLimitingService rateLimitingService;

    private User admin;
    private User recipient;
    private User otherUser;
    private Workspace workspace;

    private String adminToken;
    private String recipientToken;
    private String otherToken;

    @BeforeEach
    void setUp() throws Exception {
        // Mock default rate limiting to always allow
        when(rateLimitingService.tryConsumeIpLimit(anyString(), anyString(), anyInt(), anyInt())).thenReturn(0L);
        when(rateLimitingService.tryConsumeUserLimit(anyString(), anyString(), anyInt(), anyInt())).thenReturn(0L);

        admin = userRepository.save(User.builder()
                .name("ctrl_admin_" + UUID.randomUUID().toString().substring(0, 6))
                .fullName("Admin")
                .email("admin@test.com")
                .password("pass")
                .role(User.Role.USER)
                .build());

        recipient = userRepository.save(User.builder()
                .name("ctrl_rec_" + UUID.randomUUID().toString().substring(0, 6))
                .fullName("Recipient")
                .email("recipient@test.com")
                .password("pass")
                .role(User.Role.USER)
                .build());

        otherUser = userRepository.save(User.builder()
                .name("ctrl_oth_" + UUID.randomUUID().toString().substring(0, 6))
                .fullName("Other")
                .email("other@test.com")
                .password("pass")
                .role(User.Role.USER)
                .build());

        adminToken = jwtTokenProvider.generateTokenFromUser(admin);
        recipientToken = jwtTokenProvider.generateTokenFromUser(recipient);
        otherToken = jwtTokenProvider.generateTokenFromUser(otherUser);

        workspace = workspaceRepository.save(Workspace.builder()
                .title("Controller Test Workspace")
                .ownerId(admin.getId())
                .build());
    }

    @Test
    @DisplayName("GET /api/invitations/me: receiverId bo'yicha takliflar qaytariladi")
    void getMyInvitations_returnsOnlyReceiverIdInvitations() throws Exception {
        WorkspaceInvitation inv = invitationRepository.save(WorkspaceInvitation.builder()
                .workspaceId(workspace.getId())
                .senderId(admin.getId())
                .receiverId(recipient.getId())
                .type(InvitationType.EMAIL)
                .role(WorkspaceRole.MEMBER)
                .status(InvitationStatus.PENDING)
                .tokenHash("hash_me_test")
                .useCount(0)
                .emailDeliveryStatus(EmailDeliveryStatus.NOT_APPLICABLE)
                .expiresAt(LocalDateTime.now().plusDays(5))
                .build());

        mvc.perform(get("/api/invitations/me")
                        .header("Authorization", "Bearer " + recipientToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data[0].id").value(inv.getId()))
                .andExpect(jsonPath("$.data[0].workspaceTitle").value(workspace.getTitle()));
    }

    @Test
    @DisplayName("POST /api/invitations/by-id/{id}/accept: Qabul qiluvchi o'z taklifini qabul qilishi mumkin")
    void acceptInvitationById_success() throws Exception {
        WorkspaceInvitation inv = invitationRepository.save(WorkspaceInvitation.builder()
                .workspaceId(workspace.getId())
                .senderId(admin.getId())
                .receiverId(recipient.getId())
                .receiverEmail(recipient.getEmail())
                .type(InvitationType.EMAIL)
                .role(WorkspaceRole.MEMBER)
                .status(InvitationStatus.PENDING)
                .tokenHash("hash_accept_test")
                .useCount(0)
                .emailDeliveryStatus(EmailDeliveryStatus.NOT_APPLICABLE)
                .expiresAt(LocalDateTime.now().plusDays(5))
                .build());

        mvc.perform(post("/api/invitations/by-id/" + inv.getId() + "/accept")
                        .header("Authorization", "Bearer " + recipientToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Taklif qabul qilindi va jamoaga qo'shildingiz"));
    }

    @Test
    @DisplayName("POST /api/invitations/by-id/{id}/accept: Boshqa user chaqirganda 403 Forbidden qaytaradi")
    void acceptInvitationById_forbiddenForOtherUser() throws Exception {
        WorkspaceInvitation inv = invitationRepository.save(WorkspaceInvitation.builder()
                .workspaceId(workspace.getId())
                .senderId(admin.getId())
                .receiverId(recipient.getId())
                .receiverEmail(recipient.getEmail())
                .type(InvitationType.EMAIL)
                .role(WorkspaceRole.MEMBER)
                .status(InvitationStatus.PENDING)
                .tokenHash("hash_forbidden_test")
                .useCount(0)
                .emailDeliveryStatus(EmailDeliveryStatus.NOT_APPLICABLE)
                .expiresAt(LocalDateTime.now().plusDays(5))
                .build());

        mvc.perform(post("/api/invitations/by-id/" + inv.getId() + "/accept")
                        .header("Authorization", "Bearer " + otherToken))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("POST /api/invitations/by-id/{id}/reject: Qabul qiluvchi rad etganda muvaffaqiyatli bajariladi")
    void rejectInvitationById_success() throws Exception {
        WorkspaceInvitation inv = invitationRepository.save(WorkspaceInvitation.builder()
                .workspaceId(workspace.getId())
                .senderId(admin.getId())
                .receiverId(recipient.getId())
                .type(InvitationType.EMAIL)
                .role(WorkspaceRole.MEMBER)
                .status(InvitationStatus.PENDING)
                .tokenHash("hash_reject_test")
                .useCount(0)
                .emailDeliveryStatus(EmailDeliveryStatus.NOT_APPLICABLE)
                .expiresAt(LocalDateTime.now().plusDays(5))
                .build());

        mvc.perform(post("/api/invitations/by-id/" + inv.getId() + "/reject")
                        .header("Authorization", "Bearer " + recipientToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Taklif rad etildi"));
    }

    @Test
    @DisplayName("4. POST /workspaces/{id}/invites/test-email: Limit oshganda 429 Too Many Requests qaytaradi")
    void testEmail_returns429WhenRateLimitExceeded() throws Exception {
        when(rateLimitingService.tryConsumeUserLimit(eq("test-email"), eq(admin.getId()), eq(5), eq(60)))
                .thenReturn(2400L); // 40 daqiqa kutish

        mvc.perform(post("/api/workspaces/" + workspace.getId() + "/invites/test-email")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.code").value("TOO_MANY_REQUESTS"))
                .andExpect(jsonPath("$.status").value(429));
    }

    @Test
    @DisplayName("2. POST /api/invitations/by-id/{id}/accept: LINK taklif uchun 400 Bad Request qaytaradi")
    void acceptInvitationById_linkType_returns400() throws Exception {
        WorkspaceInvitation linkInv = invitationRepository.save(WorkspaceInvitation.builder()
                .workspaceId(workspace.getId())
                .senderId(admin.getId())
                .receiverId(recipient.getId())
                .type(InvitationType.LINK)
                .role(WorkspaceRole.MEMBER)
                .status(InvitationStatus.PENDING)
                .tokenHash("hash_link_accept_test")
                .useCount(0)
                .emailDeliveryStatus(EmailDeliveryStatus.NOT_APPLICABLE)
                .expiresAt(LocalDateTime.now().plusDays(5))
                .build());

        mvc.perform(post("/api/invitations/by-id/" + linkInv.getId() + "/accept")
                        .header("Authorization", "Bearer " + recipientToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"));
    }

    @Test
    @DisplayName("2. POST /api/invitations/by-id/{id}/reject: LINK taklif uchun 400 Bad Request qaytaradi")
    void rejectInvitationById_linkType_returns400() throws Exception {
        WorkspaceInvitation linkInv = invitationRepository.save(WorkspaceInvitation.builder()
                .workspaceId(workspace.getId())
                .senderId(admin.getId())
                .receiverId(recipient.getId())
                .type(InvitationType.LINK)
                .role(WorkspaceRole.MEMBER)
                .status(InvitationStatus.PENDING)
                .tokenHash("hash_link_reject_test")
                .useCount(0)
                .emailDeliveryStatus(EmailDeliveryStatus.NOT_APPLICABLE)
                .expiresAt(LocalDateTime.now().plusDays(5))
                .build());

        mvc.perform(post("/api/invitations/by-id/" + linkInv.getId() + "/reject")
                        .header("Authorization", "Bearer " + recipientToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"));
    }
}
