package com.taskcenter.service;

import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;

public record TelegramMessageEvent(Long chatId, String message, InlineKeyboardMarkup keyboard) {
    public TelegramMessageEvent(Long chatId, String message) {
        this(chatId, message, null);
    }
}
