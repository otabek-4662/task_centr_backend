-- V6: Add priority, assignee_id, due_date columns to tasks table

ALTER TABLE tasks
    ADD COLUMN IF NOT EXISTS priority VARCHAR(20) NOT NULL DEFAULT 'MEDIUM',
    ADD COLUMN IF NOT EXISTS assignee_id VARCHAR(255),
    ADD COLUMN IF NOT EXISTS due_date TIMESTAMP(6);
