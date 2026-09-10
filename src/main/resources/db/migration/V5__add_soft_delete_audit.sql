-- V5: Add audit trail and soft-delete columns
-- Enables data recovery (soft delete) and audit logging

-- users
ALTER TABLE users
    ADD COLUMN IF NOT EXISTS created_by VARCHAR(255),
    ADD COLUMN IF NOT EXISTS updated_by VARCHAR(255),
    ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMP(6);

-- workspaces
ALTER TABLE workspaces
    ADD COLUMN IF NOT EXISTS created_by VARCHAR(255),
    ADD COLUMN IF NOT EXISTS updated_by VARCHAR(255),
    ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMP(6);

-- board_columns
ALTER TABLE board_columns
    ADD COLUMN IF NOT EXISTS created_by VARCHAR(255),
    ADD COLUMN IF NOT EXISTS updated_by VARCHAR(255),
    ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMP(6);
ALTER TABLE board_columns ADD COLUMN IF NOT EXISTS created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE board_columns ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP(6);

-- labels
ALTER TABLE labels
    ADD COLUMN IF NOT EXISTS created_by VARCHAR(255),
    ADD COLUMN IF NOT EXISTS updated_by VARCHAR(255),
    ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMP(6);
ALTER TABLE labels ADD COLUMN IF NOT EXISTS created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE labels ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP(6);

-- tasks
ALTER TABLE tasks
    ADD COLUMN IF NOT EXISTS created_by VARCHAR(255),
    ADD COLUMN IF NOT EXISTS updated_by VARCHAR(255),
    ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMP(6);
ALTER TABLE tasks ADD COLUMN IF NOT EXISTS created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE tasks ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP(6);

-- Indexes to speed up soft-delete filtering
CREATE INDEX IF NOT EXISTS idx_users_deleted_at ON users(deleted_at);
CREATE INDEX IF NOT EXISTS idx_workspaces_deleted_at ON workspaces(deleted_at);
CREATE INDEX IF NOT EXISTS idx_board_columns_deleted_at ON board_columns(deleted_at);
CREATE INDEX IF NOT EXISTS idx_labels_deleted_at ON labels(deleted_at);
CREATE INDEX IF NOT EXISTS idx_tasks_deleted_at ON tasks(deleted_at);