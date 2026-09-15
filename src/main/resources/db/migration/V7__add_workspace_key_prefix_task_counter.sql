-- V7: Add key_prefix and task_counter to workspaces for Jira-style publicId (e.g. TC-1, TC-2)

ALTER TABLE workspaces
    ADD COLUMN IF NOT EXISTS key_prefix VARCHAR(10) NOT NULL DEFAULT 'WS',
    ADD COLUMN IF NOT EXISTS task_counter INTEGER NOT NULL DEFAULT 0;

-- Backfill key_prefix for existing workspaces from their title:
-- Takes first letters of each word, up to 4 chars (e.g. "Task Center" -> "TC")
UPDATE workspaces
SET key_prefix = UPPER(
    CASE
        WHEN title ~ '\s'
        THEN REGEXP_REPLACE(
                REGEXP_REPLACE(title, '(\S)\S*\s*', '\1', 'g'),
                '\s', '', 'g'
             )
        ELSE SUBSTRING(title FROM 1 FOR 4)
    END
)
WHERE key_prefix = 'WS';

-- Trim to max 10 chars just in case
UPDATE workspaces SET key_prefix = SUBSTRING(key_prefix FROM 1 FOR 10);
