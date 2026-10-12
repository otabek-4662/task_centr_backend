package com.taskcenter.util;

import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;

/**
 * Umumiy vaqt yordamchisi (Shared Clock / AppTime):
 * - Saqlanadigan vaqtlar (stored timestamps) uchun: ZoneOffset.UTC
 * - Foydalanuvchiga ko'rinadigan kunlar (user-facing days) uchun: Asia/Tashkent (UTC+5)
 */
@Component
public class AppTime {

    public static final ZoneOffset STORAGE_ZONE = ZoneOffset.UTC;
    public static final ZoneId USER_ZONE = ZoneId.of("Asia/Tashkent");

    private static volatile Clock clock = Clock.systemUTC();

    public static void setClock(Clock newClock) {
        clock = newClock != null ? newClock : Clock.systemUTC();
    }

    public static void resetClock() {
        clock = Clock.systemUTC();
    }

    /**
     * Bazaga saqlanadigan vaqt (UTC).
     */
    public static LocalDateTime nowUtc() {
        return LocalDateTime.now(clock.withZone(STORAGE_ZONE));
    }

    /**
     * Bazaga saqlanadigan vaqt (nowUtc taxallusi).
     */
    public static LocalDateTime now() {
        return nowUtc();
    }

    /**
     * Joriy lahza (Instant).
     */
    public static Instant instant() {
        return clock.instant();
    }

    /**
     * Foydalanuvchi joriy sanasi (Asia/Tashkent bo'yicha LocalDate).
     */
    public static LocalDate today() {
        return LocalDate.now(clock.withZone(USER_ZONE));
    }

    /**
     * Foydalanuvchi joriy vaqti (Asia/Tashkent bo'yicha LocalDateTime).
     */
    public static LocalDateTime nowTashkent() {
        return LocalDateTime.now(clock.withZone(USER_ZONE));
    }

    public ZoneOffset getStorageZone() {
        return STORAGE_ZONE;
    }

    public ZoneId getUserZone() {
        return USER_ZONE;
    }
}
