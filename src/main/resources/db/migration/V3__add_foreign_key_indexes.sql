-- Add indexes on foreign key columns for better query performance
-- These indexes significantly improve JOIN operations and WHERE clauses on FK columns

-- Board columns foreign keys
CREATE INDEX IF NOT EXISTS idx_board_columns_workspace_fk ON board_columns(workspace_id);

-- Tasks foreign keys
CREATE INDEX IF NOT EXISTS idx_tasks_workspace_fk ON tasks(workspace_id);
CREATE INDEX IF NOT EXISTS idx_tasks_column_fk ON tasks(column_id);

-- Labels foreign keys
CREATE INDEX IF NOT EXISTS idx_labels_workspace_fk ON labels(workspace_id);

-- Workspace members foreign keys
CREATE INDEX IF NOT EXISTS idx_workspace_members_workspace_fk ON workspace_members(workspace_id);
CREATE INDEX IF NOT EXISTS idx_workspace_members_user_fk ON workspace_members(user_id);

-- Task labels junction table
CREATE INDEX IF NOT EXISTS idx_task_labels_task_fk ON task_labels(task_id);
CREATE INDEX IF NOT EXISTS idx_task_labels_label_fk ON task_labels(label_id);

-- Task assignees junction table
CREATE INDEX IF NOT EXISTS idx_task_assignees_task_fk ON task_assignees(task_id);
CREATE INDEX IF NOT EXISTS idx_task_assignees_user_fk ON task_assignees(user_id);

-- Workspaces owner foreign key
CREATE INDEX IF NOT EXISTS idx_workspaces_owner_fk ON workspaces(owner_id);
