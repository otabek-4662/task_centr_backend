-- V59: Enable pg_trgm extension (if permitted) and add fast GIN trigram search indexes for tasks
DO $$
BEGIN
    CREATE EXTENSION IF NOT EXISTS pg_trgm;
EXCEPTION
    WHEN OTHERS THEN
        RAISE NOTICE 'pg_trgm extension could not be created (insufficient privileges), skipping GIN trigram indexes';
END
$$;

DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM pg_extension WHERE extname = 'pg_trgm') THEN
        EXECUTE 'CREATE INDEX IF NOT EXISTS idx_tasks_title_trgm ON tasks USING gin (LOWER(title) gin_trgm_ops) WHERE deleted_at IS NULL';
        EXECUTE 'CREATE INDEX IF NOT EXISTS idx_tasks_public_id_trgm ON tasks USING gin (LOWER(public_id) gin_trgm_ops) WHERE deleted_at IS NULL';
    END IF;
END
$$;
