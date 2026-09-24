-- V20: Create chat_messages table for public and direct (DM) messages

CREATE TABLE IF NOT EXISTS chat_messages (
    id VARCHAR(255) PRIMARY KEY,
    sender_id VARCHAR(255) NOT NULL,
    recipient_id VARCHAR(255),
    content TEXT NOT NULL,
    type VARCHAR(50) NOT NULL DEFAULT 'PUBLIC',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP,
    CONSTRAINT fk_chat_messages_sender FOREIGN KEY (sender_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_chat_messages_recipient FOREIGN KEY (recipient_id) REFERENCES users(id) ON DELETE CASCADE
);

-- Performance indexes for fast pagination
CREATE INDEX IF NOT EXISTS idx_chat_messages_public ON chat_messages (type, created_at);
CREATE INDEX IF NOT EXISTS idx_chat_messages_sender_recipient ON chat_messages (sender_id, recipient_id, created_at);
CREATE INDEX IF NOT EXISTS idx_chat_messages_recipient_created ON chat_messages (recipient_id, created_at);
