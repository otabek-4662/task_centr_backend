-- V45: board_columns ga is_done qo'shish va telegram_reminder_log ga FK cascade qo'shish

-- 1. board_columns ga is_done qo'shish va 'Ko''z tegmasin' ustunlarini is_done = true qilish
ALTER TABLE board_columns ADD COLUMN is_done BOOLEAN NOT NULL DEFAULT FALSE;

UPDATE board_columns SET is_done = TRUE WHERE title = 'Ko''z tegmasin';

-- 2. telegram_reminder_log dagi yetim yozuvlarni tozalash
DELETE FROM telegram_reminder_log
WHERE task_id NOT IN (SELECT id FROM tasks)
   OR user_id NOT IN (SELECT id FROM users);

-- 3. telegram_reminder_log ga FK (ON DELETE CASCADE) qo'shish
ALTER TABLE telegram_reminder_log
    ADD CONSTRAINT fk_reminder_log_task
    FOREIGN KEY (task_id) REFERENCES tasks(id) ON DELETE CASCADE;

ALTER TABLE telegram_reminder_log
    ADD CONSTRAINT fk_reminder_log_user
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE;
