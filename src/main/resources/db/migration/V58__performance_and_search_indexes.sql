-- V58: High-traffic query optimization indexes for Render/PostgreSQL production
CREATE INDEX IF NOT EXISTS idx_tasks_workspace_active_order
    ON tasks (workspace_id, column_id, task_order)
    WHERE deleted_at IS NULL AND (is_archived = false OR is_archived IS NULL);

CREATE INDEX IF NOT EXISTS idx_tasks_workspace_sprint_active
    ON tasks (workspace_id, sprint_id, task_order)
    WHERE deleted_at IS NULL AND (is_archived = false OR is_archived IS NULL);

CREATE INDEX IF NOT EXISTS idx_tasks_due_date_active
    ON tasks (due_date, workspace_id)
    WHERE deleted_at IS NULL AND (is_archived = false OR is_archived IS NULL) AND due_date IS NOT NULL;

CREATE INDEX IF NOT EXISTS idx_task_activities_task_created_desc
    ON task_activities (task_id, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_notifications_user_unread_created
    ON notifications (user_id, is_read, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_chat_messages_workspace_created_desc
    ON chat_messages (workspace_id, created_at DESC)
    WHERE deleted_at IS NULL;
