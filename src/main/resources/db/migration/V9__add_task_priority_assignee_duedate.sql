-- V9: Add priority, assignee_id, due_date columns to tasks table
-- (Eslatma: bu DDL avval xatolik bilan V6 fayliga yozilgan edi;
-- prod DB da V6 allaqachon optimistic-locking varianti bilan applied bo'lgani uchun
-- bu yerga ko'chirildi. IF NOT EXISTS bo'lgani uchun xavfsiz.)

ALTER TABLE tasks
    ADD COLUMN IF NOT EXISTS priority VARCHAR(20) NOT NULL DEFAULT 'MEDIUM',
    ADD COLUMN IF NOT EXISTS assignee_id VARCHAR(255),
    ADD COLUMN IF NOT EXISTS due_date TIMESTAMP(6);
