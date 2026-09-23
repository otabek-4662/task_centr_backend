-- Create sprints table
CREATE TABLE IF NOT EXISTS sprints (
    id VARCHAR(255) PRIMARY KEY,
    workspace_id VARCHAR(255) NOT NULL REFERENCES workspaces(id) ON DELETE CASCADE,
    name VARCHAR(255) NOT NULL,
    goal TEXT,
    status VARCHAR(32) NOT NULL DEFAULT 'FUTURE',
    start_date DATE,
    end_date DATE,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP(6)
);

CREATE INDEX IF NOT EXISTS idx_sprints_workspace_status ON sprints(workspace_id, status);

-- Add sprint_id to tasks table
ALTER TABLE tasks ADD COLUMN IF NOT EXISTS sprint_id VARCHAR(255) REFERENCES sprints(id) ON DELETE SET NULL;
CREATE INDEX IF NOT EXISTS idx_tasks_sprint_id ON tasks(sprint_id);
CREATE INDEX IF NOT EXISTS idx_tasks_workspace_sprint ON tasks(workspace_id, sprint_id);
