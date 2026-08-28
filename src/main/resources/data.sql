ALTER TABLE workspaces ADD COLUMN IF NOT EXISTS created_at TIMESTAMP;
ALTER TABLE workspaces ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP;
UPDATE workspaces SET created_at = NOW() WHERE created_at IS NULL;
UPDATE workspaces SET updated_at = NOW() WHERE updated_at IS NULL;
