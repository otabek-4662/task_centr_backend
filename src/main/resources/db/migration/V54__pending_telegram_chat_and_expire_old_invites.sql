-- 1. Token_hash bo'lmagan yoki muddati o'tgan eski PENDING takliflarni EXPIRED qilish
UPDATE workspace_invitations
SET status = 'EXPIRED'
WHERE status = 'PENDING' AND (token_hash IS NULL OR expires_at < CURRENT_TIMESTAMP);

-- 2. Pending Telegram chat takliflarini bazada (TTL 24 soat) saqlash jadvali
CREATE TABLE IF NOT EXISTS telegram_pending_chat_invites (
    chat_id BIGINT PRIMARY KEY,
    token VARCHAR(255) NOT NULL,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMP(6) NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_pending_chat_invites_expires_at ON telegram_pending_chat_invites(expires_at);
