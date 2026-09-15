-- V10: key_prefix uniqueness faqat ACTIVE (deleted_at IS NULL) qatorlar uchun.
-- Sabab: soft-delete qilingan workspace'lar eski prefiksni band qilib qo'yib,
-- bir xil nom bilan qayta yaratishga 409 berardi (existsByKeyPrefix @Where bilan
-- o'chirilganlarni ko'rmaydi, lekin full UNIQUE ularni ham tekshiradi).
-- Partial index JPA dagi @Where mantig'iga to'liq mos keladi.

ALTER TABLE workspaces DROP CONSTRAINT IF EXISTS uk_workspace_key_prefix;
DROP INDEX IF EXISTS uk_workspace_key_prefix;
CREATE UNIQUE INDEX IF NOT EXISTS uk_workspace_key_prefix
    ON workspaces (key_prefix) WHERE deleted_at IS NULL;
