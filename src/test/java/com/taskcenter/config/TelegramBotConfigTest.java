package com.taskcenter.config;

import com.taskcenter.service.TelegramBotService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.api.methods.updates.SetWebhook;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TelegramBotConfigTest {

    @Mock
    private TelegramBotService telegramBotService;

    @Test
    void telegramBotsApi_withWebhookUrl_configuresWebhookAndSkipsLongPolling() throws Exception {
        TelegramBotConfig config = new TelegramBotConfig();
        ReflectionTestUtils.setField(config, "webhookUrl", "https://api.taskcenter.uz/api/webhooks/telegram");
        ReflectionTestUtils.setField(config, "webhookSecret", "super-secret");

        TelegramBotsApi botsApi = config.telegramBotsApi(telegramBotService);

        // Long-polling botsApi null bo'lishi kerak (bitta botdan 409 xatosi chiqmasligi uchun)
        assertNull(botsApi);

        ArgumentCaptor<SetWebhook> captor = ArgumentCaptor.forClass(SetWebhook.class);
        verify(telegramBotService, times(1)).execute(captor.capture());

        SetWebhook setWebhook = captor.getValue();
        assertEquals("https://api.taskcenter.uz/api/webhooks/telegram", setWebhook.getUrl());
        assertEquals("super-secret", setWebhook.getSecretToken());
    }

    @Test
    void telegramBotsApi_withWebhookError_handlesGracefully() throws Exception {
        TelegramBotConfig config = new TelegramBotConfig();
        ReflectionTestUtils.setField(config, "webhookUrl", "https://api.taskcenter.uz/api/webhooks/telegram");

        doThrow(new TelegramApiException("Network error")).when(telegramBotService).execute(any(SetWebhook.class));

        TelegramBotsApi botsApi = config.telegramBotsApi(telegramBotService);
        assertNull(botsApi);
        verify(telegramBotService, times(1)).execute(any(SetWebhook.class));
    }
}
