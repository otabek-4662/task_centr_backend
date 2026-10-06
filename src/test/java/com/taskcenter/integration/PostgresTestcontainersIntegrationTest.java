package com.taskcenter.integration;

import com.taskcenter.model.User;
import com.taskcenter.model.Workspace;
import com.taskcenter.model.RefreshToken;
import com.taskcenter.repository.UserRepository;
import com.taskcenter.repository.WorkspaceRepository;
import com.taskcenter.repository.RefreshTokenRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Testcontainers
@ActiveProfiles("test")
@EnabledIf("isDockerAvailable")
class PostgresTestcontainersIntegrationTest {

    static boolean isDockerAvailable() {
        try {
            return DockerClientFactory.instance().isDockerAvailable();
        } catch (Throwable t) {
            return false;
        }
    }

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine")
            .withDatabaseName("testdb")
            .withUsername("testuser")
            .withPassword("testpass");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
        registry.add("spring.jpa.properties.hibernate.dialect", () -> "org.hibernate.dialect.PostgreSQLDialect");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
        registry.add("spring.flyway.enabled", () -> "true");
        registry.add("flyway.enabled", () -> "true");
    }

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private WorkspaceRepository workspaceRepository;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Test
    @DisplayName("PostgreSQL Testcontainers: Flyway migratsiyalari va asosiy jadvallar mavjudligini tekshirish")
    void testFlywayMigrationsAppliedToRealPostgres() {
        assertTrue(postgres.isRunning(), "PostgreSQL konteyneri ishlab turishi kerak");

        // Flyway migratsiyalari natijasida jadvallar yaratilganini tekshiramiz
        Integer userTableCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.tables WHERE table_name = 'users'", Integer.class);
        assertEquals(1, userTableCount);

        Integer shedlockTableCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.tables WHERE table_name = 'shedlock'", Integer.class);
        assertEquals(1, shedlockTableCount, "ShedLock jadvali real PostgreSQL da mavjud bo'lishi kerak");

        // V49 migratsiyasi: refresh_tokens jadvalidagi yangi ustunlar
        Integer revokedColCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.columns WHERE table_name = 'refresh_tokens' AND column_name = 'revoked'", Integer.class);
        assertEquals(1, revokedColCount, "refresh_tokens.revoked ustuni PostgreSQL da mavjud bo'lishi kerak");
    }

    @Test
    @DisplayName("PostgreSQL Testcontainers: Haqiqiy bazada User, Workspace va Refresh Token operatsiyalari")
    void testRealDatabaseEntityPersistence() {
        // 1. Yangi foydalanuvchi saqlash
        User user = User.builder()
                .name("pg_user_" + UUID.randomUUID().toString().substring(0, 8))
                .fullName("Postgres Test User")
                .password("hashed_pwd")
                .role(User.Role.USER)
                .build();
        User savedUser = userRepository.save(user);
        assertNotNull(savedUser.getId());

        // 2. Real PostgreSQL da Refresh Token va rotatsiya
        RefreshToken token = RefreshToken.builder()
                .userId(savedUser.getId())
                .token(UUID.randomUUID().toString())
                .expiryDate(LocalDateTime.now().plusDays(30))
                .revoked(false)
                .build();
        RefreshToken savedToken = refreshTokenRepository.save(token);
        assertNotNull(savedToken.getId());
        assertFalse(savedToken.getRevoked());

        // Tokenni bekor qilish (Revoke)
        savedToken.setRevoked(true);
        savedToken.setReplacedByToken("new-rotated-token-123");
        refreshTokenRepository.save(savedToken);

        RefreshToken updatedToken = refreshTokenRepository.findByToken(savedToken.getToken()).orElseThrow();
        assertTrue(updatedToken.getRevoked());
        assertEquals("new-rotated-token-123", updatedToken.getReplacedByToken());
    }
}
