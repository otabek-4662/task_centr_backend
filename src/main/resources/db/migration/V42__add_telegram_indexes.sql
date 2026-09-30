-- V42: Telegram maydonlari uchun index va unique cheklovini qo'shish

-- token bo'yicha tez qidirish uchun index
CREATE INDEX IF NOT EXISTS idx_users_telegram_link_token ON users(telegram_link_token);

-- bitta telegram akkaunt bir nechta tizim akkauntlariga ulanmasligi uchun
ALTER TABLE users ADD CONSTRAINT uk_users_telegram_chat_id UNIQUE (telegram_chat_id);
