-- V22: Xabarlarni o'qilganligini kuzatish (Read Receipts / ✓✓)

CREATE TABLE IF NOT EXISTS message_read_status (
    id VARCHAR(255) PRIMARY KEY,
    message_id VARCHAR(255) NOT NULL,
    user_id VARCHAR(255) NOT NULL,
    read_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_read_status_message FOREIGN KEY (message_id) REFERENCES chat_messages(id) ON DELETE CASCADE,
    CONSTRAINT fk_read_status_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT uq_read_status_message_user UNIQUE (message_id, user_id)
);

CREATE INDEX IF NOT EXISTS idx_read_status_message_id ON message_read_status (message_id);
CREATE INDEX IF NOT EXISTS idx_read_status_user_id ON message_read_status (user_id, read_at);
