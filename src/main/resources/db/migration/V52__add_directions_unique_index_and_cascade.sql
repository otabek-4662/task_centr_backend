-- V52: Add unique index on directions name per workspace and CASCADE delete to task_directions
ALTER TABLE IF EXISTS task_directions DROP CONSTRAINT IF EXISTS fk_task_directions_task;
ALTER TABLE IF EXISTS task_directions DROP CONSTRAINT IF EXISTS fk_task_directions_direction;
ALTER TABLE IF EXISTS directions DROP CONSTRAINT IF EXISTS fk_directions_workspace;

ALTER TABLE directions 
    ADD CONSTRAINT fk_directions_workspace 
    FOREIGN KEY (workspace_id) REFERENCES workspaces(id) ON DELETE CASCADE;

ALTER TABLE task_directions 
    ADD CONSTRAINT fk_task_directions_task 
    FOREIGN KEY (task_id) REFERENCES tasks(id) ON DELETE CASCADE;

ALTER TABLE task_directions 
    ADD CONSTRAINT fk_task_directions_direction 
    FOREIGN KEY (direction_id) REFERENCES directions(id) ON DELETE CASCADE;

CREATE UNIQUE INDEX IF NOT EXISTS idx_directions_workspace_name_unique 
    ON directions (workspace_id, LOWER(name)) WHERE deleted_at IS NULL;
