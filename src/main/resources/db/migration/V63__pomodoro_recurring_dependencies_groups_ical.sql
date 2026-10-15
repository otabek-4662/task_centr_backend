-- V63: Pomodoro timer, recurring tasks, dependencies, telegram supergroups, and iCal calendar token

-- 1. Recurring Tasks fields on tasks
ALTER TABLE tasks ADD COLUMN IF NOT EXISTS recurrence_rule VARCHAR(32) DEFAULT 'NONE';
ALTER TABLE tasks ADD COLUMN IF NOT EXISTS recurrence_interval INTEGER DEFAULT 1;
ALTER TABLE tasks ADD COLUMN IF NOT EXISTS next_occurrence_date DATE;
CREATE INDEX IF NOT EXISTS idx_tasks_next_occurrence ON tasks (next_occurrence_date) WHERE next_occurrence_date IS NOT NULL AND deleted_at IS NULL;

-- 2. Telegram Group Chat and Topic synchronization on workspaces
ALTER TABLE workspaces ADD COLUMN IF NOT EXISTS telegram_group_chat_id BIGINT;
ALTER TABLE workspaces ADD COLUMN IF NOT EXISTS telegram_topic_id INTEGER;
CREATE INDEX IF NOT EXISTS idx_workspaces_tg_group ON workspaces (telegram_group_chat_id) WHERE telegram_group_chat_id IS NOT NULL AND deleted_at IS NULL;

-- 3. iCal Calendar Token on users
ALTER TABLE users ADD COLUMN IF NOT EXISTS calendar_token VARCHAR(64);
CREATE UNIQUE INDEX IF NOT EXISTS idx_users_calendar_token ON users (calendar_token) WHERE calendar_token IS NOT NULL;

-- 4. Task Dependencies (Blockers)
CREATE TABLE IF NOT EXISTS task_dependencies (
    id VARCHAR(36) PRIMARY KEY,
    task_id VARCHAR(36) NOT NULL REFERENCES tasks(id) ON DELETE CASCADE,
    depends_on_task_id VARCHAR(36) NOT NULL REFERENCES tasks(id) ON DELETE CASCADE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_task_dependencies_pair UNIQUE (task_id, depends_on_task_id),
    CONSTRAINT chk_task_dependencies_no_self CHECK (task_id <> depends_on_task_id)
);
CREATE INDEX IF NOT EXISTS idx_task_dependencies_task_id ON task_dependencies (task_id);
CREATE INDEX IF NOT EXISTS idx_task_dependencies_depends_on ON task_dependencies (depends_on_task_id);

-- 5. Focus Timers (Pomodoro)
CREATE TABLE IF NOT EXISTS focus_timers (
    id VARCHAR(36) PRIMARY KEY,
    user_id VARCHAR(36) NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    task_id VARCHAR(36) REFERENCES tasks(id) ON DELETE SET NULL,
    target_minutes INTEGER NOT NULL DEFAULT 25,
    started_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    ended_at TIMESTAMP,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    logged_hours_added DOUBLE PRECISION DEFAULT 0.0
);
CREATE INDEX IF NOT EXISTS idx_focus_timers_user_active ON focus_timers (user_id) WHERE is_active = TRUE;
CREATE INDEX IF NOT EXISTS idx_focus_timers_task_id ON focus_timers (task_id);
