-- V21: Chat xabarlarga reply (javob) va edit (tahrirlash) imkoniyati

-- reply_to_id: Qaysi xabarga javob berilganligi
ALTER TABLE chat_messages ADD COLUMN IF NOT EXISTS reply_to_id VARCHAR(255);
ALTER TABLE chat_messages ADD CONSTRAINT fk_chat_messages_reply_to
    FOREIGN KEY (reply_to_id) REFERENCES chat_messages(id) ON DELETE SET NULL;

-- edited_at: Xabar tahrirlanganligini bilish uchun
ALTER TABLE chat_messages ADD COLUMN IF NOT EXISTS edited_at TIMESTAMP;

-- reply_to_id indeksi
CREATE INDEX IF NOT EXISTS idx_chat_messages_reply_to ON chat_messages (reply_to_id);
