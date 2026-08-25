-- Least Privilege DB User Script for PostgreSQL
-- Run as superuser (postgres) on the taskcenter database

-- 1. Create application user with password
CREATE ROLE app_user LOGIN PASSWORD 'CHANGE_THIS_TO_STRONG_RANDOM_PASSWORD';

-- 2. Grant CONNECT permission on database
GRANT CONNECT ON DATABASE taskcenter TO app_user;

-- 3. Grant USAGE on schema
GRANT USAGE ON SCHEMA public TO app_user;

-- 4. Grant DML permissions on all tables (SELECT, INSERT, UPDATE, DELETE)
GRANT SELECT, INSERT, UPDATE, DELETE ON ALL TABLES IN SCHEMA public TO app_user;

-- 5. Grant USAGE on sequences (for auto-increment/serial columns)
GRANT USAGE, SELECT ON ALL SEQUENCES IN SCHEMA public TO app_user;

-- 6. Set default privileges for future tables
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT SELECT, INSERT, UPDATE, DELETE ON TABLES TO app_user;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT USAGE, SELECT ON SEQUENCES TO app_user;

-- 7. Verify grants
\dp

-- Note: app_user does NOT have DDL permissions (CREATE/DROP/ALTER TABLE)
-- DDL operations should be run by migration user (flyway_user) or admin
-- Flyway migrations should run as a separate user with DDL permissions