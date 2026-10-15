-- V62: Fast functional indexes for case-insensitive user & workspace lookup
CREATE INDEX IF NOT EXISTS idx_users_lower_email ON users (LOWER(email)) WHERE deleted_at IS NULL;
CREATE INDEX IF NOT EXISTS idx_users_lower_name ON users (LOWER(name)) WHERE deleted_at IS NULL;
CREATE INDEX IF NOT EXISTS idx_workspaces_lower_title ON workspaces (LOWER(title)) WHERE deleted_at IS NULL;
CREATE INDEX IF NOT EXISTS idx_workspaces_upper_key_prefix ON workspaces (UPPER(key_prefix)) WHERE deleted_at IS NULL;
