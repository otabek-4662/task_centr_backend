package com.taskcenter.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DatabaseUrlNormalizerTest {

    @Test
    @DisplayName("1. Standard postgres:// URL is correctly converted to jdbc:postgresql://")
    void normalize_standardPostgresUrl() {
        String raw = "postgres://taskuser:secretpass@dpg-abc123xyz-a.ohio-postgres.render.com:5432/taskcenter";
        DatabaseUrlNormalizer.NormalizedConfig config = DatabaseUrlNormalizer.normalize(raw, "fallbackUser", "fallbackPass");

        assertThat(config.jdbcUrl()).isEqualTo("jdbc:postgresql://dpg-abc123xyz-a.ohio-postgres.render.com:5432/taskcenter");
        assertThat(config.username()).isEqualTo("taskuser");
        assertThat(config.password()).isEqualTo("secretpass");
        assertThat(config.host()).isEqualTo("dpg-abc123xyz-a.ohio-postgres.render.com:5432");
        assertThat(config.dbName()).isEqualTo("taskcenter");
    }

    @Test
    @DisplayName("2. postgresql:// URL with query parameters preserves query parameters")
    void normalize_postgresqlUrlWithQueryParams() {
        String raw = "postgresql://myuser:mypass@localhost:5432/mydb?sslmode=require&prepareThreshold=0";
        DatabaseUrlNormalizer.NormalizedConfig config = DatabaseUrlNormalizer.normalize(raw, null, null);

        assertThat(config.jdbcUrl()).isEqualTo("jdbc:postgresql://localhost:5432/mydb?sslmode=require&prepareThreshold=0");
        assertThat(config.username()).isEqualTo("myuser");
        assertThat(config.password()).isEqualTo("mypass");
        assertThat(config.dbName()).isEqualTo("mydb");
    }

    @Test
    @DisplayName("3. Already formatted jdbc: URL remains untouched")
    void normalize_alreadyJdbcUrl_preserved() {
        String raw = "jdbc:postgresql://localhost:5432/taskcenter";
        DatabaseUrlNormalizer.NormalizedConfig config = DatabaseUrlNormalizer.normalize(raw, "postgres", "password");

        assertThat(config.jdbcUrl()).isEqualTo("jdbc:postgresql://localhost:5432/taskcenter");
        assertThat(config.username()).isEqualTo("postgres");
        assertThat(config.password()).isEqualTo("password");
    }

    @Test
    @DisplayName("4. URL encoded characters in user/password are safely decoded")
    void normalize_urlEncodedCredentials() {
        String raw = "postgres://user%40company.com:p%40ss%3Aword@pgserver:5432/appdb";
        DatabaseUrlNormalizer.NormalizedConfig config = DatabaseUrlNormalizer.normalize(raw, null, null);

        assertThat(config.jdbcUrl()).isEqualTo("jdbc:postgresql://pgserver:5432/appdb");
        assertThat(config.username()).isEqualTo("user@company.com");
        assertThat(config.password()).isEqualTo("p@ss:word");
    }

    @Test
    @DisplayName("5. Null or empty URL returns safe fallbacks")
    void normalize_nullOrEmpty() {
        DatabaseUrlNormalizer.NormalizedConfig nullConfig = DatabaseUrlNormalizer.normalize(null, "u", "p");
        assertThat(nullConfig.jdbcUrl()).isNull();
        assertThat(nullConfig.username()).isEqualTo("u");

        DatabaseUrlNormalizer.NormalizedConfig blankConfig = DatabaseUrlNormalizer.normalize("   ", "u", "p");
        assertThat(blankConfig.jdbcUrl()).isEqualTo("   ");
        assertThat(blankConfig.username()).isEqualTo("u");
    }
}
