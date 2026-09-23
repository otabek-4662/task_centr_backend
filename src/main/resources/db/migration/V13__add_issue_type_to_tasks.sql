-- V13: Add issue_type to tasks table with performance index

ALTER TABLE tasks ADD COLUMN IF NOT EXISTS issue_type VARCHAR(32) NOT NULL DEFAULT 'TASK';

-- Performance index for filtering by issue type in workspace
CREATE INDEX IF NOT EXISTS idx_tasks_workspace_issue_type ON tasks (workspace_id, issue_type);
