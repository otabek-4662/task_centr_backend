package com.taskcenter.controller;

import com.taskcenter.service.TelegramBotService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.telegram.telegrambots.meta.api.objects.Update;

@Slf4j
@Tag(name = "Telegram Webhook", description = "Telegram Bot webhook so'rovlarini qabul qilish")
@RestController
@RequestMapping("/api/webhooks/telegram")
@ConditionalOnBean(TelegramBotService.class)
public class TelegramWebhookController {

    private final TelegramBotService telegramBotService;
    private final String expectedSecret;

    public TelegramWebhookController(
            TelegramBotService telegramBotService,
            @Value("${telegram.bot.webhook-secret:}") String expectedSecret) {
        this.telegramBotService = telegramBotService;
        this.expectedSecret = expectedSecret;
    }

    @Operation(summary = "Telegram Update qabul qilish", description = "Telegram Bot API dan kelgan webhook updates ni qayta ishlash")
    @PostMapping
    public ResponseEntity<Void> handleTelegramUpdate(
            @RequestHeader(value = "X-Telegram-Bot-Api-Secret-Token", required = false) String secretToken,
            @RequestBody Update update) {

        if (expectedSecret != null && !expectedSecret.isBlank()) {
            if (secretToken == null || !expectedSecret.trim().equals(secretToken.trim())) {
                log.warn("Telegram Webhook xavfsizlik xatosi: noto'g'ri secret token");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }
        }

        try {
            if (update != null) {
                telegramBotService.onUpdateReceived(update);
            }
        } catch (Exception e) {
            log.error("Telegram Webhook update qayta ishlashda xatolik: {}", e.getMessage(), e);
        }

        return ResponseEntity.ok().build();
    }
}
