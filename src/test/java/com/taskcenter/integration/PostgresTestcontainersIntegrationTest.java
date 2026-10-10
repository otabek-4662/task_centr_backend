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
        boolean isCi = "true".equalsIgnoreCase(System.getenv("CI")) || "true".equalsIgnoreCase(System.getProperty("CI"));
        try {
            boolean available = DockerClientFactory.instance().isDockerAvailable();
            if (!available && isCi) {
                org.junit.jupiter.api.Assertions.fail("CI muhitida (CI=true) Docker mavjud bo'lishi shart, lekin Testcontainers Docker'ni topa olmadi");
            }
            return available;
        } catch (org.opentest4j.AssertionFailedError afe) {
            throw afe;
        } catch (Throwable t) {
            if (isCi) {
                org.junit.jupiter.api.Assertions.fail("CI muhitida (CI=true) Docker mavjud bo'lishi shart, lekin xatolik yuz berdi: " + t.getMessage());
            }
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

    @Test
    @DisplayName("PostgreSQL Testcontainers: V53/V54 migratsiyalari va workspace_invitations EXPIRED status CHECK cheklovi tekshiruvi")
    void testV53V54MigrationsAndStatusConstraintInPostgres() {
        assertTrue(postgres.isRunning(), "PostgreSQL konteyneri ishlab turishi kerak");

        // 1. V53 migratsiyasi ustunlari mavjudligini tekshirish
        Integer typeCol = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.columns WHERE table_name = 'workspace_invitations' AND column_name = 'type'", Integer.class);
        assertEquals(1, typeCol, "workspace_invitations.type ustuni mavjud bo'lishi kerak");

        Integer tokenHashCol = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.columns WHERE table_name = 'workspace_invitations' AND column_name = 'token_hash'", Integer.class);
        assertEquals(1, tokenHashCol, "workspace_invitations.token_hash ustuni mavjud bo'lishi kerak");

        Integer maxUsesCol = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.columns WHERE table_name = 'workspace_invitations' AND column_name = 'max_uses'", Integer.class);
        assertEquals(1, maxUsesCol, "workspace_invitations.max_uses ustuni mavjud bo'lishi kerak");

        Integer useCountCol = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.columns WHERE table_name = 'workspace_invitations' AND column_name = 'use_count'", Integer.class);
        assertEquals(1, useCountCol, "workspace_invitations.use_count ustuni mavjud bo'lishi kerak");

        Integer deliveryCol = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.columns WHERE table_name = 'workspace_invitations' AND column_name = 'email_delivery_status'", Integer.class);
        assertEquals(1, deliveryCol, "workspace_invitations.email_delivery_status ustuni mavjud bo'lishi kerak");

        // 2. V54 migratsiyasi: telegram_pending_chat_invites jadvali
        Integer pendingChatTable = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.tables WHERE table_name = 'telegram_pending_chat_invites'", Integer.class);
        assertEquals(1, pendingChatTable, "telegram_pending_chat_invites jadvali mavjud bo'lishi kerak");

        // 3. V37 / V54 dagi status ustunida EXPIRED qiymatini qabul qilishini tekshirish
        String userId = UUID.randomUUID().toString();
        String wsId = UUID.randomUUID().toString();
        String invId = UUID.randomUUID().toString();

        jdbcTemplate.update("INSERT INTO users (id, name, full_name, password, role) VALUES (?, ?, ?, ?, ?)",
                userId, "u_" + invId.substring(0, 8), "User " + invId.substring(0, 4), "hash", "USER");
        jdbcTemplate.update("INSERT INTO workspaces (id, title, owner_id) VALUES (?, ?, ?)",
                wsId, "Integration Workspace", userId);

        // EXPIRED statusi bilan to'g'ridan-to'g'ri INSERT qilish — hech qanday CHECK cheklovi to'sqinlik qilmasligi kerak
        assertDoesNotThrow(() -> {
            jdbcTemplate.update(
                    "INSERT INTO workspace_invitations (id, workspace_id, sender_id, role, status, expires_at, type, token_hash, use_count, email_delivery_status) " +
                    "VALUES (?, ?, ?, ?, 'EXPIRED', NOW() - INTERVAL '1 day', 'LINK', ?, 0, 'NOT_APPLICABLE')",
                    invId, wsId, userId, "MEMBER", "hash_" + invId);
        }, "Real PostgreSQL da EXPIRED statusi to'siqsiz qabul qilinishi kerak");

        // Mavjud PENDING taklifni EXPIRED ga UPDATE qilish (V54 logikasi)
        String invPendingId = UUID.randomUUID().toString();
        jdbcTemplate.update(
                "INSERT INTO workspace_invitations (id, workspace_id, sender_id, role, status, expires_at, type, token_hash, use_count, email_delivery_status) " +
                "VALUES (?, ?, ?, ?, 'PENDING', NOW() - INTERVAL '2 hours', 'LINK', ?, 0, 'NOT_APPLICABLE')",
                invPendingId, wsId, userId, "MEMBER", "hash_" + invPendingId);

        int updated = jdbcTemplate.update(
                "UPDATE workspace_invitations SET status = 'EXPIRED' WHERE id = ? AND expires_at < NOW()", invPendingId);
        assertEquals(1, updated, "Eski taklif statusi EXPIRED ga muvaffaqiyatli o'zgarishi kerak");

        // 4. Atomik use_count oshirish operatsiyasi real PostgreSQL da to'g'ri ishlashi
        String invLinkId = UUID.randomUUID().toString();
        jdbcTemplate.update(
                "INSERT INTO workspace_invitations (id, workspace_id, sender_id, role, status, expires_at, type, token_hash, max_uses, use_count, email_delivery_status) " +
                "VALUES (?, ?, ?, ?, 'PENDING', NOW() + INTERVAL '7 days', 'LINK', ?, 3, 0, 'NOT_APPLICABLE')",
                invLinkId, wsId, userId, "MEMBER", "hash_" + invLinkId);

        int row1 = jdbcTemplate.update(
                "UPDATE workspace_invitations SET use_count = use_count + 1 WHERE id = ? AND use_count < max_uses", invLinkId);
        int row2 = jdbcTemplate.update(
                "UPDATE workspace_invitations SET use_count = use_count + 1 WHERE id = ? AND use_count < max_uses", invLinkId);
        int row3 = jdbcTemplate.update(
                "UPDATE workspace_invitations SET use_count = use_count + 1 WHERE id = ? AND use_count < max_uses", invLinkId);
        int row4 = jdbcTemplate.update(
                "UPDATE workspace_invitations SET use_count = use_count + 1 WHERE id = ? AND use_count < max_uses", invLinkId);

        assertEquals(1, row1);
        assertEquals(1, row2);
        assertEquals(1, row3);
        assertEquals(0, row4, "Limit (3) ga yetganda 4-yangilash 0 qaytarishi kerak");

        Integer finalUseCount = jdbcTemplate.queryForObject(
                "SELECT use_count FROM workspace_invitations WHERE id = ?", Integer.class, invLinkId);
        assertEquals(3, finalUseCount);

        // 5. V55 migratsiyasi: workspace_members da (workspace_id, user_id) unique/primary key cheklovi mavjudligini tekshirish
        Integer memberConstraintCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM pg_constraint WHERE conrelid = 'workspace_members'::regclass AND contype IN ('u', 'p')", Integer.class);
        assertTrue(memberConstraintCount != null && memberConstraintCount >= 1, "workspace_members da (workspace_id, user_id) bo'yicha cheklov bo'lishi kerak");
    }
}
