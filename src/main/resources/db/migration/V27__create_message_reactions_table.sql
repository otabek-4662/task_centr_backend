CREATE TABLE message_reactions (
    id VARCHAR(36) PRIMARY KEY,
    message_id VARCHAR(36) NOT NULL,
    user_id VARCHAR(36) NOT NULL,
    emoji VARCHAR(50) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_message_reactions_msg FOREIGN KEY (message_id) REFERENCES chat_messages (id) ON DELETE CASCADE,
    CONSTRAINT fk_message_reactions_usr FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT uq_message_reaction UNIQUE (message_id, user_id)
);

CREATE INDEX idx_message_reactions_message_id ON message_reactions (message_id);
