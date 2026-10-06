package com.taskcenter.config;

import com.taskcenter.service.TelegramBotService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.api.methods.updates.SetWebhook;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

@Slf4j
@Configuration
@ConditionalOnProperty(name = "telegram.bot.token")
public class TelegramBotConfig {

    @Value("${telegram.bot.webhook-url:}")
    private String webhookUrl;

    @Value("${telegram.bot.webhook-secret:}")
    private String webhookSecret;

    @Bean
    public TelegramBotsApi telegramBotsApi(TelegramBotService telegramBotService) {
        if (webhookUrl != null && !webhookUrl.isBlank()) {
            log.info("Telegram Bot Webhook rejimida: {}. Long-polling (DefaultBotSession) o'chirildi (409 Conflict bartaraf etildi).", webhookUrl);
            try {
                SetWebhook.SetWebhookBuilder builder = SetWebhook.builder().url(webhookUrl.trim());
                if (webhookSecret != null && !webhookSecret.isBlank()) {
                    builder.secretToken(webhookSecret.trim());
                }
                telegramBotService.execute(builder.build());
                log.info("Telegram Bot Webhook muvaffaqiyatli o'rnatildi.");
            } catch (TelegramApiException e) {
                log.warn("Telegram Webhook o'rnatishda xatolik: {}", e.getMessage());
            }
            return null;
        }

        try {
            TelegramBotsApi botsApi = new TelegramBotsApi(DefaultBotSession.class);
            botsApi.registerBot(telegramBotService);
            log.info("Telegram Bot Long-Polling rejimida muvaffaqiyatli ro'yxatdan o'tkazildi: @{}", telegramBotService.getBotUsername());
            return botsApi;
        } catch (TelegramApiException e) {
            log.warn("Telegram botni ro'yxatdan o'tkazishda xatolik (ehtimol allaqachon ro'yxatdan o'tgan): {}", e.getMessage());
            return null;
        }
    }
}
