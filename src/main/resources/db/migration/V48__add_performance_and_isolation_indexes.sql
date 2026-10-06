-- V48: DB Performance & Multi-tenancy Isolation Indexes
-- Asosiy ustunlar (workspace_id, column_id, task_id, sprint_id) bo'yicha indekslar

-- Tasks: workspace va column bo'yicha saralash va filtrlash (Board va Kanban uchun)
CREATE INDEX IF NOT EXISTS idx_tasks_ws_col_rank ON tasks (workspace_id, column_id, lexo_rank);

-- Tasks: ko'p ijarachilik (multi-tenancy) izolyatsiyasi bo'yicha tekshirish
CREATE INDEX IF NOT EXISTS idx_tasks_id_workspace ON tasks (id, workspace_id);

-- Tasks: sprint bo'yicha saralash va backlog qidiruvi
CREATE INDEX IF NOT EXISTS idx_tasks_sprint_rank ON tasks (sprint_id, lexo_rank);
CREATE INDEX IF NOT EXISTS idx_tasks_ws_sprint_archived ON tasks (workspace_id, sprint_id, is_archived);

-- Tasks: arxiv va soft-delete holati bo'yicha filtrlash
CREATE INDEX IF NOT EXISTS idx_tasks_ws_archived_deleted ON tasks (workspace_id, is_archived, deleted_at);

-- Board Columns: tenant izolyatsiyasi bo'yicha tekshirish
CREATE INDEX IF NOT EXISTS idx_board_columns_id_workspace ON board_columns (id, workspace_id);

-- Sprints: tenant izolyatsiyasi va status bo'yicha qidiruv
CREATE INDEX IF NOT EXISTS idx_sprints_id_workspace ON sprints (id, workspace_id);
CREATE INDEX IF NOT EXISTS idx_sprints_ws_status ON sprints (workspace_id, status);

-- Labels: tenant izolyatsiyasi
CREATE INDEX IF NOT EXISTS idx_labels_id_workspace ON labels (id, workspace_id);

-- Task Checklists: task_id va tartib bo'yicha indeks
CREATE INDEX IF NOT EXISTS idx_checklist_task_order ON task_checklist_items (task_id, order_index);
