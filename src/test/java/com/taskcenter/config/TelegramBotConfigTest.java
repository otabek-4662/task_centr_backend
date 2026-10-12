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

    @Test
    void destroy_stopsBotSessionAndReleasesLock() throws Exception {
        javax.sql.DataSource dataSource = mock(javax.sql.DataSource.class);
        TelegramBotConfig config = new TelegramBotConfig(dataSource, telegramBotService);

        org.telegram.telegrambots.meta.generics.BotSession mockSession = mock(org.telegram.telegrambots.meta.generics.BotSession.class);
        ReflectionTestUtils.setField(config, "botSession", mockSession);

        java.sql.Connection mockConnection = mock(java.sql.Connection.class);
        java.sql.PreparedStatement mockPs = mock(java.sql.PreparedStatement.class);
        when(mockConnection.prepareStatement("SELECT pg_advisory_unlock(?)")).thenReturn(mockPs);
        ReflectionTestUtils.setField(config, "leaderConnection", mockConnection);

        config.destroy();

        verify(mockSession, times(1)).stop();
        verify(mockPs, times(1)).setLong(1, TelegramBotConfig.TELEGRAM_BOT_LEADER_LOCK_ID);
        verify(mockPs, times(1)).executeQuery();
        verify(mockConnection, times(1)).close();
        assertNull(config.getBotSession());
    }

    @Test
    void leaderGate_standbyClosesConnectionWhenLockNotAcquired() throws Exception {
        javax.sql.DataSource dataSource = mock(javax.sql.DataSource.class);
        java.sql.Connection mockConnection = mock(java.sql.Connection.class);
        java.sql.PreparedStatement mockPs = mock(java.sql.PreparedStatement.class);
        java.sql.ResultSet mockRs = mock(java.sql.ResultSet.class);

        when(dataSource.getConnection()).thenReturn(mockConnection);
        when(mockConnection.prepareStatement("SELECT pg_try_advisory_lock(?)")).thenReturn(mockPs);
        when(mockPs.executeQuery()).thenReturn(mockRs);
        when(mockRs.next()).thenReturn(true);
        when(mockRs.getBoolean(1)).thenReturn(false); // Lock held by another instance

        TelegramBotConfig config = new TelegramBotConfig(dataSource, telegramBotService);

        // tryAcquireLeaderAndRegisterBot ni chaqiramiz
        org.springframework.test.util.ReflectionTestUtils.invokeMethod(config, "tryAcquireLeaderAndRegisterBot");

        // Lock olinmadi -> Connection darhol yopilishi kerak (connection pool band qilinmasligi uchun)
        verify(mockConnection, times(1)).close();
        assertFalse(config.isLeader());
    }
}
