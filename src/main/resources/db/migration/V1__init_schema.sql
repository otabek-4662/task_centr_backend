-- Flyway baseline migration: initial schema
-- Idempotent: safe to run against an existing (pre-Flyway) schema.

-- Users table
CREATE TABLE IF NOT EXISTS users (
    id VARCHAR(255) PRIMARY KEY,
    name VARCHAR(255) NOT NULL UNIQUE,
    full_name VARCHAR(255),
    email VARCHAR(255) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    role VARCHAR(255) NOT NULL DEFAULT 'USER' CHECK (role IN ('USER', 'ADMIN')),
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP(6)
);

-- Workspaces table
CREATE TABLE IF NOT EXISTS workspaces (
    id VARCHAR(255) PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    bg_color VARCHAR(255),
    description TEXT,
    owner_id VARCHAR(255) NOT NULL,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP(6)
);

-- Board columns table
CREATE TABLE IF NOT EXISTS board_columns (
    id VARCHAR(255) PRIMARY KEY,
    workspace_id VARCHAR(255) NOT NULL,
    title VARCHAR(255) NOT NULL,
    column_order INTEGER NOT NULL DEFAULT 0,
    is_default BOOLEAN NOT NULL DEFAULT FALSE
);

-- Labels table
CREATE TABLE IF NOT EXISTS labels (
    id VARCHAR(255) PRIMARY KEY,
    workspace_id VARCHAR(255) NOT NULL,
    name VARCHAR(255) NOT NULL,
    color VARCHAR(255) NOT NULL
);

-- Tasks table
CREATE TABLE IF NOT EXISTS tasks (
    id VARCHAR(255) PRIMARY KEY,
    public_id VARCHAR(255) NOT NULL UNIQUE,
    workspace_id VARCHAR(255) NOT NULL,
    column_id VARCHAR(255) NOT NULL,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    task_order INTEGER NOT NULL DEFAULT 0
);

-- Task labels (many-to-many)
CREATE TABLE IF NOT EXISTS task_labels (
    task_id VARCHAR(255) NOT NULL,
    label_id VARCHAR(255) NOT NULL,
    PRIMARY KEY (task_id, label_id)
);

-- Task assignees (many-to-many)
CREATE TABLE IF NOT EXISTS task_assignees (
    task_id VARCHAR(255) NOT NULL,
    user_id VARCHAR(255) NOT NULL,
    PRIMARY KEY (task_id, user_id)
);

-- Workspace members (many-to-many with role)
CREATE TABLE IF NOT EXISTS workspace_members (
    workspace_id VARCHAR(255) NOT NULL,
    user_id VARCHAR(255) NOT NULL,
    role VARCHAR(255) NOT NULL,
    PRIMARY KEY (workspace_id, user_id)
);

-- Cleanup orphan data before adding constraints
DELETE FROM task_assignees WHERE task_id NOT IN (SELECT id FROM tasks) OR user_id NOT IN (SELECT id FROM users);
DELETE FROM task_labels WHERE task_id NOT IN (SELECT id FROM tasks) OR label_id NOT IN (SELECT id FROM labels);
DELETE FROM tasks WHERE workspace_id NOT IN (SELECT id FROM workspaces) OR column_id NOT IN (SELECT id FROM board_columns);
DELETE FROM labels WHERE workspace_id NOT IN (SELECT id FROM workspaces);
DELETE FROM board_columns WHERE workspace_id NOT IN (SELECT id FROM workspaces);
DELETE FROM workspace_members WHERE workspace_id NOT IN (SELECT id FROM workspaces) OR user_id NOT IN (SELECT id FROM users);
DELETE FROM workspaces WHERE owner_id NOT IN (SELECT id FROM users);

-- Foreign key constraints
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_workspaces_owner') THEN
        ALTER TABLE workspaces ADD CONSTRAINT fk_workspaces_owner FOREIGN KEY (owner_id) REFERENCES users(id);
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_columns_workspace') THEN
        ALTER TABLE board_columns ADD CONSTRAINT fk_columns_workspace FOREIGN KEY (workspace_id) REFERENCES workspaces(id);
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_labels_workspace') THEN
        ALTER TABLE labels ADD CONSTRAINT fk_labels_workspace FOREIGN KEY (workspace_id) REFERENCES workspaces(id);
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_tasks_workspace') THEN
        ALTER TABLE tasks ADD CONSTRAINT fk_tasks_workspace FOREIGN KEY (workspace_id) REFERENCES workspaces(id);
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_tasks_column') THEN
        ALTER TABLE tasks ADD CONSTRAINT fk_tasks_column FOREIGN KEY (column_id) REFERENCES board_columns(id);
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_task_labels_task') THEN
        ALTER TABLE task_labels ADD CONSTRAINT fk_task_labels_task FOREIGN KEY (task_id) REFERENCES tasks(id);
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_task_labels_label') THEN
        ALTER TABLE task_labels ADD CONSTRAINT fk_task_labels_label FOREIGN KEY (label_id) REFERENCES labels(id);
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_task_assignees_task') THEN
        ALTER TABLE task_assignees ADD CONSTRAINT fk_task_assignees_task FOREIGN KEY (task_id) REFERENCES tasks(id);
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_task_assignees_user') THEN
        ALTER TABLE task_assignees ADD CONSTRAINT fk_task_assignees_user FOREIGN KEY (user_id) REFERENCES users(id);
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_ws_members_workspace') THEN
        ALTER TABLE workspace_members ADD CONSTRAINT fk_ws_members_workspace FOREIGN KEY (workspace_id) REFERENCES workspaces(id);
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_ws_members_user') THEN
        ALTER TABLE workspace_members ADD CONSTRAINT fk_ws_members_user FOREIGN KEY (user_id) REFERENCES users(id);
    END IF;
END $$;

-- Indexes for performance
CREATE INDEX IF NOT EXISTS idx_board_columns_workspace_order ON board_columns (workspace_id, column_order);
CREATE INDEX IF NOT EXISTS idx_tasks_workspace_order ON tasks (workspace_id, task_order);
CREATE INDEX IF NOT EXISTS idx_tasks_column_order ON tasks (column_id, task_order);
CREATE UNIQUE INDEX IF NOT EXISTS idx_tasks_public_id ON tasks (public_id);
CREATE INDEX IF NOT EXISTS idx_workspaces_owner ON workspaces (owner_id);