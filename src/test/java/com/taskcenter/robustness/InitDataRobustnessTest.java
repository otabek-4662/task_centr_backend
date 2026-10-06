package com.taskcenter.robustness;

import com.taskcenter.security.TelegramInitDataValidator;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class InitDataRobustnessTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private TelegramInitDataValidator validator;

    @Test
    void missingHash_producesRejectionAndNotException() throws Exception {
        String payload = "{\"initData\":\"auth_date=1690000000&query_id=ABC&user=%7B%22id%22%3A123%7D\"}";
        mvc.perform(post("/api/v1/auth/telegram")
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void duplicateKeys_producesRejectionAndNotException() throws Exception {
        String payload = "{\"initData\":\"auth_date=1690000000&auth_date=1690000001&hash=fakehash\"}";
        mvc.perform(post("/api/v1/auth/telegram")
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void malformedUserJson_producesRejectionAndNotException() throws Exception {
        String badUser = URLEncoder.encode("{bad_json}", StandardCharsets.UTF_8);
        String payload = "{\"initData\":\"auth_date=1690000000&hash=fakehash&user=" + badUser + "\"}";
        mvc.perform(post("/api/v1/auth/telegram")
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void expiredAuthDate_producesRejectionAndNotException() throws Exception {
        String payload = "{\"initData\":\"auth_date=1000000000&hash=fakehash&user=%7B%22id%22%3A123%7D\"}";
        mvc.perform(post("/api/v1/auth/telegram")
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void emptyBotToken_producesRejectionAndNotException() throws Exception {
        TelegramInitDataValidator emptyTokenValidator = new TelegramInitDataValidator("", 3600);
        String initData = "auth_date=2000000000&hash=fakehash&user=%7B%22id%22%3A123%7D";
        assertTrue(emptyTokenValidator.validate(initData).isEmpty());
    }
}
