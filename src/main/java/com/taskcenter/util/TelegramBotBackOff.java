package com.taskcenter.util;

import lombok.extern.slf4j.Slf4j;
import org.telegram.telegrambots.meta.generics.BackOff;

/**
 * Telegram Long Polling xatolarida (ayniqsa [409] Conflict va tarmoq uzilishlarida)
 * tinimsiz crash loop bo'lmasligi uchun eksponentsial backoff kechikishi.
 */
@Slf4j
public class TelegramBotBackOff implements BackOff {

    private static final long INITIAL_INTERVAL_MS = 2000L;
    private static final long MAX_INTERVAL_MS = 60000L;
    private static final double MULTIPLIER = 1.5;

    private long currentIntervalMs = INITIAL_INTERVAL_MS;

    @Override
    public synchronized void reset() {
        this.currentIntervalMs = INITIAL_INTERVAL_MS;
    }

    @Override
    public synchronized long nextBackOffMillis() {
        long interval = this.currentIntervalMs;
        this.currentIntervalMs = Math.min(MAX_INTERVAL_MS, (long) (this.currentIntervalMs * MULTIPLIER));
        log.warn("Telegram bot polling backoff faollashdi: {} ms kutish (409 Conflict yoki tarmoq xatosi)", interval);
        return interval;
    }
}
