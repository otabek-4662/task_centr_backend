ALTER TABLE users
ADD COLUMN telegram_notify_assigned BOOLEAN NOT NULL DEFAULT TRUE,
ADD COLUMN telegram_notify_comments BOOLEAN NOT NULL DEFAULT TRUE,
ADD COLUMN telegram_notify_deadlines BOOLEAN NOT NULL DEFAULT TRUE,
ADD COLUMN telegram_daily_digest BOOLEAN NOT NULL DEFAULT TRUE,
ADD COLUMN telegram_quiet_start TIME NULL,
ADD COLUMN telegram_quiet_end TIME NULL;
