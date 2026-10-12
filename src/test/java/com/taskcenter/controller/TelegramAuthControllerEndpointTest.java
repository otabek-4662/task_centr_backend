package com.taskcenter.controller;

import com.taskcenter.model.User;
import com.taskcenter.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "telegram.bot.token=123456789:ABCdefGhIJKlmNoPQRsTUVwxyZ",
        "telegram.miniapp.max-age-sec=86400"
})
@Transactional
class TelegramAuthControllerEndpointTest {

    private static final String BOT_TOKEN = "123456789:ABCdefGhIJKlmNoPQRsTUVwxyZ";

    @Autowired
    private MockMvc mvc;

    @Autowired
    private UserRepository userRepository;

    private User testUser;
    private long telegramChatId;

    @BeforeEach
    void setUp() {
        telegramChatId = 777123456L;
        testUser = userRepository.save(User.builder()
                .name("tg_user_" + UUID.randomUUID().toString().substring(0, 6))
                .fullName("Telegram User")
                .email("tg_user_" + UUID.randomUUID().toString().substring(0, 6) + "@example.com")
                .password("password123")
                .role(User.Role.USER)
                .telegramChatId(telegramChatId)
                .build());
    }

    private String generateInitData(Map<String, String> params, String botToken) {
        Map<String, String> sortedParams = new TreeMap<>(params);
        String dataCheck = sortedParams.entrySet().stream()
                .map(e -> e.getKey() + "=" + e.getValue())
                .collect(Collectors.joining("\n"));

        byte[] secret = hmac("WebAppData".getBytes(StandardCharsets.UTF_8), botToken.getBytes(StandardCharsets.UTF_8));
        String hash = HexFormat.of().formatHex(hmac(secret, dataCheck.getBytes(StandardCharsets.UTF_8)));

        return sortedParams.entrySet().stream()
                .map(e -> e.getKey() + "=" + URLEncoder.encode(e.getValue(), StandardCharsets.UTF_8))
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

    @Test
    @DisplayName("POST /api/auth/telegram: haqiqiy initData bilan token olinadi (200)")
    void testTelegramLogin_success_returnsTokenAndUser() throws Exception {
        Map<String, String> params = new LinkedHashMap<>();
        params.put("auth_date", String.valueOf(Instant.now().getEpochSecond() - 30));
        params.put("query_id", "AAHdF6IQAAAAAN0XohDhrOrc");
        params.put("user", "{\"id\":" + telegramChatId + ",\"first_name\":\"Telegram\",\"username\":\"tguser\"}");
        String validInitData = generateInitData(params, BOT_TOKEN);

        mvc.perform(post("/api/auth/telegram")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"initData\":\"" + validInitData + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.token").isNotEmpty())
                .andExpect(jsonPath("$.data.refreshToken").isNotEmpty())
                .andExpect(jsonPath("$.data.user.id").value(testUser.getId()))
                .andExpect(jsonPath("$.data.user.name").value(testUser.getName()))
                .andExpect(jsonPath("$.data.user.telegramLinked").value(true));
    }

    @Test
    @DisplayName("POST /api/auth/telegram: o'zgartirilgan initData 401 beradi")
    void testTelegramLogin_tamperedInitData_returns401() throws Exception {
        Map<String, String> params = new LinkedHashMap<>();
        params.put("auth_date", String.valueOf(Instant.now().getEpochSecond() - 30));
        params.put("query_id", "AAHdF6IQAAAAAN0XohDhrOrc");
        params.put("user", "{\"id\":" + telegramChatId + ",\"first_name\":\"Telegram\"}");
        String validInitData = generateInitData(params, BOT_TOKEN);
        String tamperedInitData = validInitData.substring(0, validInitData.length() - 4) + "beef";

        mvc.perform(post("/api/auth/telegram")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"initData\":\"" + tamperedInitData + "\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.error").value("UNAUTHORIZED"))
                .andExpect(jsonPath("$.message").value("Invalid or expired initData"));
    }

    @Test
    @DisplayName("POST /api/auth/telegram: eskirgan auth_date (> 24h) 401 beradi")
    void testTelegramLogin_expiredAuthDate_returns401() throws Exception {
        Map<String, String> params = new LinkedHashMap<>();
        params.put("auth_date", String.valueOf(Instant.now().getEpochSecond() - (25 * 3600)));
        params.put("query_id", "AAHdF6IQAAAAAN0XohDhrOrc");
        params.put("user", "{\"id\":" + telegramChatId + ",\"first_name\":\"Telegram\"}");
        String expiredInitData = generateInitData(params, BOT_TOKEN);

        mvc.perform(post("/api/auth/telegram")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"initData\":\"" + expiredInitData + "\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.error").value("UNAUTHORIZED"))
                .andExpect(jsonPath("$.message").value("Invalid or expired initData"));
    }

    @Test
    @DisplayName("POST /api/auth/telegram: foydalanuvchi bazada yo'q bo'lsa 401 'Avval botda akkauntingizni ulang' beradi")
    void testTelegramLogin_unlinkedUser_returns401() throws Exception {
        long unlinkedTelegramId = 999888777L;
        Map<String, String> params = new LinkedHashMap<>();
        params.put("auth_date", String.valueOf(Instant.now().getEpochSecond() - 30));
        params.put("query_id", "AAHdF6IQAAAAAN0XohDhrOrc");
        params.put("user", "{\"id\":" + unlinkedTelegramId + ",\"first_name\":\"Stranger\"}");
        String validInitData = generateInitData(params, BOT_TOKEN);

        mvc.perform(post("/api/auth/telegram")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"initData\":\"" + validInitData + "\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.error").value("UNAUTHORIZED"))
                .andExpect(jsonPath("$.message").value("Avval botda akkauntingizni ulang"));
    }

    @Test
    @DisplayName("POST /api/v1/auth/telegram: mavjud v1 prefiksi ham ishlaydi (orqaga moslik)")
    void testTelegramLogin_v1Path_returns200() throws Exception {
        Map<String, String> params = new LinkedHashMap<>();
        params.put("auth_date", String.valueOf(Instant.now().getEpochSecond() - 30));
        params.put("query_id", "AAHdF6IQAAAAAN0XohDhrOrc");
        params.put("user", "{\"id\":" + telegramChatId + ",\"first_name\":\"Telegram\"}");
        String validInitData = generateInitData(params, BOT_TOKEN);

        mvc.perform(post("/api/v1/auth/telegram")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"initData\":\"" + validInitData + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.token").isNotEmpty());
    }

    @Test
    @DisplayName("X-Telegram-Init-Data sarlavhasi: mavjud header usuli buzilmagan (orqaga moslik)")
    void testHeaderAuthentication_backwardCompatibility() throws Exception {
        Map<String, String> params = new LinkedHashMap<>();
        params.put("auth_date", String.valueOf(Instant.now().getEpochSecond() - 30));
        params.put("query_id", "AAHdF6IQAAAAAN0XohDhrOrc");
        params.put("user", "{\"id\":" + telegramChatId + ",\"first_name\":\"Telegram\"}");
        String validInitData = generateInitData(params, BOT_TOKEN);

        mvc.perform(get("/api/users/me/tasks")
                        .header("X-Telegram-Init-Data", validInitData))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }
}
