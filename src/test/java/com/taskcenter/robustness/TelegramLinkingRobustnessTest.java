package com.taskcenter.robustness;

import com.jayway.jsonpath.JsonPath;
import com.taskcenter.model.User;
import com.taskcenter.repository.UserRepository;
import com.taskcenter.security.JwtTokenProvider;
import com.taskcenter.security.TelegramInitDataValidator;
import com.taskcenter.service.TelegramBotService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import jakarta.persistence.EntityManager;
import org.telegram.telegrambots.meta.api.objects.Chat;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
public class TelegramLinkingRobustnessTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private UserRepository userRepository;

    @SpyBean
    private TelegramBotService botService;

    @Autowired
    private EntityManager entityManager;

    // We can't mock TelegramInitDataValidator easily if it's not a mock bean, 
    // but we can generate a valid initData using its own logic if we know the token,
    // or we can test the expectations that are testable via API.
    // The prompt says: "can one Telegram id end up linked to two accounts (409)"
    // Let's test the endpoint directly.

    private String createTestUser(String username) throws Exception {
        String body = mvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"" + username + "\",\"password\":\"password123\"}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$.data.token");
    }

    private String getLinkToken(String jwt) throws Exception {
        String body = mvc.perform(get("/api/users/me/telegram-link-token")
                        .header("Authorization", "Bearer " + jwt))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$.data.token");
    }

    private void simulateTelegramStart(long chatId, String token) {
        org.mockito.Mockito.doNothing().when(botService).sendMessageWithMainKeyboard(org.mockito.ArgumentMatchers.anyLong(), org.mockito.ArgumentMatchers.anyString());
        org.mockito.Mockito.doNothing().when(botService).sendMessage(org.mockito.ArgumentMatchers.anyLong(), org.mockito.ArgumentMatchers.anyString());
        Update update = new Update();
        Message message = new Message();
        Chat chat = new Chat();
        chat.setId(chatId);
        chat.setType("private");
        message.setChat(chat);
        message.setText("/start " + token);
        update.setMessage(message);
        botService.onUpdateReceived(update);
        entityManager.flush();
        entityManager.clear();
    }

    @Test
    void sameTelegramIdCannotBeLinkedToTwoAccounts() throws Exception {
        String jwt1 = createTestUser("user1_tg");
        String jwt2 = createTestUser("user2_tg");

        String linkToken1 = getLinkToken(jwt1);
        simulateTelegramStart(7777777L, linkToken1);

        String linkToken2 = getLinkToken(jwt2);
        simulateTelegramStart(7777777L, linkToken2);

        // Expectation: user2 should NOT be linked to 7777777L because it's already linked to user1.
        User user2 = userRepository.findByName("user2_tg").get();
        assertNull(user2.getTelegramChatId());
    }

    @Test
    void relinkingSameAccountIsIdempotent() throws Exception {
        String jwt = createTestUser("user3_tg");
        
        String linkToken1 = getLinkToken(jwt);
        simulateTelegramStart(8888888L, linkToken1);

        String linkToken2 = getLinkToken(jwt);
        simulateTelegramStart(8888888L, linkToken2);

        User user = userRepository.findByName("user3_tg").get();
        assertEquals(8888888L, user.getTelegramChatId());
    }

    @Test
    void linkTokenExpires() throws Exception {
        String jwt = createTestUser("user4_tg");
        String linkToken = getLinkToken(jwt);
        
        // Manually expire it in DB
        User user = userRepository.findByName("user4_tg").get();
        user.setTelegramLinkTokenExpiresAt(LocalDateTime.now().minusMinutes(10));
        userRepository.save(user);

        simulateTelegramStart(9999999L, linkToken);

        User userAfter = userRepository.findByName("user4_tg").get();
        assertNull(userAfter.getTelegramChatId());
    }

    @Test
    void linkTokenCannotBeUsedTwice() throws Exception {
        String jwt = createTestUser("user5_tg");
        String linkToken = getLinkToken(jwt);

        simulateTelegramStart(1111111L, linkToken);

        // Try to use it again for another telegram ID
        simulateTelegramStart(2222222L, linkToken);

        User user = userRepository.findByName("user5_tg").get();
        assertEquals(1111111L, user.getTelegramChatId());
    }

    @Test
    void linkTokenStopsWorkingAfterUnlink() throws Exception {
        String jwt = createTestUser("user6_tg");
        String linkToken = getLinkToken(jwt);

        // Unlink BEFORE using the token
        mvc.perform(delete("/api/users/me/telegram").header("Authorization", "Bearer " + jwt))
                .andExpect(status().isOk());

        simulateTelegramStart(3333333L, linkToken);

        User user = userRepository.findByName("user6_tg").get();
        assertNull(user.getTelegramChatId());
    }
}
