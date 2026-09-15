-- V6: Add optimistic locking version columns
-- Prevents lost updates on concurrent modification

ALTER TABLE tasks ADD COLUMN IF NOT EXISTS version BIGINT NOT NULL DEFAULT 0;
ALTER TABLE board_columns ADD COLUMN IF NOT EXISTS version BIGINT NOT NULL DEFAULT 0;
ALTER TABLE workspaces ADD COLUMN IF NOT EXISTS version BIGINT NOT NULL DEFAULT 0;
ALTER TABLE labels ADD COLUMN IF NOT EXISTS version BIGINT NOT NULL DEFAULT 0;