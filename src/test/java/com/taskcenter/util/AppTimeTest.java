package com.taskcenter.util;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;

class AppTimeTest {

    @AfterEach
    void tearDown() {
        AppTime.resetClock();
    }

    @Test
    @DisplayName("nowUtc bazaga saqlanadigan vaqtni UTC da qaytarishi kerak")
    void nowUtc_returnsUtcDateTime() {
        Instant fixedInstant = Instant.parse("2026-10-11T12:00:00Z");
        AppTime.setClock(Clock.fixed(fixedInstant, ZoneOffset.UTC));

        LocalDateTime utcNow = AppTime.nowUtc();
        assertThat(utcNow).isEqualTo(LocalDateTime.of(2026, 10, 11, 12, 0, 0));
    }

    @Test
    @DisplayName("today foydalanuvchiga ko'rinadigan kunni Asia/Tashkent (UTC+5) bo'yicha qaytarishi kerak")
    void today_returnsTashkentDate() {
        // UTC 20:30 on 2026-10-11 is 01:30 on 2026-10-12 in Tashkent (UTC+5)
        Instant nightInstant = Instant.parse("2026-10-11T20:30:00Z");
        AppTime.setClock(Clock.fixed(nightInstant, ZoneOffset.UTC));

        LocalDate tashkentDate = AppTime.today();
        assertThat(tashkentDate).isEqualTo(LocalDate.of(2026, 10, 12));
    }
}
