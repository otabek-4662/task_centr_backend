package com.taskcenter.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.Profiles;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.Statement;

@Component
public class DbMigration {

    private static final Logger log = LoggerFactory.getLogger(DbMigration.class);
    private final DataSource dataSource;
    private final ConfigurableEnvironment environment;

    public DbMigration(DataSource dataSource, ConfigurableEnvironment environment) {
        this.dataSource = dataSource;
        this.environment = environment;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void migrate() {
        if (!environment.acceptsProfiles(Profiles.of("!local"))) {
            log.info("Skipping migration for local profile");
            return;
        }

        String[] migrations = {
            "ALTER TABLE workspaces ADD COLUMN IF NOT EXISTS created_at TIMESTAMP",
            "ALTER TABLE workspaces ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP",
            "UPDATE workspaces SET created_at = NOW() WHERE created_at IS NULL",
            "UPDATE workspaces SET updated_at = NOW() WHERE updated_at IS NULL"
        };

        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {
            for (String sql : migrations) {
                stmt.execute(sql);
                log.info("Migration OK: {}", sql);
            }
            log.info("All migrations completed successfully");
        } catch (Exception e) {
            log.error("Migration FAILED: {}", e.getMessage());
        }
    }
}
