-- V61: Workspace Production Hardening, Sequential Jira Task Keys, Subtasks, WIP Limits, Worklogs & Soft-Delete Cascade
-- 1. Sequential Jira Task Counter on Workspaces (LOY-1, LOY-2, ...)
ALTER TABLE workspaces ADD COLUMN IF NOT EXISTS task_seq BIGINT NOT NULL DEFAULT 0;

UPDATE workspaces w
SET task_seq = COALESCE((SELECT COUNT(*) FROM tasks t WHERE t.workspace_id = w.id), 0)
WHERE w.task_seq = 0;

CREATE OR REPLACE FUNCTION fn_assign_sequential_task_key()
RETURNS TRIGGER AS $$
DECLARE
    v_prefix VARCHAR(32);
    v_next_seq BIGINT;
BEGIN
    IF NEW.public_id IS NULL OR BTRIM(NEW.public_id) = '' THEN
        UPDATE workspaces
        SET task_seq = COALESCE(task_seq, 0) + 1
        WHERE id = NEW.workspace_id
        RETURNING
            COALESCE(NULLIF(REGEXP_REPLACE(SPLIT_PART(COALESCE(key_prefix, 'WFM'), '-', 1), '[^A-Za-z0-9]', '', 'g'), ''), 'WFM'),
            task_seq
        INTO v_prefix, v_next_seq;

        IF v_prefix IS NULL THEN
            v_prefix := 'WFM';
            v_next_seq := floor(random() * 900000 + 100000)::BIGINT;
        END IF;

        NEW.public_id := UPPER(v_prefix) || '-' || v_next_seq;
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_assign_sequential_task_key ON tasks;
CREATE TRIGGER trg_assign_sequential_task_key
    BEFORE INSERT ON tasks
    FOR EACH ROW
    EXECUTE FUNCTION fn_assign_sequential_task_key();

-- 2. Subtasks Hierarchy (parent_id on tasks)
ALTER TABLE tasks ADD COLUMN IF NOT EXISTS parent_id VARCHAR(36);
CREATE INDEX IF NOT EXISTS idx_tasks_parent_id ON tasks (parent_id) WHERE deleted_at IS NULL;

-- 3. Column WIP Limits (wip_limit on board_columns)
ALTER TABLE board_columns ADD COLUMN IF NOT EXISTS wip_limit INTEGER;

-- 4. Time Tracking / Worklogs Table (task_worklogs)
CREATE TABLE IF NOT EXISTS task_worklogs (
    id VARCHAR(36) PRIMARY KEY,
    task_id VARCHAR(36) NOT NULL REFERENCES tasks(id) ON DELETE CASCADE,
    workspace_id VARCHAR(36) NOT NULL,
    user_id VARCHAR(36) NOT NULL,
    hours DOUBLE PRECISION NOT NULL,
    description TEXT,
    work_date DATE NOT NULL DEFAULT CURRENT_DATE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_task_worklogs_task_id ON task_worklogs (task_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_task_worklogs_workspace_user ON task_worklogs (workspace_id, user_id);

-- 5. Soft-Delete Cascade Triggers (Workspace -> Columns & Tasks, Column -> Tasks)
CREATE OR REPLACE FUNCTION fn_workspace_soft_delete_cascade()
RETURNS TRIGGER AS $$
BEGIN
    IF OLD.deleted_at IS NULL AND NEW.deleted_at IS NOT NULL THEN
        UPDATE board_columns
        SET deleted_at = NEW.deleted_at
        WHERE workspace_id = NEW.id AND deleted_at IS NULL;

        UPDATE tasks
        SET deleted_at = NEW.deleted_at
        WHERE workspace_id = NEW.id AND deleted_at IS NULL;
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_workspace_soft_delete_cascade ON workspaces;
CREATE TRIGGER trg_workspace_soft_delete_cascade
    AFTER UPDATE OF deleted_at ON workspaces
    FOR EACH ROW
    EXECUTE FUNCTION fn_workspace_soft_delete_cascade();

CREATE OR REPLACE FUNCTION fn_column_soft_delete_cascade()
RETURNS TRIGGER AS $$
BEGIN
    IF OLD.deleted_at IS NULL AND NEW.deleted_at IS NOT NULL THEN
        UPDATE tasks
        SET deleted_at = NEW.deleted_at
        WHERE column_id = NEW.id AND deleted_at IS NULL;
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_column_soft_delete_cascade ON board_columns;
CREATE TRIGGER trg_column_soft_delete_cascade
    AFTER UPDATE OF deleted_at ON board_columns
    FOR EACH ROW
    EXECUTE FUNCTION fn_column_soft_delete_cascade();
