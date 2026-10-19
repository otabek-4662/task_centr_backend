-- =============================================================================
-- Task Center Backend — Local DB Read-Only Agent Sandbox Setup
-- =============================================================================
-- Ushbu skript faqat LOKAL PostgreSQL (docker-compose yoki localhost) muhitida
-- Antigravity / AI Agent yoki tashqi audit vositalari uchun xavfsiz,
-- mutatsiyalardan (INSERT, UPDATE, DELETE, DROP, ALTER) to'liq himoyalangan
-- 'readonly_agent' foydalanuvchisini yaratish uchun mo'ljallangan.
--
-- XAVFSIZLIK OGOHLANTIRISHI:
-- 1. Ushbu skriptni HECH QACHON production (Render) bazasida ishga tushirmang!
-- 2. MCP va AI agentlarga faqat ushbu cheklangan foydalanuvchi ma'lumotlarini bering.
-- =============================================================================

DO $$
BEGIN
    -- 1. Rol mavjud bo'lmasa yaratamiz
    IF NOT EXISTS (SELECT FROM pg_catalog.pg_roles WHERE rolname = 'readonly_agent') THEN
        CREATE ROLE readonly_agent WITH LOGIN PASSWORD 'local_readonly_pass_123';
    ELSE
        ALTER ROLE readonly_agent WITH PASSWORD 'local_readonly_pass_123';
    END IF;
END $$;

-- 2. Tranzaksiyalarni sessiya darajasida faqat READ-ONLY qilish
ALTER ROLE readonly_agent SET default_transaction_read_only = on;

-- 3. Jadvallarga faqat CONNECT va SELECT huquqini berish
GRANT CONNECT ON DATABASE taskcenter TO readonly_agent;
GRANT USAGE ON SCHEMA public TO readonly_agent;
GRANT SELECT ON ALL TABLES IN SCHEMA public TO readonly_agent;
GRANT SELECT ON ALL SEQUENCES IN SCHEMA public TO readonly_agent;

-- 4. Kelajakda Flyway orqali yaratiladigan yangi jadvallar uchun ham faqat SELECT berish
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT SELECT ON TABLES TO readonly_agent;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT SELECT ON SEQUENCES TO readonly_agent;

-- 5. Barcha xavfli huquqlarni (CREATE, DROP, ALTER) qat'iy bekor qilish
REVOKE CREATE ON SCHEMA public FROM readonly_agent;

-- =============================================================================
-- Test / Tasdiqlash:
-- Quyidagi buyruq bilan ulanib tekshiring:
-- psql -U readonly_agent -d taskcenter -h localhost -p 5432
-- Test so'rovi:
--   SELECT count(*) FROM users;                --> Muvaffaqiyatli
--   CREATE TABLE test (id int);                --> ERROR: permission denied
--   DELETE FROM users WHERE id = 1;            --> ERROR: cannot execute DELETE in a read-only transaction
-- =============================================================================
