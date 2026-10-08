-- V51: Create directions table and task_directions many-to-many join table
CREATE TABLE IF NOT EXISTS directions (
    id VARCHAR(255) PRIMARY KEY,
    workspace_id VARCHAR(255) NOT NULL,
    name VARCHAR(255) NOT NULL,
    color VARCHAR(255) DEFAULT '#3B82F6',
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    deleted_at TIMESTAMP(6),
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP(6),
    version BIGINT NOT NULL DEFAULT 0
);

CREATE TABLE IF NOT EXISTS task_directions (
    task_id VARCHAR(255) NOT NULL,
    direction_id VARCHAR(255) NOT NULL,
    PRIMARY KEY (task_id, direction_id)
);

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_directions_workspace') THEN
        ALTER TABLE directions ADD CONSTRAINT fk_directions_workspace FOREIGN KEY (workspace_id) REFERENCES workspaces(id);
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_task_directions_task') THEN
        ALTER TABLE task_directions ADD CONSTRAINT fk_task_directions_task FOREIGN KEY (task_id) REFERENCES tasks(id);
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_task_directions_direction') THEN
        ALTER TABLE task_directions ADD CONSTRAINT fk_task_directions_direction FOREIGN KEY (direction_id) REFERENCES directions(id);
    END IF;
END $$;

CREATE INDEX IF NOT EXISTS idx_directions_workspace ON directions(workspace_id);
CREATE INDEX IF NOT EXISTS idx_directions_deleted_at ON directions(deleted_at);
CREATE INDEX IF NOT EXISTS idx_task_directions_task ON task_directions(task_id);
CREATE INDEX IF NOT EXISTS idx_task_directions_direction ON task_directions(direction_id);
