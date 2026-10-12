-- V56: telegram_reminder_log dagi synthetic key (DIGEST_<date>, REMINDER_<date>) muammosini tuzatish
-- ROLLBACK NOTE:
-- ALTER TABLE telegram_reminder_log DROP CONSTRAINT IF EXISTS chk_reminder_log_task_or_key;
-- DROP INDEX IF EXISTS uq_reminder_log_key;
-- DROP INDEX IF EXISTS idx_reminder_log_task_user_type_partial;
-- ALTER TABLE telegram_reminder_log DROP COLUMN IF EXISTS log_key;
-- ALTER TABLE telegram_reminder_log ALTER COLUMN task_id SET NOT NULL;
-- ALTER TABLE telegram_reminder_log ALTER COLUMN user_id SET NOT NULL;

-- 1. task_id va user_id ustunlaridan NOT NULL cheklovini olib tashlash (synthetic locklar uchun)
ALTER TABLE telegram_reminder_log ALTER COLUMN task_id DROP NOT NULL;
ALTER TABLE telegram_reminder_log ALTER COLUMN user_id DROP NOT NULL;

-- 2. log_key ustunini qo'shish (agar mavjud bo'lmasa)
ALTER TABLE telegram_reminder_log ADD COLUMN IF NOT EXISTS log_key VARCHAR(100);

-- 3. CHECK constraint: task_id va log_key dan aynan bittasi to'ldirilgan bo'lishi shart
-- NOT VALID qilib qo'shilib, keyin VALIDATE qilinadi (mavjud qatorlarni bloklamaslik uchun)
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'chk_reminder_log_task_or_key'
    ) THEN
        ALTER TABLE telegram_reminder_log
            ADD CONSTRAINT chk_reminder_log_task_or_key
            CHECK (
                (task_id IS NOT NULL AND log_key IS NULL) OR
                (task_id IS NULL AND log_key IS NOT NULL)
            ) NOT VALID;
        ALTER TABLE telegram_reminder_log
            VALIDATE CONSTRAINT chk_reminder_log_task_or_key;
    END IF;
END $$;

-- 4. log_key uchun partial unique index (NULL bo'lmagan yozuvlar uchun)
CREATE UNIQUE INDEX IF NOT EXISTS uq_reminder_log_key
    ON telegram_reminder_log (log_key)
    WHERE log_key IS NOT NULL;

-- 5. Haqiqiy tasklar uchun duplicate larni tekshirish va partial unique index qo'shish
DO $$
DECLARE
    dup_count INTEGER;
BEGIN
    SELECT COUNT(*) INTO dup_count
    FROM (
        SELECT task_id, user_id, type
        FROM telegram_reminder_log
        WHERE task_id IS NOT NULL
        GROUP BY task_id, user_id, type
        HAVING COUNT(*) > 1
    ) d;

    IF dup_count > 0 THEN
        RAISE WARNING 'telegram_reminder_log jadvalida % ta takroriy yozuv topildi! Partial unique index yaratilmadi.', dup_count;
    ELSE
        IF NOT EXISTS (
            SELECT 1 FROM pg_indexes
            WHERE tablename = 'telegram_reminder_log'
              AND indexname = 'idx_reminder_log_task_user_type_partial'
        ) THEN
            CREATE UNIQUE INDEX idx_reminder_log_task_user_type_partial
                ON telegram_reminder_log (task_id, user_id, type)
                WHERE task_id IS NOT NULL;
        END IF;
    END IF;
END $$;
