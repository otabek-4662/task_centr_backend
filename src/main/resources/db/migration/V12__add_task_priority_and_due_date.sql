-- V12: Add priority and due_date to tasks table with performance indexes

ALTER TABLE tasks ADD COLUMN IF NOT EXISTS priority VARCHAR(32) NOT NULL DEFAULT 'MEDIUM';
ALTER TABLE tasks ADD COLUMN IF NOT EXISTS due_date DATE;

-- Performance indexes for filtering and sorting
CREATE INDEX IF NOT EXISTS idx_tasks_workspace_priority ON tasks (workspace_id, priority);
CREATE INDEX IF NOT EXISTS idx_tasks_workspace_due_date ON tasks (workspace_id, due_date);
