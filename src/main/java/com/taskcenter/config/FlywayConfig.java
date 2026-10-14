package com.taskcenter.config;

import org.flywaydb.core.api.exception.FlywayValidateException;
import org.springframework.boot.autoconfigure.flyway.FlywayMigrationStrategy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class FlywayConfig {

    @Bean
    public FlywayMigrationStrategy cleanMigrateStrategy() {
        return flyway -> {
            try {
                flyway.migrate();
            } catch (FlywayValidateException e) {
                // Faqat validatsiya yoki oldingi migratsiyada xato bo'lgandagina repair() chaqiriladi
                flyway.repair();
                flyway.migrate();
            }
        };
    }
}
