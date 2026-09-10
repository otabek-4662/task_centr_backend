-- Cleanup orphan data before applying constraints
-- This migration removes rows that violate foreign key constraints

-- Delete orphan board_columns (workspace_id not in workspaces)
DELETE FROM board_columns
WHERE workspace_id NOT IN (SELECT id FROM workspaces);

-- Delete orphan labels (workspace_id not in workspaces)
DELETE FROM labels
WHERE workspace_id NOT IN (SELECT id FROM workspaces);

-- Delete orphan tasks (workspace_id or column_id not valid)
DELETE FROM tasks
WHERE workspace_id NOT IN (SELECT id FROM workspaces)
   OR column_id NOT IN (SELECT id FROM board_columns);

-- Delete orphan task_labels (task_id or label_id not valid)
DELETE FROM task_labels
WHERE task_id NOT IN (SELECT id FROM tasks)
   OR label_id NOT IN (SELECT id FROM labels);

-- Delete orphan task_assignees (task_id or user_id not valid)
DELETE FROM task_assignees
WHERE task_id NOT IN (SELECT id FROM tasks)
   OR user_id NOT IN (SELECT id FROM users);

-- Delete orphan workspace_members (workspace_id or user_id not valid)
DELETE FROM workspace_members
WHERE workspace_id NOT IN (SELECT id FROM workspaces)
   OR user_id NOT IN (SELECT id FROM users);

-- Delete orphan workspaces (owner_id not in users)
DELETE FROM workspaces
WHERE owner_id NOT IN (SELECT id FROM users);
