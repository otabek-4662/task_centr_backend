package com.taskcenter.config;

import com.taskcenter.service.TelegramBotService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

@Slf4j
@Configuration
@ConditionalOnProperty(name = "telegram.bot.token")
public class TelegramBotConfig {

    @Bean
    public TelegramBotsApi telegramBotsApi(TelegramBotService telegramBotService) {
        try {
            TelegramBotsApi botsApi = new TelegramBotsApi(DefaultBotSession.class);
            botsApi.registerBot(telegramBotService);
            log.info("Telegram Bot muvaffaqiyatli ro'yxatdan o'tkazildi: @{}", telegramBotService.getBotUsername());
            return botsApi;
        } catch (TelegramApiException e) {
            log.warn("Telegram botni ro'yxatdan o'tkazishda xatolik (ehtimol allaqachon ro'yxatdan o'tgan): {}", e.getMessage());
            return null;
        }
    }
}
