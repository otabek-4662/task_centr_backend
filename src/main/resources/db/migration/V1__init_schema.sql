-- Flyway baseline migration: initial schema
-- This is the baseline - creates all tables from scratch

-- Users table
CREATE TABLE users (
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
CREATE TABLE workspaces (
    id VARCHAR(255) PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    bg_color VARCHAR(255),
    description TEXT,
    owner_id VARCHAR(255) NOT NULL,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP(6)
);

-- Board columns table
CREATE TABLE board_columns (
    id VARCHAR(255) PRIMARY KEY,
    workspace_id VARCHAR(255) NOT NULL,
    title VARCHAR(255) NOT NULL,
    column_order INTEGER NOT NULL DEFAULT 0,
    is_default BOOLEAN NOT NULL DEFAULT FALSE
);

-- Labels table
CREATE TABLE labels (
    id VARCHAR(255) PRIMARY KEY,
    workspace_id VARCHAR(255) NOT NULL,
    name VARCHAR(255) NOT NULL,
    color VARCHAR(255) NOT NULL
);

-- Tasks table
CREATE TABLE tasks (
    id VARCHAR(255) PRIMARY KEY,
    public_id VARCHAR(255) NOT NULL UNIQUE,
    workspace_id VARCHAR(255) NOT NULL,
    column_id VARCHAR(255) NOT NULL,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    task_order INTEGER NOT NULL DEFAULT 0
);

-- Task labels (many-to-many)
CREATE TABLE task_labels (
    task_id VARCHAR(255) NOT NULL,
    label_id VARCHAR(255) NOT NULL,
    PRIMARY KEY (task_id, label_id)
);

-- Task assignees (many-to-many)
CREATE TABLE task_assignees (
    task_id VARCHAR(255) NOT NULL,
    user_id VARCHAR(255) NOT NULL,
    PRIMARY KEY (task_id, user_id)
);

-- Workspace members (many-to-many with role)
CREATE TABLE workspace_members (
    workspace_id VARCHAR(255) NOT NULL,
    user_id VARCHAR(255) NOT NULL,
    role VARCHAR(255) NOT NULL,
    PRIMARY KEY (workspace_id, user_id)
);

-- Foreign key constraints
ALTER TABLE workspaces ADD CONSTRAINT fk_workspaces_owner 
    FOREIGN KEY (owner_id) REFERENCES users(id);

ALTER TABLE board_columns ADD CONSTRAINT fk_columns_workspace 
    FOREIGN KEY (workspace_id) REFERENCES workspaces(id);

ALTER TABLE labels ADD CONSTRAINT fk_labels_workspace 
    FOREIGN KEY (workspace_id) REFERENCES workspaces(id);

ALTER TABLE tasks ADD CONSTRAINT fk_tasks_workspace 
    FOREIGN KEY (workspace_id) REFERENCES workspaces(id);

ALTER TABLE tasks ADD CONSTRAINT fk_tasks_column 
    FOREIGN KEY (column_id) REFERENCES board_columns(id);

ALTER TABLE task_labels ADD CONSTRAINT fk_task_labels_task 
    FOREIGN KEY (task_id) REFERENCES tasks(id);

ALTER TABLE task_labels ADD CONSTRAINT fk_task_labels_label 
    FOREIGN KEY (label_id) REFERENCES labels(id);

ALTER TABLE task_assignees ADD CONSTRAINT fk_task_assignees_task 
    FOREIGN KEY (task_id) REFERENCES tasks(id);

ALTER TABLE task_assignees ADD CONSTRAINT fk_task_assignees_user 
    FOREIGN KEY (user_id) REFERENCES users(id);

ALTER TABLE workspace_members ADD CONSTRAINT fk_ws_members_workspace 
    FOREIGN KEY (workspace_id) REFERENCES workspaces(id);

ALTER TABLE workspace_members ADD CONSTRAINT fk_ws_members_user 
    FOREIGN KEY (user_id) REFERENCES users(id);

-- Indexes for performance
CREATE INDEX idx_board_columns_workspace_order ON board_columns (workspace_id, column_order);
CREATE INDEX idx_tasks_workspace_order ON tasks (workspace_id, task_order);
CREATE INDEX idx_tasks_column_order ON tasks (column_id, task_order);
CREATE INDEX idx_tasks_public_id ON tasks (public_id) UNIQUE;
CREATE INDEX idx_workspaces_owner ON workspaces (owner_id);

-- Seed data (optional - can be removed for production)
-- This is the initial test workspace and users