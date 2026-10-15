package com.taskcenter.service;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Dedicated scheduled cleaner for TelegramBotService task creation states.
 * Keeps @Scheduled out of TelegramBotService to avoid interface proxy conflicts with LongPollingBot.
 */
@Component
public class TelegramTaskCreationStateCleaner {

    private final ObjectProvider<TelegramBotService> botServiceProvider;

    public TelegramTaskCreationStateCleaner(ObjectProvider<TelegramBotService> botServiceProvider) {
        this.botServiceProvider = botServiceProvider;
    }

    @Scheduled(fixedRate = 1800000, initialDelay = 60000)
    public void cleanup() {
        TelegramBotService bot = botServiceProvider.getIfAvailable();
        if (bot != null) {
            bot.cleanupTaskCreationStates();
        }
    }
}
