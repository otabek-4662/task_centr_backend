CREATE TABLE message_attachments (
    id VARCHAR(36) PRIMARY KEY,
    message_id VARCHAR(36) NOT NULL,
    file_url VARCHAR(255) NOT NULL,
    file_type VARCHAR(100) NOT NULL,
    file_size BIGINT,
    thumbnail_url VARCHAR(255),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP,
    CONSTRAINT fk_message_attachments_message FOREIGN KEY (message_id) REFERENCES chat_messages (id) ON DELETE CASCADE
);

CREATE INDEX idx_message_attachments_message_id ON message_attachments (message_id);
