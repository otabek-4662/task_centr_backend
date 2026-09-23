-- V11: Sync local schema with live schema for key_prefix
-- Adds key_prefix column and unique constraint if they don't exist

ALTER TABLE workspaces ADD COLUMN IF NOT EXISTS key_prefix VARCHAR(255);

DO $$ 
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'uk_workspace_key_prefix') THEN
        ALTER TABLE workspaces ADD CONSTRAINT uk_workspace_key_prefix UNIQUE (key_prefix);
    END IF;
END $$;
