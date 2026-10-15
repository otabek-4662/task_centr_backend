-- V62: Fast functional indexes for case-insensitive user & workspace lookup
CREATE INDEX IF NOT EXISTS idx_users_lower_email ON users (LOWER(email));
CREATE INDEX IF NOT EXISTS idx_users_lower_name ON users (LOWER(name));
CREATE INDEX IF NOT EXISTS idx_workspaces_lower_name ON workspaces (LOWER(name)) WHERE deleted_at IS NULL;
CREATE INDEX IF NOT EXISTS idx_workspaces_upper_key_prefix ON workspaces (UPPER(key_prefix)) WHERE deleted_at IS NULL;
