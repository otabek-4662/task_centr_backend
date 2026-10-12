-- ============================================================================
-- FLYWAY POSTGRESQL MIGRATION: BOARDS (WORKSPACES), COLUMNS, TASKS
-- Description: PostgreSQL uchun jadvallar, cheklovlar, LexoRank va indekslar.
-- ============================================================================

-- 1. BOARDS / WORKSPACES TABLE
CREATE TABLE IF NOT EXISTS workspaces (
    id VARCHAR(255) PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    bg_color VARCHAR(255),
    key_prefix VARCHAR(32) UNIQUE,
    description TEXT,
    owner_id VARCHAR(255) NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE,
    deleted_at TIMESTAMP WITH TIME ZONE,
    CONSTRAINT fk_workspaces_owner FOREIGN KEY (owner_id) REFERENCES users(id) ON DELETE RESTRICT
);

-- Indekslar: Workspace bo'yicha tezkor qidiruv
CREATE INDEX IF NOT EXISTS idx_workspaces_owner_id ON workspaces(owner_id);
CREATE INDEX IF NOT EXISTS idx_workspaces_deleted_at ON workspaces(deleted_at) WHERE deleted_at IS NULL;

-- 2. BOARD COLUMNS TABLE
CREATE TABLE IF NOT EXISTS board_columns (
    id VARCHAR(255) PRIMARY KEY,
    workspace_id VARCHAR(255) NOT NULL,
    title VARCHAR(255) NOT NULL,
    column_order INTEGER NOT NULL DEFAULT 0,
    is_default BOOLEAN NOT NULL DEFAULT FALSE,
    is_done BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE,
    CONSTRAINT fk_columns_workspace FOREIGN KEY (workspace_id) REFERENCES workspaces(id) ON DELETE CASCADE
);

-- Indekslar: Doska ustunlari va ularning tartibi bo'yicha indekslar
CREATE INDEX IF NOT EXISTS idx_board_columns_workspace_id ON board_columns(workspace_id);
CREATE INDEX IF NOT EXISTS idx_board_columns_order ON board_columns(workspace_id, column_order);

-- 3. TASKS TABLE (LexoRank tartiblash ustuni bilan)
CREATE TABLE IF NOT EXISTS tasks (
    id VARCHAR(255) PRIMARY KEY,
    public_id VARCHAR(255) NOT NULL UNIQUE,
    workspace_id VARCHAR(255) NOT NULL,
    column_id VARCHAR(255) NOT NULL,
    sprint_id VARCHAR(255),
    title VARCHAR(255) NOT NULL,
    description TEXT,
    task_order VARCHAR(255) NOT NULL, -- LexoRank string (masalan: "0|hzzzzz:")
    priority VARCHAR(32) NOT NULL DEFAULT 'MEDIUM' CHECK (priority IN ('LOW', 'MEDIUM', 'HIGH', 'URGENT')),
    issue_type VARCHAR(32) NOT NULL DEFAULT 'TASK' CHECK (issue_type IN ('TASK', 'BUG', 'STORY', 'EPIC')),
    due_date DATE,
    story_points INTEGER,
    is_archived BOOLEAN NOT NULL DEFAULT FALSE,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE,
    deleted_at TIMESTAMP WITH TIME ZONE,
    CONSTRAINT fk_tasks_workspace FOREIGN KEY (workspace_id) REFERENCES workspaces(id) ON DELETE CASCADE,
    CONSTRAINT fk_tasks_column FOREIGN KEY (column_id) REFERENCES board_columns(id) ON DELETE CASCADE
);

-- Indekslar: Tasks uchun qidiruv va LexoRank tartiblash indekslari
CREATE INDEX IF NOT EXISTS idx_tasks_column_lexorank ON tasks(column_id, task_order ASC) WHERE deleted_at IS NULL;
CREATE INDEX IF NOT EXISTS idx_tasks_workspace_id ON tasks(workspace_id) WHERE deleted_at IS NULL;
CREATE INDEX IF NOT EXISTS idx_tasks_sprint_id ON tasks(sprint_id) WHERE sprint_id IS NOT NULL AND deleted_at IS NULL;
CREATE INDEX IF NOT EXISTS idx_tasks_public_id ON tasks(public_id);
CREATE INDEX IF NOT EXISTS idx_tasks_deleted_at ON tasks(deleted_at);
