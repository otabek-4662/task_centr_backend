-- V8: Add unique constraint to workspaces.key_prefix
DO $$
DECLARE
    duplicate_count INTEGER;
BEGIN
    SELECT COUNT(*) INTO duplicate_count
    FROM (
        SELECT key_prefix
        FROM workspaces
        GROUP BY key_prefix
        HAVING COUNT(*) > 1
    ) dup;
    IF duplicate_count > 0 THEN
        RAISE EXCEPTION 'Cannot add unique constraint on key_prefix because there are duplicate values. Please resolve the duplicates first.';
    END IF;
END $$;

ALTER TABLE workspaces ADD CONSTRAINT uk_workspace_key_prefix UNIQUE (key_prefix);