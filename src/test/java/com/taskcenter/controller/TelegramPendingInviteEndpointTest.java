package com.taskcenter.controller;

import com.taskcenter.model.*;
import com.taskcenter.repository.PendingTelegramChatInviteRepository;
import com.taskcenter.repository.UserRepository;
import com.taskcenter.repository.WorkspaceInvitationRepository;
import com.taskcenter.repository.WorkspaceRepository;
import com.taskcenter.security.RateLimitingService;
import com.taskcenter.util.InvitationTokenUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URLEncoder;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.*;
import java.util.stream.Collectors;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "telegram.bot.token=123456789:ABCdefGhIJKlmNoPQRsTUVwxyZ",
        "telegram.bot.username=test_bot",
        "telegram.miniapp.max-age-sec=3600",
        "ratelimit.auth.enabled=true"
})
@Transactional
class TelegramPendingInviteEndpointTest {

    private static final String BOT_TOKEN = "123456789:ABCdefGhIJKlmNoPQRsTUVwxyZ";

    @Autowired
    private MockMvc mvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private WorkspaceRepository workspaceRepository;

    @Autowired
    private WorkspaceInvitationRepository invitationRepository;

    @Autowired
    private PendingTelegramChatInviteRepository pendingChatInviteRepository;

    @MockBean
    private RateLimitingService rateLimitingService;

    private User admin;
    private Workspace workspace;
    private String rawInviteToken;

    private String generateInitData(Map<String, String> params, String botToken) {
        Map<String, String> sortedParams = new TreeMap<>(params);
        String dataCheck = sortedParams.entrySet().stream()
                .map(e -> e.getKey() + "=" + e.getValue())
                .collect(Collectors.joining("\n"));

        byte[] secret = hmac("WebAppData".getBytes(UTF_8), botToken.getBytes(UTF_8));
        String hash = HexFormat.of().formatHex(hmac(secret, dataCheck.getBytes(UTF_8)));

        return sortedParams.entrySet().stream()
                .map(e -> e.getKey() + "=" + URLEncoder.encode(e.getValue(), UTF_8))
                .collect(Collectors.joining("&")) + "&hash=" + hash;
    }

    private static byte[] hmac(byte[] key, byte[] data) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(key, "HmacSHA256"));
            return mac.doFinal(data);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @BeforeEach
    void setUp() {
        when(rateLimitingService.tryConsumeIpLimit(anyString(), anyString(), anyInt(), anyInt())).thenReturn(0L);

        admin = userRepository.save(User.builder()
                .name("admin_" + UUID.randomUUID().toString().substring(0, 6))
                .email("admin_" + UUID.randomUUID().toString().substring(0, 6) + "@example.com")
                .password("password")
                .role(User.Role.ADMIN)
                .build());

        workspace = workspaceRepository.save(Workspace.builder()
                .title("Pending Invite Workspace")
                .ownerId(admin.getId())
                .build());

        rawInviteToken = InvitationTokenUtil.generateToken();
        invitationRepository.save(WorkspaceInvitation.builder()
                .workspaceId(workspace.getId())
                .senderId(admin.getId())
                .type(InvitationType.LINK)
                .tokenHash(InvitationTokenUtil.hashToken(rawInviteToken))
                .role(WorkspaceRole.MEMBER)
                .status(InvitationStatus.PENDING)
                .expiresAt(LocalDateTime.now(ZoneOffset.UTC).plusDays(5))
                .build());
    }

    @Test
    @DisplayName("1. Soxta hash: initData xeshi o'zgartirilganda 401 qaytadi")
    void testTamperedHash_returns401() throws Exception {
        Map<String, String> params = new LinkedHashMap<>();
        params.put("auth_date", String.valueOf(Instant.now().getEpochSecond() - 30));
        params.put("user", "{\"id\":777001,\"first_name\":\"Tampered\"}");
        String validInitData = generateInitData(params, BOT_TOKEN);

        String tamperedInitData = validInitData.replace("hash=", "hash=deadbeef000111");

        mvc.perform(post("/api/v1/auth/telegram/pending-invite")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"initData\":\"" + tamperedInitData + "\",\"inviteToken\":\"" + rawInviteToken + "\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("2. Eski auth_date: auth_date maxAgeSec (3600s) dan eski bo'lsa 401 qaytadi")
    void testExpiredAuthDate_returns401() throws Exception {
        Map<String, String> params = new LinkedHashMap<>();
        params.put("auth_date", String.valueOf(Instant.now().getEpochSecond() - 7200)); // 2 hours ago
        params.put("user", "{\"id\":777002,\"first_name\":\"OldAuthDate\"}");
        String expiredInitData = generateInitData(params, BOT_TOKEN);

        mvc.perform(post("/api/v1/auth/telegram/pending-invite")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"initData\":\"" + expiredInitData + "\",\"inviteToken\":\"" + rawInviteToken + "\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("3. Mavjud bo'lmagan token: 404 va INVITE_NOT_FOUND qaytadi")
    void testNonExistentToken_returns404InviteNotFound() throws Exception {
        Map<String, String> params = new LinkedHashMap<>();
        params.put("auth_date", String.valueOf(Instant.now().getEpochSecond() - 30));
        params.put("user", "{\"id\":777003,\"first_name\":\"ValidUser\"}");
        String validInitData = generateInitData(params, BOT_TOKEN);

        mvc.perform(post("/api/v1/auth/telegram/pending-invite")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"initData\":\"" + validInitData + "\",\"inviteToken\":\"non-existent-token-xyz\"}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value("INVITE_NOT_FOUND"));
    }

    @Test
    @DisplayName("4. Faol bo'lmagan (muddati o'tgan) token: 400 va INVITE_EXPIRED qaytadi")
    void testExpiredInviteToken_returns400InviteExpired() throws Exception {
        String expiredToken = InvitationTokenUtil.generateToken();
        invitationRepository.save(WorkspaceInvitation.builder()
                .workspaceId(workspace.getId())
                .senderId(admin.getId())
                .type(InvitationType.LINK)
                .tokenHash(InvitationTokenUtil.hashToken(expiredToken))
                .role(WorkspaceRole.MEMBER)
                .status(InvitationStatus.PENDING)
                .expiresAt(LocalDateTime.now(ZoneOffset.UTC).minusDays(1))
                .build());

        Map<String, String> params = new LinkedHashMap<>();
        params.put("auth_date", String.valueOf(Instant.now().getEpochSecond() - 30));
        params.put("user", "{\"id\":777004,\"first_name\":\"ValidUser\"}");
        String validInitData = generateInitData(params, BOT_TOKEN);

        mvc.perform(post("/api/v1/auth/telegram/pending-invite")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"initData\":\"" + validInitData + "\",\"inviteToken\":\"" + expiredToken + "\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value("INVITE_EXPIRED"));
    }

    @Test
    @DisplayName("5. Muvaffaqiyat: botLink qaytadi va telegram_pending_chat_invites jadvaliga 24 soatlik yozuv saqlanadi")
    void testSuccess_stores24hPendingInviteAndReturnsBotLink() throws Exception {
        long telegramUserId = 88812345L;
        Map<String, String> params = new LinkedHashMap<>();
        params.put("auth_date", String.valueOf(Instant.now().getEpochSecond() - 30));
        params.put("user", "{\"id\":" + telegramUserId + ",\"first_name\":\"TelegramGuest\"}");
        String validInitData = generateInitData(params, BOT_TOKEN);

        mvc.perform(post("/api/v1/auth/telegram/pending-invite")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"initData\":\"" + validInitData + "\",\"inviteToken\":\"" + rawInviteToken + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.botLink").value("https://t.me/test_bot?start=inv_" + rawInviteToken));

        // Bazadagi yozuvni tekshirish
        Optional<PendingTelegramChatInvite> pendingOpt = pendingChatInviteRepository.findById(telegramUserId);
        assertThat(pendingOpt).isPresent();
        PendingTelegramChatInvite pending = pendingOpt.get();
        assertThat(pending.getToken()).isEqualTo(rawInviteToken);
        assertThat(pending.getExpiresAt()).isAfter(LocalDateTime.now(ZoneOffset.UTC).plusHours(23));
        assertThat(pending.getExpiresAt()).isBeforeOrEqualTo(LocalDateTime.now(ZoneOffset.UTC).plusHours(25));
    }

    @Test
    @DisplayName("6. Rate Limit: So'rovlar soni oshganda 429 va RATE_LIMITED qaytadi")
    void testRateLimit_returns429RateLimited() throws Exception {
        when(rateLimitingService.tryConsumeIpLimit(eq("telegram-auth"), anyString(), eq(30), eq(1)))
                .thenReturn(45L);

        Map<String, String> params = new LinkedHashMap<>();
        params.put("auth_date", String.valueOf(Instant.now().getEpochSecond() - 30));
        params.put("user", "{\"id\":999001,\"first_name\":\"RateLimitedUser\"}");
        String validInitData = generateInitData(params, BOT_TOKEN);

        mvc.perform(post("/api/v1/auth/telegram/pending-invite")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"initData\":\"" + validInitData + "\",\"inviteToken\":\"" + rawInviteToken + "\"}"))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().string("Retry-After", "45"))
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value("RATE_LIMITED"))
                .andExpect(jsonPath("$.retryAfterSeconds").value(45));
    }
}
