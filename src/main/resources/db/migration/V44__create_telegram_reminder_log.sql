-- V44: telegram_reminder_log jadvali — muddat eslatmalari takrorlanmaslik uchun
CREATE TABLE telegram_reminder_log (
    id          BIGSERIAL PRIMARY KEY,
    task_id     VARCHAR(255) NOT NULL,
    user_id     VARCHAR(255) NOT NULL,
    type        VARCHAR(16)  NOT NULL,          -- H24, H1, OVERDUE
    sent_at     TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_reminder_task_user_type UNIQUE (task_id, user_id, type)
);

CREATE INDEX idx_reminder_log_task ON telegram_reminder_log (task_id);
CREATE INDEX idx_reminder_log_user ON telegram_reminder_log (user_id);
