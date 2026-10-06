package com.taskcenter.controller;

import com.taskcenter.service.TelegramBotService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.telegram.telegrambots.meta.api.objects.Update;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class TelegramWebhookControllerTest {

    @Mock
    private TelegramBotService telegramBotService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        TelegramWebhookController controller = new TelegramWebhookController(telegramBotService, "secret-webhook-key");
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    void handleTelegramUpdate_validSecret_success() throws Exception {
        mockMvc.perform(post("/api/webhooks/telegram")
                        .header("X-Telegram-Bot-Api-Secret-Token", "secret-webhook-key")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"update_id\": 10001}"))
                .andExpect(status().isOk());

        verify(telegramBotService, times(1)).onUpdateReceived(any(Update.class));
    }

    @Test
    void handleTelegramUpdate_invalidSecret_returns401() throws Exception {
        mockMvc.perform(post("/api/webhooks/telegram")
                        .header("X-Telegram-Bot-Api-Secret-Token", "wrong-key")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"update_id\": 10001}"))
                .andExpect(status().isUnauthorized());

        verify(telegramBotService, never()).onUpdateReceived(any());
    }

    @Test
    void handleTelegramUpdate_missingSecretHeader_returns401() throws Exception {
        mockMvc.perform(post("/api/webhooks/telegram")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"update_id\": 10001}"))
                .andExpect(status().isUnauthorized());

        verify(telegramBotService, never()).onUpdateReceived(any());
    }

    @Test
    void handleTelegramUpdate_noSecretConfigured_allowsRequest() throws Exception {
        TelegramWebhookController noSecretController = new TelegramWebhookController(telegramBotService, "");
        MockMvc noSecretMvc = MockMvcBuilders.standaloneSetup(noSecretController).build();

        noSecretMvc.perform(post("/api/webhooks/telegram")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"update_id\": 10001}"))
                .andExpect(status().isOk());

        verify(telegramBotService, times(1)).onUpdateReceived(any(Update.class));
    }
}
