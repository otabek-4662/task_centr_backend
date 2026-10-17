-- V64: Create user_contacts table for managing personal contacts

CREATE TABLE IF NOT EXISTS user_contacts (
    id VARCHAR(255) PRIMARY KEY,
    user_id VARCHAR(255) NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    contact_user_id VARCHAR(255) NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_user_contacts_pair UNIQUE (user_id, contact_user_id),
    CONSTRAINT chk_user_contacts_no_self CHECK (user_id <> contact_user_id)
);

CREATE INDEX IF NOT EXISTS idx_user_contacts_user_id ON user_contacts (user_id);
CREATE INDEX IF NOT EXISTS idx_user_contacts_contact_id ON user_contacts (contact_user_id);
