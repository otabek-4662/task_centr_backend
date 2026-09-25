CREATE TABLE user_presence (
    user_id VARCHAR(36) PRIMARY KEY,
    is_online BOOLEAN NOT NULL DEFAULT FALSE,
    last_seen_at TIMESTAMP,
    CONSTRAINT fk_user_presence_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
);

-- Copy existing users to user_presence
INSERT INTO user_presence (user_id, is_online, last_seen_at)
SELECT id, FALSE, last_seen_at FROM users;
