package com.taskcenter.service;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

import static org.assertj.core.api.Assertions.assertThat;

class TaskDateBoundaryTest {

    @Test
    void testTashkent0100Boundary() {
        // UTC 20:00 is exactly 01:00 in Asia/Tashkent on the next day
        Instant utc20 = Instant.parse("2023-10-10T20:00:00Z");
        Clock clock = Clock.fixed(utc20, ZoneId.of("Asia/Tashkent"));
        
        LocalDate tashkentDate = LocalDate.now(clock);
        
        // Assert that the date has rolled over to the next day due to the +5 timezone
        assertThat(tashkentDate).isEqualTo(LocalDate.of(2023, 10, 11));
    }
}
