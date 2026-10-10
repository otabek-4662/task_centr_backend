-- 1. Status ustunida EXPIRED va CANCELLED statuslari to'siqsiz ishlashini ta'minlash:
-- Agar status bo'yicha cheklov (CHECK constraint) mavjud bo'lsa, uni xavfsiz bekor qilish
DO $$
DECLARE
    r RECORD;
BEGIN
    FOR r IN (
        SELECT c.conname
        FROM pg_constraint c
        JOIN pg_attribute a ON a.attrelid = c.conrelid AND a.attnum = ANY(c.conkey)
        WHERE c.conrelid = 'workspace_invitations'::regclass
          AND c.contype = 'c'
          AND a.attname = 'status'
    ) LOOP
        EXECUTE 'ALTER TABLE workspace_invitations DROP CONSTRAINT ' || quote_ident(r.conname);
    END LOOP;
END $$;

-- 2. workspace_members jadvalida (workspace_id, user_id) unique cheklovini tekshirish va ta'minlash:
-- V1 da PRIMARY KEY (workspace_id, user_id) mavjud, bu Postgres'da unique likni kafolatlaydi.
-- Qo'shimcha ravishda xavfsiz tekshirib, agar mavjud bo'lmasa, explicit UNIQUE constraint qo'shiladi:
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint c
        WHERE c.conrelid = 'workspace_members'::regclass
          AND c.contype IN ('u', 'p')
    ) THEN
        ALTER TABLE workspace_members ADD CONSTRAINT uk_workspace_members_workspace_user UNIQUE (workspace_id, user_id);
    END IF;
END $$;
