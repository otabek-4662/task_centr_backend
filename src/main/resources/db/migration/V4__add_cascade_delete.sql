-- Add CASCADE delete behavior to existing foreign key constraints
-- This migration modifies FK constraints to enable automatic cascade deletion

-- Drop existing constraints
ALTER TABLE IF EXISTS task_assignees DROP CONSTRAINT IF EXISTS fk_task_assignees_task;
ALTER TABLE IF EXISTS task_assignees DROP CONSTRAINT IF EXISTS fk_task_assignees_user;
ALTER TABLE IF EXISTS task_labels DROP CONSTRAINT IF EXISTS fk_task_labels_task;
ALTER TABLE IF EXISTS task_labels DROP CONSTRAINT IF EXISTS fk_task_labels_label;
ALTER TABLE IF EXISTS tasks DROP CONSTRAINT IF EXISTS fk_tasks_column;
ALTER TABLE IF EXISTS tasks DROP CONSTRAINT IF EXISTS fk_tasks_workspace;
ALTER TABLE IF EXISTS labels DROP CONSTRAINT IF EXISTS fk_labels_workspace;
ALTER TABLE IF EXISTS board_columns DROP CONSTRAINT IF EXISTS fk_columns_workspace;
ALTER TABLE IF EXISTS workspace_members DROP CONSTRAINT IF EXISTS fk_ws_members_workspace;
ALTER TABLE IF EXISTS workspace_members DROP CONSTRAINT IF EXISTS fk_ws_members_user;

-- Recreate with CASCADE
ALTER TABLE board_columns 
    ADD CONSTRAINT fk_columns_workspace 
    FOREIGN KEY (workspace_id) REFERENCES workspaces(id) ON DELETE CASCADE;

ALTER TABLE labels 
    ADD CONSTRAINT fk_labels_workspace 
    FOREIGN KEY (workspace_id) REFERENCES workspaces(id) ON DELETE CASCADE;

ALTER TABLE tasks 
    ADD CONSTRAINT fk_tasks_workspace 
    FOREIGN KEY (workspace_id) REFERENCES workspaces(id) ON DELETE CASCADE;

ALTER TABLE tasks 
    ADD CONSTRAINT fk_tasks_column 
    FOREIGN KEY (column_id) REFERENCES board_columns(id) ON DELETE CASCADE;

ALTER TABLE task_labels 
    ADD CONSTRAINT fk_task_labels_task 
    FOREIGN KEY (task_id) REFERENCES tasks(id) ON DELETE CASCADE;

ALTER TABLE task_labels 
    ADD CONSTRAINT fk_task_labels_label 
    FOREIGN KEY (label_id) REFERENCES labels(id) ON DELETE CASCADE;

ALTER TABLE task_assignees 
    ADD CONSTRAINT fk_task_assignees_task 
    FOREIGN KEY (task_id) REFERENCES tasks(id) ON DELETE CASCADE;

ALTER TABLE task_assignees 
    ADD CONSTRAINT fk_task_assignees_user 
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE;

ALTER TABLE workspace_members 
    ADD CONSTRAINT fk_ws_members_workspace 
    FOREIGN KEY (workspace_id) REFERENCES workspaces(id) ON DELETE CASCADE;

ALTER TABLE workspace_members 
    ADD CONSTRAINT fk_ws_members_user 
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE;
