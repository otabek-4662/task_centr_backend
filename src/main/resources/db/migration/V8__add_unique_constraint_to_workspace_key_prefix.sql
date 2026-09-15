-- V8: Deduplicate workspaces.key_prefix (keep earliest, rename rest WR -> WR2 -> WR3 ...),
-- then add unique constraint. Failed V8 attempts are rolled back by Flyway, so editing is safe.

DO $$
DECLARE
    dup RECORD;
    n INT;
    candidate VARCHAR(10);
    stem VARCHAR(10);
BEGIN
    LOOP
        -- Bir xil prefiksli guruhda eng eskisidan keyingi bitta qatorni ol
        SELECT w.id, w.key_prefix INTO dup
        FROM workspaces w
        WHERE w.id <> (
            SELECT o.id FROM workspaces o
            WHERE o.key_prefix = w.key_prefix
            ORDER BY o.created_at NULLS LAST, o.id
            LIMIT 1
        )
        ORDER BY w.key_prefix, w.created_at NULLS LAST, w.id
        LIMIT 1;
        EXIT WHEN NOT FOUND;

        -- Bo'sh prefiks topilguncha raqam qo'shib tekshir (max 10 belgi)
        n := 2;
        LOOP
            stem := SUBSTRING(dup.key_prefix FROM 1 FOR (10 - LENGTH(n::TEXT)));
            candidate := stem || n::TEXT;
            EXIT WHEN NOT EXISTS (SELECT 1 FROM workspaces WHERE key_prefix = candidate);
            n := n + 1;
        END LOOP;

        UPDATE workspaces SET key_prefix = candidate WHERE id = dup.id;
    END LOOP;
END $$;

ALTER TABLE workspaces ADD CONSTRAINT uk_workspace_key_prefix UNIQUE (key_prefix);
