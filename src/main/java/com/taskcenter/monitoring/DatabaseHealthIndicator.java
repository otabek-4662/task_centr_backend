package com.taskcenter.monitoring;

import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicBoolean;

@Component
public class DatabaseHealthIndicator implements HealthIndicator {

    private static final AtomicBoolean DB_DOWN_ALERTED = new AtomicBoolean(false);

    private final JdbcTemplate jdbcTemplate;

    public DatabaseHealthIndicator(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public Health health() {
        try {
            Integer result = jdbcTemplate.queryForObject("SELECT 1", Integer.class);
            if (result != null && result == 1) {
                if (DB_DOWN_ALERTED.get()) {
                    System.out.println("DB UP: database health restored");
                    DB_DOWN_ALERTED.set(false);
                }
                return Health.up().withDetail("database", "PostgreSQL")
                        .withDetail("status", "up")
                        .build();
            }
            return Health.down().withDetail("database", "PostgreSQL")
                    .withDetail("status", "unexpected result")
                    .build();
        } catch (Exception e) {
            if (DB_DOWN_ALERTED.compareAndSet(false, true)) {
                System.err.println("DB DOWN ALERT: database health check failed: " + e.getMessage());
            }
            return Health.down(e).withDetail("database", "PostgreSQL")
                    .withDetail("status", "down")
                    .build();
        }
    }
}