-- V11: Sync local schema with live schema for key_prefix
-- Adds key_prefix column and unique constraint if they don't exist

ALTER TABLE workspaces ADD COLUMN IF NOT EXISTS key_prefix VARCHAR(255);
