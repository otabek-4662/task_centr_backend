package com.taskcenter.config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.Statement;

@Component
public class DbMigration implements CommandLineRunner {

    private final DataSource dataSource;

    public DbMigration(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public void run(String... args) throws Exception {
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute("ALTER TABLE workspaces ADD COLUMN IF NOT EXISTS created_at TIMESTAMP");
            stmt.execute("ALTER TABLE workspaces ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP");
            stmt.execute("UPDATE workspaces SET created_at = NOW() WHERE created_at IS NULL");
            stmt.execute("UPDATE workspaces SET updated_at = NOW() WHERE updated_at IS NULL");
        }
    }
}
